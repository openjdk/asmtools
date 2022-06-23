/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.jasm.NameInfo;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.structure.ClassFileContext.*;
import static org.openjdk.asmtools.jdis.ConstantPool.TAG.*;

/**
 * Internal presentation of a module
 */
public final class ModuleContent extends Indenter {

    private static int MODULE_DIRECTIVE_PADDING = 9;
    //* A module name and module_flags
    public final Header header;
    //* A service dependence's of this module
    public final Set<Uses> uses;
    //* Modules on which the current module has a dependence.
    public final Set<Dependence> requires;
    //* A module exports, may be qualified or unqualified.
    public final Map<Exported, Set<ModuleContent.TargetType>> exports;
    //* Packages, to be opened by the current module
    public final Map<Opened, Set<ModuleContent.TargetType>> opens;
    //* A service that a  dule provides one or more implementations of.
    public final Map<Provided, Set<ModuleContent.TargetType>> provides;

    private ModuleContent(Builder builder) {
        this.header = builder.header;
        this.requires = Collections.unmodifiableSet(builder.requires);
        this.exports = Collections.unmodifiableMap(builder.exports);
        this.opens = Collections.unmodifiableMap(builder.opens);
        this.uses = Collections.unmodifiableSet(builder.uses);
        this.provides = Collections.unmodifiableMap(builder.provides);
    }

    public String getModuleFlags() {
        return EModifier.asKeywords(header.getFlags(), MODULE);
    }

    public String getModuleName() {
        return header.getModuleName();
    }

    // Gets the Constant Pool index to this module
    public int getModuleCPX() {
        return header.getModuleCPX();
    }

    public String getModuleVersion() {
        return header.getModuleVersion();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        requires.stream().forEach(
                d -> sb.append(IndentPadRight("requires", MODULE_DIRECTIVE_PADDING)).
                        append(d).
                        append(format(";%s%n", d.getModuleVersion() == null ? "" : " // @" + d.getModuleVersion())));
        //
        exports.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(e -> IndentPadRight("exports", MODULE_DIRECTIVE_PADDING).concat(format("%s;%n", e.getKey().toString())))
                .forEach(sb::append);
        exports.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> IndentPadRight("exports", MODULE_DIRECTIVE_PADDING).
                        concat(format("%s to%n%s;%n", e.getKey().toString(),
                                e.getValue().stream().
                                        map(mn -> this.enlargedIndent(mn.toString(), MODULE_DIRECTIVE_PADDING)).
                                        collect(Collectors.joining(",\n"))))
                ).forEach(sb::append);
        //
        opens.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(e -> IndentPadRight("opens", MODULE_DIRECTIVE_PADDING).concat(format("%s;%n", e.getKey().toString())))
                .forEach(sb::append);
        opens.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> IndentPadRight("opens", MODULE_DIRECTIVE_PADDING).
                        concat(format("%s to%n%s;%n", e.getKey().toString(),
                                e.getValue().stream()
                                        .map(mn -> this.enlargedIndent(mn.toString(), MODULE_DIRECTIVE_PADDING))
                                        .collect(Collectors.joining(",\n")))))
                .forEach(sb::append);
        //
        uses.stream().map(s -> IndentPadRight("uses", MODULE_DIRECTIVE_PADDING).concat(format("%s;%n", s)))
                .forEach(sb::append);
        //
        provides.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> IndentPadRight("provides", MODULE_DIRECTIVE_PADDING).
                        concat(format("%s with%n%s;%n", e.getKey().toString(),
                                e.getValue().stream()
                                        .map(mn -> this.enlargedIndent(mn.toString(), MODULE_DIRECTIVE_PADDING))
                                        .collect(Collectors.joining(",\n")))))
                .forEach(sb::append);
        //
        if (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    // A module header consists of a module name and module flags
    public final static class Header extends VersionedFlaggedTargetType {

        Header(int cpIndex, String typeName, int flag, String moduleVersion) {
            super(CONSTANT_MODULE, cpIndex, typeName, flag, MODULE, moduleVersion);
        }

        public String getModuleName() {
            return getTypeName();
        }

        public int getModuleFlags() {
            return getFlags();
        }

        public String getModuleVersion() {
            return getVersion();
        }

        // Gets the Constant Pool index to this module
        public int getModuleCPX() {
            return getCPIndex();
        }
    }

    //* A module on which the current module has a dependence.
    public final static class Dependence extends VersionedFlaggedTargetType {
        public Dependence(int cpIndex, String moduleName, int flag, String moduleVersion) {
            super(CONSTANT_MODULE, cpIndex, moduleName, flag, REQUIRES, moduleVersion);
        }

        public String getModuleVersion() {
            return getVersion();
        }
    }

    public final static class Uses extends TargetType {
        public Uses(int cpIndex, String typeName) {
            super(CONSTANT_CLASS, cpIndex, typeName);
        }

        public Uses(TargetType targetType) {
            this(targetType.getCPIndex(), targetType.getTypeName());
        }
    }

    //* A provided type of the current module.
    public final static class Provided extends TargetType {
        public Provided(int cpIndex, String typeName) {
            super(CONSTANT_CLASS, cpIndex, typeName);
        }

        public Provided(TargetType targetType) {
            this(targetType.getCPIndex(), targetType.getTypeName());
        }

    }

    //* An opened package of the current module.
    public final static class Opened extends FlaggedTargetType {
        public Opened(int cpIndex, String typeName, int opensFlags) {
            super(CONSTANT_PACKAGE, cpIndex, typeName, opensFlags, OPENS);
        }

        public Opened(FlaggedTargetType targetType) {
            this(targetType.getCPIndex(), targetType.getTypeName(), targetType.getFlags());
        }
    }

    //* An exported package of the current module.
    public final static class Exported extends FlaggedTargetType {
        public Exported(int cpIndex, String typeName, int exportsFlags) {
            super(CONSTANT_PACKAGE, cpIndex, typeName, exportsFlags, EXPORTS);
        }

        public Exported(FlaggedTargetType targetType) {
            this(targetType.getCPIndex(), targetType.getTypeName(), targetType.getFlags());
        }
    }

    public static class VersionedFlaggedTargetType extends FlaggedTargetType {
        private String version;

        VersionedFlaggedTargetType(ConstantPool.TAG tag, int cpIndex, String typeName, int flag, ClassFileContext context, String version) {
            super(tag, cpIndex, typeName, flag, context);
            this.version = version != null && !version.isEmpty() ? version : null;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public int hashCode() {
            int code = version == null ? 0 : version.hashCode();
            return code + super.hashCode();
        }
    }

    public static class FlaggedTargetType extends TargetType {
        private int flag;
        private ClassFileContext context;

        public FlaggedTargetType(ConstantPool.TAG tag, int cpIndex, String typeName, int flag, ClassFileContext context) {
            super(tag, cpIndex, typeName);
            this.flag = flag;
            this.context = context;
        }

        public boolean isFlagged() {
            return true;
        }

        public int getFlags() {
            return flag;
        }

        public void setFlag(int value) {
            flag = value;
        }

        @Override
        public int hashCode() {
            return super.hashCode() + flag;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && ((FlaggedTargetType) obj).flag == this.flag;
        }

        @Override
        public String toString() {
            return EModifier.asKeywords(this.flag, context) + super.toString();
        }
    }

    public static class TargetType extends NameInfo implements Comparable<TargetType> {

        // Type of constant in the constant pool
        private final ConstantPool.TAG tag;

        public TargetType(ConstantPool.TAG tag, int cpIndex, String typeName) {
            super(cpIndex, typeName);
            this.tag = tag;
        }

        public String getTypeName() {
            return super.name();
        }

        public int getCPIndex() {
            return super.cpIndex();
        }

        public boolean isFlagged() {
            return false;
        }

        @Override
        public int hashCode() {
            return super.cpIndex() + super.name().hashCode() * 11;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TargetType) {
                TargetType t = (TargetType) obj;
                return this.name().equals(t.getTypeName());
            }
            return false;
        }

        @Override
        public int compareTo(TargetType t) {
            return this.name().compareTo(t.getTypeName());
        }

        @Override
        public String toString() {
            return (Options.contains(Options.PR.CPX)) ? String.format("#%-4d /* %s */", getCPIndex(), name()) : name();
        }
    }

    /**
     * The module builder.
     */
    public static final class Builder {
        final Set<Dependence> requires = new HashSet<>();
        final Map<Exported, Set<ModuleContent.TargetType>> exports = new HashMap<>();
        final Map<Opened, Set<ModuleContent.TargetType>> opens = new HashMap<>();
        final Set<Uses> uses = new HashSet<>();
        final Map<Provided, Set<ModuleContent.TargetType>> provides = new HashMap<>();
        private Header header;
        private int moduleFlags = EModifier.ACC_NONE.getFlag();
        private int cpIndex;
        private String moduleName;
        private String moduleVersion;

        public Builder() {
        }

        public Builder(int cpIndex, String moduleName, int moduleFlags, String moduleVersion) {
            this.cpIndex = cpIndex;
            this.moduleFlags = moduleFlags;
            this.moduleName = moduleName;
            this.moduleVersion = moduleVersion;
        }

        public void setModuleFlags(int moduleFlags) {
            this.moduleFlags = moduleFlags;
        }

        public void setCpIndex(int cpIndex) {
            this.cpIndex = cpIndex;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public void setModuleVersion(String moduleVersion) {
            this.moduleVersion = moduleVersion;
        }

        public Builder require(int cpIndex, String d, int requiresFlag, String version) {
            return require(new Dependence(cpIndex, d, requiresFlag, version));
        }

        public Builder require(Dependence dependence) {
            requires.add(dependence);
            return this;
        }

        public Builder exports(Exported p, Set<ModuleContent.TargetType> ms) {
            return add(exports, p, ms);
        }

        public Builder exports(int cpIndex, String packageName, int exportFlags) {
            return add(exports, new Exported(cpIndex, packageName, exportFlags), new HashSet<>());
        }

        public Builder exports(int cpIndex, String packageName, int exportFlags, Set<ModuleContent.TargetType> ms) {
            return add(exports, new Exported(cpIndex, packageName, exportFlags), ms);
        }

        public Builder opens(Opened p, Set<ModuleContent.TargetType> ms) {
            return add(opens, p, ms);
        }

        public Builder opens(int cpIndex, String packageName, int exportFlags) {
            return add(opens, new Opened(cpIndex, packageName, exportFlags), new HashSet<>());
        }

        public Builder opens(int cpIndex, String packageName, int exportFlags, Set<ModuleContent.TargetType> ms) {
            return add(opens, new Opened(cpIndex, packageName, exportFlags), ms);
        }

        public Builder provides(Provided t, Set<ModuleContent.TargetType> implementations) {
            return add(provides, t, implementations);
        }

        public Builder provides(int cpIndex, String serviceName, Set<ModuleContent.TargetType> implementations) {
            return this.provides(new Provided(cpIndex, serviceName), implementations);
        }

        public Builder uses(int cpIndex, String serviceName) {
            return this.uses(new Uses(cpIndex, serviceName));
        }

        public Builder uses(Uses service) {
            uses.add(service);
            return this;
        }

        /**
         * @return The new module
         */
        public ModuleContent build() {
            header = new Header(cpIndex, moduleName, moduleFlags, moduleVersion);
            return new ModuleContent(this);
        }

        private <T extends TargetType> Builder add(Map<T, Set<ModuleContent.TargetType>> collection, T source, Set<ModuleContent.TargetType> target) {
            Objects.requireNonNull(source);
            Objects.requireNonNull(target);
            if (!collection.containsKey(source))
                collection.put(source, new HashSet<>());
            collection.get(source).addAll(target);
            return this;
        }
    }
}
