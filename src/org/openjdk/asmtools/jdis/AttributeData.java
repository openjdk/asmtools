/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.ToolLogger;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.TableFormatModel;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Supplier;

import static org.openjdk.asmtools.jasm.TableFormatModel.Token.ENCLOSING_METHOD;

/**
 * Base class for attributes: SourceFile, NestHost, EnclosingMethod with format:
 * <p>
 * attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;     SourceFile, NestHost: 2     EnclosingMethod: 4
 * u2 constant_pool_index;  sourcefile_index, host_class_index, class_index
 * ****
 * u2 method_index;         Only if EnclosingMethod
 */
public abstract class AttributeData<A extends AttributeData<A>> extends Indenter {
    protected ConstantPool pool;
    protected ToolLogger logger;

    protected int attribute_length = 0;
    // Constant pool: SourceFile, NestHost class, Inner class.
    protected String name = null;
    // sourcefile_index, host_class_index, class_index
    protected int cpx;
    // only applicable to EnclosingMethod
    // EnclosingMethod: If the current class is not immediately enclosed by a method or constructor,
    // then the value of the method_index (that is equal to CONSTANT_NameAndType_info_index;) item must be zero.
    protected int methodCpx = 0;

    // Utility functions
    Supplier<String> idxStringSupplier;
    private Printable indexPrinter = () -> println(idxStringSupplier.get());
    private Printable namePrinter;
    private Printable indexAndNamePrinter = () -> print(PadRight(idxStringSupplier.get(), getPrintAttributeCommentPadding())).
            println((name != null) ? " // %s".formatted(name) : "");

    protected AttributeData(ClassData classData, TableFormatModel.Token token) {
        super(classData.toolOutput);
        pool = classData.pool;
        logger = classData.data.environment.getLogger();
        tableToken = token;
        switch (tableToken) {
            case NEST_HOST, SOURCE_FILE -> {
                attribute_length = 2;
                idxStringSupplier = () -> "#%d;".formatted(cpx);
                namePrinter = () -> println("\"%s\";".formatted(name != null ? name : "???"));
            }
            case ENCLOSING_METHOD -> {
                attribute_length = 4;
                idxStringSupplier = () -> "#%d:#%d;".formatted(cpx, methodCpx);
                namePrinter = () -> println("%s;".formatted(name));
            }
            default -> throw new RuntimeException(
                    "Implementation of \"%s\" is not supported".formatted(tableToken.parseKey()));
        }
    }

    public A read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        if (attribute_length != this.attribute_length) {
            if (bestEffort) {
                logger.error("err.invalid.attribute.length",
                        tableToken.getAttribute().printValue(), attribute_length);
            } else {
                throw new FormatError(logger,
                        "err.invalid.attribute.length",
                        tableToken.getAttribute().printValue(), attribute_length);
            }
        }
        this.cpx = in.readUnsignedShort();
        if (tableToken == ENCLOSING_METHOD) {
            this.methodCpx = in.readUnsignedShort();
        }
        return (A) this;
    }

    @Override
    protected void jasmPrint() {
        calculateName();
        printIndent(PadRight(tableToken.getJasmToken().parseKey(), getPrintAttributeKeyPadding()));
        if (printCPIndex) {
            if (skipComments) {
                indexPrinter.print();
            } else {
                indexAndNamePrinter.print();
            }
        } else {
            namePrinter.print();
        }
    }

    /**
     * There are no differences between the simple (jasm) and extended (table) presentations of NestHost,
     * SourceFile, EnclosingMethod attribute, and NestMembers attribute.
     */
    @Override
    protected void tablePrint() {
        this.jasmPrint();
    }

    protected abstract String calculateName();

    @FunctionalInterface
    public interface Printable {
        void print();
    }
}
