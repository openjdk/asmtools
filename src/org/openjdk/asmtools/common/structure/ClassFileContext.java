/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.structure;

/**
 * Class File context CF_Context enums
 */
public enum ClassFileContext {

    NONE(0x0000, "n/a"),
    CLASS(0x0001, "class"),
    FIELD(0x0002, "field"),
    METHOD(0x0004, "method"),
    INNER_CLASS(0x0008, "inner-class"),
    MODULE(0x0010, "module"),
    REQUIRES(0x0020, "requires"),
    EXPORTS(0x0040, "exports"),
    OPENS(0x0080, "opens"),
    METHOD_PARAMETERS(0x0100, "method parameters"),
    MODULE_DIRECTIVES(0x0020 | 0x0040 | 0x0080, "module directives");

    private final int id;
    private final String printVal;

    ClassFileContext(int id, String print) {
        this.id = id;
        printVal = print;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return printVal;
    }

    public boolean isOneOf(ClassFileContext... contexts) {
        for (ClassFileContext cfc : contexts) {
            if ( (cfc.id & this.id) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean belongToContextOf(EModifier modifier) {
        return (this.id & modifier.getAllovedContextMask()) != 0;
    }
}
