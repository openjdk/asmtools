/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.TypeAnnotationTypes.TypePathEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * JVMS 4.7.20.2. The type_path structure
 * <p>
 * type_path {
 * u1 path_length;
 * {   u1 type_path_kind;
 * u1 type_argument_index;
 * } path[path_length];
 * }
 */
public class TypeAnnotationTypePathData implements DataWriter {

    private ArrayList<TypePathEntry> typePathEntries = new ArrayList<>();

    public void addTypePathEntry(TypePathEntry entry) {
        typePathEntries.add(entry);
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeByte(typePathEntries.size());
        for (TypePathEntry entry : typePathEntries) {
            out.writeByte(entry.getTypePathKind());
            out.writeByte(entry.getTypeArgumentIndex());
        }
    }

    @Override
    public int getLength() {
        return 1 + typePathEntries.size() * 2;
    }

    @Override
    public String toString() {
        if (typePathEntries.size() > 0) {
            StringBuilder sb = new StringBuilder("{ ");
            sb.append(typePathEntries.stream().map(e -> e.toString()).collect(Collectors.joining(", ")));
            sb.append(" }");
            return sb.toString();
        }
        return "";
    }

    /**
     * jdis: get a string representation of type path structure for kids printing
     */
    public String toPrintString() {
        return toString();
    }
}
