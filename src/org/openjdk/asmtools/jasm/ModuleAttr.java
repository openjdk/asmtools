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
package org.openjdk.asmtools.jasm;

import java.io.IOException;
import java.util.*;

import static org.openjdk.asmtools.jasm.RuntimeConstants.ACC_NONE;
import static org.openjdk.asmtools.jasm.RuntimeConstants.ACC_REEXPORT;

public class ModuleAttr extends AttrData {
  // shared data
  private final Module.Builder builder;
  private final ClassData clsData;
  private Module module;

  ModuleAttr(ClassData cdata) {
    super(cdata, Tables.AttrTag.ATT_Module.parsekey());
    builder = new Module.Builder();
    clsData = cdata;
  }

  public ModuleAttr build() {
    Content.instance.requiresStruct = new RequiresStruct();
    Content.instance.exportsStruct = new ExportsStruct();
    Content.instance.usesStruct = new UsesStruct();
    Content.instance.providesStruct = new ProvidesStruct();
    return this;
  }

  @Override
  public int attrLength() { return Content.instance.getLength(); }

  @Override
  public void write(CheckedDataOutputStream out) throws IOException {
    super.write(out);
    Content.instance.write(out);
  }

  /**
   * Adds a module on which the current module has a dependence.
   */
  protected void require(String d, boolean reexports) {
    builder.require(d, reexports);
  }

  /**
   * Adds a qualified/an unqualified  module exports.
   */
  protected void exports(String p, Set<String> ms) { builder.exports(p, ms); }

  /**
   * Adds a service dependence's of this module
   */
  protected void use(String cn) {
    builder.use(cn);
  }

  /**
   * Adds a service that a module provides one implementation of.
   */
  protected void provide(String s, String impl) {
    builder.provide(s, impl);
  }

  /**
   * Gets the module if it is absent builds one
   *
   * @return the Module
   */
  private Module buildModuleIfAbsent() {
    if (module == null) {
      module = builder.build();
    }
    return module;
  }

  private enum Content implements Data {

    instance {
      @Override
      public int getLength() {
        return requiresStruct.getLength() + exportsStruct.getLength() +
            usesStruct.getLength() + providesStruct.getLength();
      }

      @Override
      public void write(CheckedDataOutputStream out) throws IOException {
        // keep order!
        requiresStruct.write(out);
        exportsStruct.write(out);
        usesStruct.write(out);
        providesStruct.write(out);
      }
    };
    RequiresStruct requiresStruct;
    ExportsStruct exportsStruct;
    UsesStruct usesStruct;
    ProvidesStruct providesStruct;
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

    //* A module on which the current module has a dependence.
    private final static class Dependence implements Comparable<Dependence> {
      private final String mn;
      private final boolean reexports;

      public Dependence(String name, boolean reexports) {
        this.mn = name;
        this.reexports = reexports;
      }

      /**
       * Returns the module name.
       */
      public String name() { return mn; }

      /**
       * Returns the public modifier of the requires.
       */
      public boolean isReexports() { return reexports; }

      @Override
      public int hashCode() {
        return mn.hashCode() * 11 + Boolean.hashCode(reexports);
      }

      @Override
      public boolean equals(Object o) {
        if (o instanceof Dependence) {
          Dependence d = (Dependence) o;
          return this.mn.equals(d.mn) && Boolean.compare(reexports, d.isReexports()) == 0;
        }
        return false;
      }

      @Override
      public int compareTo(Dependence o) {
        int rc = this.mn.compareTo(o.mn);
        return rc != 0 ? rc : Boolean.compare(reexports, o.isReexports());
      }
    }

    /**
     * The module builder.
     */
    private static final class Builder {
      public final Set<Dependence> requires = new HashSet<>();
      final Map<String, Set<String>> exports = new HashMap<>();
      final Set<String> uses = new HashSet<>();
      final Map<String, Set<String>> provides = new HashMap<>();

      public Builder() {
      }

      public Builder require(String d, boolean reexports) {
        requires.add(new Dependence(d, reexports));
        return this;
      }

      public Builder exports(String p, Set<String> ms) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(ms);
        if (!exports.containsKey(p))
          exports.put(p, new HashSet<>());
        exports.get(p).addAll(ms);
        return this;
      }

      public Builder use(String cn) {
        uses.add(cn);
        return this;
      }

      public Builder provide(String s, String impl) {
        provides.computeIfAbsent(s, _k -> new HashSet<>()).add(impl);
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

  // Helper class
  private class Pair<F, S> {
    public final F first;
    public final S second;

    Pair(F first, S second) {
      this.first = first;
      this.second = second;
    }
  }

  /**
   * u2 requires_count;
   * { u2 requires_index;
   * u2 requires_flags;
   * } requires[requires_count];
   */
  class RequiresStruct implements Data {
    List<Pair<ConstantPool.ConstCell, Integer>> list = new ArrayList<>();

    public RequiresStruct() {
      buildModuleIfAbsent().requires.forEach(
          r -> list.add(
              new Pair<>(clsData.pool.FindCellAsciz(r.name()),
                  r.isReexports() ? ACC_REEXPORT : ACC_NONE))
      );
    }

    @Override
    public int getLength() { return 2 + list.size() * 4; }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      out.writeShort(list.size());    // u2 requires_count;
      for (Pair<ConstantPool.ConstCell, Integer> p : list) {
        out.writeShort(p.first.arg);      // u2 requires_index;
        out.writeShort(p.second);         // u2 requires_flags;
      }
    }
  }

  /**
   * u2 exports_count;
   * { u2 exports_index;
   * u2 exports_to_count;
   * u2 exports_to_index[exports_to_count];
   * } exports[exports_count];
   */
  private class ExportsStruct implements Data {
    final List<Pair<ConstantPool.ConstCell, List<ConstantPool.ConstCell>>> exports = new ArrayList<>();

    ExportsStruct() {
      Objects.requireNonNull(module);
      //(un)qualified module exports
      buildModuleIfAbsent().exports.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> {
                ArrayList<ConstantPool.ConstCell> to = new ArrayList<>();
                e.getValue().forEach(mn -> to.add(clsData.pool.FindCellAsciz(mn)));
                exports.add(new Pair<>(clsData.pool.FindCellAsciz(e.getKey()), to));
              }
          );
    }

    @Override
    public int getLength() {
      return 2 + 4 * exports.size() + exports.stream().mapToInt(p->p.second.size()).filter(s->s>0).sum() * 2;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      out.writeShort(exports.size());         // u2 exports_count;
      for (Pair<ConstantPool.ConstCell, List<ConstantPool.ConstCell>> pair : exports) {
        out.writeShort(pair.first.arg);       // u2 exports_index;
        out.writeShort(pair.second.size());   // u2 exports_to_count;
        for( ConstantPool.ConstCell to : pair.second ) {
            out.writeShort(to.arg);           // u2 exports_to_index[exports_to_count];
        }
      }
    }
  }

  /**
   * u2 uses_count;
   * u2 uses_index[uses_count];
   */
  private class UsesStruct implements Data {
    final List<ConstantPool.ConstCell> uses = new ArrayList<>();

    UsesStruct() {
      buildModuleIfAbsent().uses.stream().sorted().forEach(u -> uses.add(clsData.pool.FindCellAsciz(u)));
    }

    @Override
    public int getLength() {
      return 2 + 2 * uses.size();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      out.writeShort(uses.size());
      for (ConstantPool.ConstCell u : uses)
        out.writeShort(u.arg);
    }
  }

  /**
   * u2 provides_count;
   * { u2 provides_index;
   * u2 with_index;
   * } provides[provides_count];
   */
  private class ProvidesStruct implements Data {
    List<Pair<ConstantPool.ConstCell, ConstantPool.ConstCell>> list = new ArrayList<>();

    protected ProvidesStruct() {
      buildModuleIfAbsent().provides.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(e -> e.getValue().stream()
              .sorted()
              .forEach(impl -> list.add(new Pair<>(
                  clsData.pool.FindCellAsciz(e.getKey()), clsData.pool.FindCellAsciz(impl))
              )));
    }

    @Override
    public int getLength() {
      return 2 + list.size() * 4;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
      out.writeShort(list.size());        // u2 provides_count;
      for (Pair<ConstantPool.ConstCell, ConstantPool.ConstCell> p : list) {
        out.writeShort(p.first.arg);      // u2 requires_index;
        out.writeShort(p.second.arg);     // u2 requires_flags;
      }
    }
  }
}
