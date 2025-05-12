/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

/**
 * CPVisitor base class defining a visitor for decoding constants.
 */
public interface CPVisitor< C extends ConstantPool.ConstValue_Cell, P extends ConstantPool.ConstValue_Pair<C, C>> {
    default void visit(ConstValue constValue) {
        switch (constValue.tag) {
            case CONSTANT_ZERO:
                break; // ignore
            case CONSTANT_METHODTYPE,
                 CONSTANT_STRING, CONSTANT_L_STRING,
                 CONSTANT_CLASS, CONSTANT_C_CLASS,
                 CONSTANT_MODULE, CONSTANT_PACKAGE:
                visitConstValueCell((C) constValue);
                break;
            case CONSTANT_METHODREF,  CONSTANT_METHOD,
                 CONSTANT_FIELDREF,  CONSTANT_FIELD,
                CONSTANT_INTERFACEMETHODREF, CONSTANT_INTERFACEMETHOD,
                CONSTANT_NAMEANDTYPE:
                visitConstValueRefCell((P) constValue);
                break;
            case CONSTANT_METHODHANDLE:
                visitMethodHandle((ConstantPool.ConstValue_MethodHandle) constValue);
                break;
            case CONSTANT_UTF8, CONSTANT_ASCIZ,
                 CONSTANT_INTEGER, CONSTANT_INT,
                 CONSTANT_BYTE, CONSTANT_C_BYTE,
                 CONSTANT_CHAR, CONSTANT_C_CHAR,
                 CONSTANT_SHORT, CONSTANT_C_SHORT,
                 CONSTANT_C_BOOLEAN, CONSTANT_BOOLEAN,
                 CONSTANT_FLOAT, CONSTANT_C_FLOAT,
                 CONSTANT_DOUBLE, CONSTANT_C_DOUBLE,
                 CONSTANT_LONG, CONSTANT_C_LONG,
                 CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC:
                // default
            default:
        }
    }
    void visitConstValueCell(C constValue);
    void visitConstValueRefCell(P constValue);
    void visitMethodHandle(ConstantPool.ConstValue_MethodHandle constValue);
}
