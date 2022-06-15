/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

public enum FieldType {
    Array('[', "reference", 1),
    Boolean('Z', "boolean", 1),
    Byte('B', "byte", 1),
    Char('C', "char", 1),
    Double('D', "double", 2),
    Float('F', "float", 1),
    Int('I', "int", 1),
    LReference('L', "reference", 1),
    Long('J', "long", 2),
    QReference('Q', "reference", 1),
    Short('S', "short", 1);

    private final char term;
    private final String type;
    private final int slotsCount;

    FieldType(char term, String type, int slotsCount) {
        this.term = term;
        this.type = type;
        this.slotsCount = slotsCount;
    }



    public static FieldType getFieldType(char term) {
        for (FieldType ft : FieldType.values()) {
            if ( ft.term == term ) {
                return ft;
            }
        }
        return null;
    }

    public int getSlotsCount() {
        return slotsCount;
    }
}
