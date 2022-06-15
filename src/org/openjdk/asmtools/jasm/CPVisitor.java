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
package org.openjdk.asmtools.jasm;

/**
 * CPVisitor base class defining a visitor for decoding constants.
 */
public interface CPVisitor< C extends ConstantPool.ConstValue_Cell, P extends ConstantPool.ConstValue_Pair<C, C>> {
    default void visit(ConstValue constValue) {
        switch (constValue.tag) {
            case CONSTANT_ZERO:
                // just ignore
                break;
            case CONSTANT_METHODTYPE:
            case CONSTANT_STRING:
            case CONSTANT_CLASS:
            case CONSTANT_MODULE:
            case CONSTANT_PACKAGE:
                visitConstValueCell((C) constValue);
                break;
            case CONSTANT_METHODREF:
            case CONSTANT_FIELDREF:
            case CONSTANT_INTERFACEMETHODREF:
            case CONSTANT_NAMEANDTYPE:
                visitConstValueRefCell((P) constValue);
                break;
            case CONSTANT_METHODHANDLE:
                visitMethodHandle((ConstantPool.ConstValue_MethodHandle) constValue);
                break;
            case CONSTANT_UTF8:
            case CONSTANT_INTEGER:
            case CONSTANT_FLOAT:
            case CONSTANT_DOUBLE:
            case CONSTANT_LONG:
            case CONSTANT_DYNAMIC:
            case CONSTANT_INVOKEDYNAMIC:
            default:
        }
    }
    void visitConstValueCell(C constValue);
    void visitConstValueRefCell(P constValue);
    void visitMethodHandle(ConstantPool.ConstValue_MethodHandle constValue);
}
