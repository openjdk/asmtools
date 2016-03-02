/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.jasm.Modifiers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 *  The module attribute data.
 */
public class ModuleData {

  // internal references
  private ConstantPool pool;
  private PrintWriter out;
  private Module module;

  public ModuleData(ClassData clsData) {
    this.pool = clsData.pool;
    this.out = clsData.out;
  }

  /**
   * Reads and resolve the method's attribute data called from ClassData.
   */
  public void read(DataInputStream in) throws IOException {
    Module.Builder builder = new Module.Builder();
    int requires_count = in.readUnsignedShort();
    for (int i = 0; i < requires_count; i++) {
      int index = in.readUnsignedShort();
      int flags = in.readUnsignedShort();
      String moduleName = pool.getString(index);

      Set<Modifier> mods;
      if (flags == 0) {
        mods = Collections.emptySet();
      } else {
        mods = new HashSet<>();
        if (Modifiers.isReexport(flags))
          mods.add(Modifier.PUBLIC);
        if (Modifiers.isSynthetic(flags))
          mods.add(Modifier.SYNTHETIC);
        if (Modifiers.isMandated(flags))
          mods.add(Modifier.MANDATED);
      }
      builder.require(moduleName, mods);
    }

    int exports_count = in.readUnsignedShort();
    if (exports_count > 0) {
      for (int i = 0; i < exports_count; i++) {
        int index = in.readUnsignedShort();
        String packageName = pool.getString(index).replace('/', '.');
        int exports_to_count = in.readUnsignedShort();
        if (exports_to_count > 0) {
          Set<String> targets = new HashSet<>(exports_to_count);
          for (int j = 0; j < exports_to_count; j++) {
            int exports_to_index = in.readUnsignedShort();
            targets.add(pool.getString(exports_to_index));
          }
          builder.exports(packageName, targets);
        } else {
          builder.export(packageName);
        }
      }
    }

    int uses_count = in.readUnsignedShort();
    if (uses_count > 0) {
      for (int i = 0; i < uses_count; i++) {
        int index = in.readUnsignedShort();
        String serviceName = pool.getClassName(index).replace('/', '.');
        builder.use(serviceName);
      }
    }

    int provides_count = in.readUnsignedShort();
    if (provides_count > 0) {
      Map<String, Set<String>> pm = new HashMap<>();
      for (int i = 0; i < provides_count; i++) {
        int index = in.readUnsignedShort();
        int with_index = in.readUnsignedShort();
        String sn = pool.getClassName(index).replace('/', '.');
        String cn = pool.getClassName(with_index).replace('/', '.');
        // computeIfAbsent
        Set<String> providers = pm.get(sn);
        if (providers == null) {
          providers = new HashSet<>();
          pm.put(sn, providers);
        }
        providers.add(cn);
      }
      for (Map.Entry<String, Set<String>> e : pm.entrySet()) {
        builder.provide(e.getKey(), e.getValue());
      }
    }
    module = builder.build();
  }

  /* Print Methods */
  public void print() throws IOException {
    if (module != null)
      out.println(module.toString());
  }


  private enum Modifier {
    PUBLIC(0x0020), SYNTHETIC(0x1000), MANDATED(0x8000);
    private final int value;
    Modifier(int value) {
      this.value = value;
    }
  }

  private final static class Module {
    //* A service dependence's of this module
    final Set<String> uses;
    //* A module on which the current module has a dependence.
    private final Set<Dependence> requires;
    //* A module export, may be qualified or unqualified.
    private final Map<String, Set<String>> exports;
    //* A service that a module provides one or more implementations of.
    private final Map<String, Set<String>> provides;

    private Module(Set<Dependence> requires,
                   Map<String, Set<String>> exports,
                   Set<String> uses,
                   Map<String, Set<String>> provides) {
      this.requires = Collections.unmodifiableSet(requires);
      this.exports = Collections.unmodifiableMap(exports);
      this.uses = Collections.unmodifiableSet(uses);
      this.provides = Collections.unmodifiableMap(provides);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      requires.stream()
          .sorted()
          .forEach(d -> sb.append(format("  %s%n", d.toString())));
      //
      exports.entrySet().stream()
          .filter(e -> e.getValue().isEmpty())
          .sorted(Map.Entry.comparingByKey())
          .map(e -> format("  exports %s;%n", e.getKey()))
          .forEach(sb::append);
      exports.entrySet().stream()
          .filter(e -> !e.getValue().isEmpty())
          .sorted(Map.Entry.comparingByKey())
          .map(e -> format("  exports %s to%n%s;%n", e.getKey(),
              e.getValue().stream().sorted()
                  .map(mn -> format("      %s", mn))
                  .collect(Collectors.joining(",\n"))))
          .forEach(sb::append);
      //
      uses.stream().sorted()
          .map(s -> format("  uses %s;%n", s))
          .forEach(sb::append);
      //
      provides.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .flatMap(e -> e.getValue().stream().sorted()
              .map(impl -> format("  provides %s with %s;%n", e.getKey(), impl)))
          .forEach(sb::append);
      return sb.toString();
    }

    //* A module on which the current module has a dependence.
    private final static class Dependence implements Comparable<Dependence> {
      private final String name;
      private final Set<Modifier> flags;

      public Dependence(String name, Set<Modifier> flags) {
        this.name = name;
        this.flags = flags;
      }

      /**
       * Returns the module name.
       *
       * @return The module name
       */
      public String name() {
        return name;
      }

      /**
       * Returns the set of modifiers.
       *
       * @return A possibly-empty unmodifiable set of modifiers
       */
      public Set<Modifier> modifiers() {
        return flags;
      }

      @Override
      public int hashCode() {
        return name.hashCode() * 11 + flags.hashCode();
      }

      @Override
      public boolean equals(Object o) {
        if (o instanceof Dependence) {
          Dependence d = (Dependence) o;
          return this.name.equals(d.name) && flags.equals(d.flags);
        }
        return false;
      }

      @Override
      public int compareTo(Dependence o) {
        int rc = this.name.compareTo(o.name);
        return rc != 0 ? rc : Integer.compare(this.flagsValue(), o.flagsValue());
      }

      @Override
      public String toString() {
        return format("requires %s%s;", flags.contains(Modifier.PUBLIC) ? "public " : "", name);
      }

      private int flagsValue() {
        int value = 0;
        for (Modifier m : flags) {
          value |= m.value;
        }
        return value;
      }
    }

    /**
     * The module builder.
     */
    public static final class Builder {

      public final Set<Dependence> requires = new HashSet<>();
      final Map<String, Set<String>> exports = new HashMap<>();
      final Set<String> uses = new HashSet<>();
      final Map<String, Set<String>> provides = new HashMap<>();

      public Builder() {
      }

      /**
       * Adds a module on which the current module has a dependence.
       */
      public Builder require(String d, Set<Modifier> flags) {
        requires.add(new Dependence(d, flags));
        return this;
      }

      /**
       * Adds a unqualified module export.
       */
      public Builder export(String p) {
        Objects.requireNonNull(p);
        if (!exports.containsKey(p)) exports.put(p, new HashSet<>());
        return this;
      }

      /**
       * Adds a qualified module exports.
       */
      public Builder exports(String p, Set<String> ms) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(ms);
        if (!exports.containsKey(p)) export(p);
        exports.get(p).addAll(ms);
        return this;
      }


      /**
       * Adds a service dependence's of this module
       */
      public Builder use(String cn) {
        uses.add(cn);
        return this;
      }

      /**
       * Adds a service that a module provides one or more implementations of.
       */
      public Builder provide(String s, Set<String> impl) {
        provides.computeIfAbsent(s, _k -> new HashSet<>()).addAll(impl);
        return this;
      }

      /**
       * @return The new module
       */
      public Module build() {
        return new Module(requires, exports, uses, provides);
      }
    }
  }
}
