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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.asmutils.Triplet;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jdis.ModuleContent;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.openjdk.asmtools.common.structure.EModifier.ACC_OPEN;

/**
 * The module attribute
 */
class ModuleAttr extends AttrData {
    // shared data
    private ModuleContent.Builder builder;
    private final Function<String, ConstCell> findUTF8Cell;
    private final Function<ModuleContent.TargetType, ConstCell> findClassCell;
    private final Function<ModuleContent.TargetType, ConstCell> findModuleCell;
    private final Function<ModuleContent.TargetType, ConstCell> findPackageCell;

    // entries to populate tables of the module attribute
    Consumer<ModuleContent.Dependence> requires = (d) ->
            this.builder.require(d);
    BiConsumer<? extends ModuleContent.TargetType, Set<ModuleContent.TargetType>> exports = (e, ms) ->
            this.builder.exports(new ModuleContent.Exported((ModuleContent.FlaggedTargetType) e), ms);
    BiConsumer<? extends ModuleContent.TargetType, Set<ModuleContent.TargetType>> opens = (o, ms) ->
            this.builder.opens(new ModuleContent.Opened((ModuleContent.FlaggedTargetType) o), ms);
    BiConsumer<? extends ModuleContent.TargetType, Set<ModuleContent.TargetType>>
            provides = (p, cs) -> this.builder.provides(new ModuleContent.Provided(p), cs);
    Consumer<ModuleContent.TargetType> uses = (u) ->
            this.builder.uses(new ModuleContent.Uses(u));

    ModuleAttr(ClassData classData) {
        super(classData.pool, EAttribute.ATT_Module);
        builder = new ModuleContent.Builder();
        findUTF8Cell = targetType -> classData.pool.findUTF8Cell(targetType);
        findClassCell = targetType -> classData.pool.findClassCell(targetType);
        findModuleCell = targetType -> classData.pool.findModuleCell(targetType);
        findPackageCell = targetType -> classData.pool.findPackageCell(targetType);
    }

    void openModule() {
        builder.setModuleFlags(ACC_OPEN.getFlag());
    }

    void setModuleName(String value) {
        builder.setModuleName(value);
    }

    void setModuleNameCpIndex(int cpIndex) {
        builder.setCpIndex(cpIndex);
    }

    ModuleAttr build() {
        ModuleContent moduleContent = builder.build();
        Content.instance.header = new HeaderStruct(moduleContent.header, findModuleCell, findUTF8Cell);
        Content.instance.requiresStruct = new SetStruct<>(moduleContent.requires, findModuleCell, findUTF8Cell);
        Content.instance.exportsMapStruct = new MapStruct<>(moduleContent.exports, findPackageCell, findModuleCell);
        Content.instance.opensMapStruct = new MapStruct<>(moduleContent.opens, findPackageCell, findModuleCell);
        Content.instance.usesStruct = new SetStruct<>(moduleContent.uses, findClassCell, null);
        Content.instance.providesMapStruct = new MapStruct<>(moduleContent.provides, findClassCell, findClassCell);
        return this;
    }

    @Override
    public int attrLength() {
        return Content.instance.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        super.write(out);
        Content.instance.write(out);
    }

    private enum Content implements DataWriter {
        instance {
            @Override
            public int getLength() {
                return header.getLength() +
                        requiresStruct.getLength() +
                        exportsMapStruct.getLength() +
                        opensMapStruct.getLength() +
                        usesStruct.getLength() +
                        providesMapStruct.getLength();
            }

            @Override
            public void write(CheckedDataOutputStream out) throws IOException {
                // keep order!
                header.write(out);
                requiresStruct.write(out);
                exportsMapStruct.write(out);
                opensMapStruct.write(out);
                usesStruct.write(out);
                providesMapStruct.write(out);
            }
        };

        HeaderStruct header;
        SetStruct<ModuleContent.Dependence> requiresStruct;
        MapStruct<ModuleContent.Exported> exportsMapStruct;
        MapStruct<ModuleContent.Opened> opensMapStruct;
        SetStruct<ModuleContent.Uses> usesStruct;
        MapStruct<ModuleContent.Provided> providesMapStruct;
    }

    /**
     * u2 {exports|opens}_count;
     * {  u2 {exports|opens}_index;
     * u2 {exports|opens}_flags;
     * u2 {exports|opens}_to_count;
     * u2 {exports|opens}_to_index[{exports|opens}_to_count];
     * } {exports|opens}[{exports|opens}_count];
     * or
     * u2 provides_count;
     * {  u2 provides_index;
     * u2 provides_with_count;
     * u2 provides_with_index[provides_with_count];
     * } provides[provides_count];
     */
    private static class MapStruct<T extends ModuleContent.TargetType> implements DataWriter {
        final List<Triplet<ConstCell, Integer, List<ConstCell>>> exportsOpensList = new ArrayList<>();
        final List<Pair<ConstCell, List<ConstCell>>> providesList = new ArrayList<>();

        MapStruct(Map<T, Set<ModuleContent.TargetType>> source,
                  Function<ModuleContent.TargetType, ConstCell> nameFinder,
                  Function<ModuleContent.TargetType, ConstCell> targetFinder) {
            Objects.requireNonNull(source);
            source.entrySet().stream().forEach(
                    e -> {
                        ArrayList<ConstCell> to = new ArrayList<>();
                        e.getValue().forEach(mn -> to.add(targetFinder.apply(mn)));
                        if (e.getKey().isFlagged()) {
                            exportsOpensList.add(new Triplet<>
                                    (nameFinder.apply(e.getKey()),
                                            ((ModuleContent.FlaggedTargetType) e.getKey()).getFlags(),
                                            to));
                        } else {
                            providesList.add(new Pair<>(nameFinder.apply(e.getKey()),
                                    to));
                        }
                    }
            );
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            if (providesList.isEmpty()) {
                out.writeShort(exportsOpensList.size());          // u2 {exports|opens}_count;
                for (Triplet<ConstCell, Integer, List<ConstCell>> triplet : exportsOpensList) {
                    out.writeShort(triplet.first.cpIndex);              // {  u2 {exports|opens}_index;
                    out.writeShort(triplet.second);                 //    u2 {exports|opens}_flags;
                    out.writeShort(triplet.third.size());           //    u2 {exports|opens}_to_count;
                    for (ConstCell to : triplet.third)
                        out.writeShort(to.cpIndex);                       // u2 {exports|opens}_to_index[{exports|opens}_to_count]; }
                }
            } else {
                out.writeShort(providesList.size());              // u2 provides_count;
                for (Pair<ConstCell, List<ConstCell>> pair : providesList) {
                    out.writeShort(pair.first.cpIndex);                 // {  u2 provides_index;
                    out.writeShort(pair.second.size());             //    u2 provides_with_count;
                    for (ConstCell to : pair.second)
                        out.writeShort(to.cpIndex);                       // u2 provides_with_index[provides_with_count]; }
                }
            }
        }

        @Override
        public int getLength() {
            if (providesList.isEmpty()) {
                // (u2:{exports|opens}_count) + (u2:{exports|opens}_index + u2:{exports|opens}_flags u2:{exports|opens}_to_count) * {exports|opens}_count +
                return 2 + 6 * exportsOpensList.size() +
                        //  (u2:{exports|opens}_to_index) * {exports|opens}_to_count
                        exportsOpensList.stream().mapToInt(p -> p.third.size()).filter(s -> s > 0).sum() * 2;
            } else {
                // (u2 : provides_count) + (u2:provides_index + u2:provides_with_count) * provides_count +
                return 2 + 4 * providesList.size() +
                        // (u2:provides_with_index) * provides_with_count
                        providesList.stream().mapToInt(p -> p.second.size()).filter(s -> s > 0).sum() * 2;
            }
        }
    }

    private static class HeaderStruct implements DataWriter {
        final ConstCell index;
        final int flags;
        final ConstCell versionIndex;

        HeaderStruct(ModuleContent.Header source,
                     Function<ModuleContent.TargetType, ConstCell> nameFinder,
                     Function<String, ConstCell> versionFinder) {
            index = nameFinder.apply(source);
            versionIndex = (source.getModuleVersion() == null) ? null : versionFinder.apply(source.getModuleVersion());
            flags = source.getModuleFlags();
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(index.cpIndex);                                    // u2 module_name_index;
            out.writeShort(flags);                                            // u2 module_flags;
            out.writeShort(versionIndex == null ? 0 : versionIndex.cpIndex);  // u2 module_version_index;
        }

        @Override
        public int getLength() {
            // u2:module_name_index) +  u2:module_flags +u2:module_version_index
            return 6;
        }
    }

    /**
     * u2 uses_count;
     * u2 uses_index[uses_count];
     * or
     * u2 requires_count;
     * {  u2 requires_index;
     * u2 requires_flags;
     * u2 requires_version_index;
     * } requires[requires_count];
     */
    private static class SetStruct<T extends ModuleContent.TargetType> implements DataWriter {
        final List<ConstCell> usesList = new ArrayList<>();
        final List<Triplet<ConstCell, Integer, ConstCell>> requiresList = new ArrayList<>();

        SetStruct(Set<T> source,
                  Function<ModuleContent.TargetType, ConstCell> nameFinder,
                  Function<String, ConstCell> versionFinder) {
            Objects.requireNonNull(source);
            source.forEach(e -> {
                if (e.isFlagged()) {
                    requiresList.add(new Triplet<>(
                            nameFinder.apply(e),
                            ((ModuleContent.FlaggedTargetType) e).getFlags(),
                            (((ModuleContent.VersionedFlaggedTargetType) e).getVersion() == null) ?
                                    null :
                                    versionFinder.apply(((ModuleContent.VersionedFlaggedTargetType) e).getVersion())));
                } else {
                    usesList.add(nameFinder.apply(e));
                }
            });
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            if (usesList.isEmpty()) {
                out.writeShort(requiresList.size());                  // u2 requires_count;
                for (Triplet<ConstCell, Integer, ConstCell> r : requiresList) {
                    out.writeShort(r.first.cpIndex);                        // u2 requires_index;
                    out.writeShort(r.second);                           // u2 requires_flags;
                    out.writeShort(r.third == null ? 0 : r.third.cpIndex);  // u2 requires_version_index;
                }
            } else {
                out.writeShort(usesList.size());                      // u2 uses_count;
                for (ConstCell u : usesList)
                    out.writeShort(u.cpIndex);                              // u2 uses_index[uses_count];
            }
        }

        @Override
        public int getLength() {
            return usesList.isEmpty() ?
                    // (u2:requires_count) + (u2:requires_index + u2:requires_flags + u2:requires_version_index) * requires_count
                    2 + 6 * requiresList.size() :
                    // (u2:uses_count) + (u2:uses_index) * uses_count
                    2 + 2 * usesList.size();
        }
    }
}
