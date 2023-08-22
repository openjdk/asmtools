/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.CompilerConstants;

import static java.lang.String.format;

/**
 * Constant Pool Tag Visitor base class defining a visitor for decoding constants.
 */
public abstract class CPTagVisitor<R> implements CompilerConstants {

    public CPTagVisitor() { }

    public final R visit(ClassFileConst.ConstType tag) {
        switch (tag) {
            case CONSTANT_UTF8, CONSTANT_ASCIZ -> {
                return visitUTF8();
            }
            case CONSTANT_INTEGER, CONSTANT_INTEGER_BYTE, CONSTANT_INTEGER_BOOLEAN, CONSTANT_INTEGER_CHAR, CONSTANT_INTEGER_SHORT -> {
                return visitInteger(tag);
            }
            case CONSTANT_FLOAT -> {
                return visitFloat();
            }
            case CONSTANT_DOUBLE -> {
                return visitDouble();
            }
            case CONSTANT_LONG -> {
                return visitLong();
            }
            case CONSTANT_METHODTYPE -> {
                return visitMethodType();
            }
            case CONSTANT_STRING -> {
                return visitString();
            }
            case CONSTANT_CLASS -> {
                return visitClass();
            }
            case CONSTANT_METHODREF -> {
                return visitMethod();
            }
            case CONSTANT_FIELDREF -> {
                return visitField();
            }
            case CONSTANT_INTERFACEMETHODREF -> {
                return visitInterfaceMethod();
            }
            case CONSTANT_NAMEANDTYPE -> {
                return visitNameAndType();
            }
            case CONSTANT_METHODHANDLE -> {
                return visitMethodHandle();
            }
            case CONSTANT_DYNAMIC -> {
                return visitDynamic();
            }
            case CONSTANT_INVOKEDYNAMIC -> {
                return visitInvokeDynamic();
            }
            case CONSTANT_PACKAGE -> {
                return visitPackage();
            }
            case CONSTANT_MODULE -> {
                return visitModule();
            }
        }
        throw new RuntimeException(
                format("The Constant Type \"%s\" does not have a corresponding visitor function.", tag.printVal()));
    }

    public abstract R visitUTF8();

    // The following types are allowed: CONSTANT_INTEGER, CONSTANT_INTEGER_BYTE,
    // CONSTANT_INTEGER_CHAR, CONSTANT_INTEGER_SHORT, CONSTANT_INTEGER_BOOLEAN
    public abstract R visitInteger(ClassFileConst.ConstType tag);

    public abstract R visitFloat();

    public abstract R visitDouble();

    public abstract R visitLong();

    public abstract R visitMethodType();

    public abstract R visitString();

    public abstract R visitClass();

    public abstract R visitMethod();

    public abstract R visitField();

    public abstract R visitInterfaceMethod();

    public abstract R visitNameAndType();

    public abstract R visitMethodHandle();

    public abstract R visitDynamic();

    public abstract R visitInvokeDynamic();

    public abstract R visitModule();

    public abstract R visitPackage();
}
