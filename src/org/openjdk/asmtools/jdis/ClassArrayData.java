/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Base class of the "classes[]" data of attributes:
 * <p>
 * Exceptions_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_exceptions;
 * u2 exception_index_table[number_of_exceptions];
 * }
 * The exception_index_table[i] is an index of a CONSTANT_Class_info structure representing a class type
 * that this method is declared to throw.
 * <p>
 * JEP 181 (Nest-based Access Control): class file 55.0
 * NestMembers_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * <p>
 * JEP 360 (Sealed types): class file 59.65535
 * PermittedSubclasses_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * </p>
 */
public class ClassArrayData extends MemberData {
    JasmTokens.Token token;
    int[] indexes;

    protected <M extends MemberData<ClassData>> ClassArrayData(M classData, JasmTokens.Token token) {
        super(classData);
        this.token = token;
    }

    public ClassArrayData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        int number_of_entities = in.readUnsignedShort();
        if (attribute_length != 2 + number_of_entities * 2) {
            throw new ClassFormatError("%s_attribute: Invalid attribute length".formatted(token.parseKey()));
        }
        indexes = new int[number_of_entities];
        for (int i = 0; i < number_of_entities; i++) {
            indexes[i] = in.readUnsignedShort();
        }
        return this;
    }

    @Override
    public void jasmPrint() {
        if (indexes.length > 3) {
            jasmPrintLong();
        } else {
            jasmPrintShort();
        }
    }

    public void jasmPrintShort() {
        StringBuilder indexes = new StringBuilder();
        StringBuilder names = new StringBuilder();
        int lastIndex = this.indexes.length - 1;
        String eoNames = (printCPIndex) ? "" : ";";
        for (int i = 0; i <= lastIndex; i++) {
            if (printCPIndex) {
                indexes.append("#").append(this.indexes[i]).append(i == lastIndex ? ";" : ", ");
            }
            names.append(pool.StringValue(this.indexes[i])).append(i == lastIndex ? eoNames : ", ");
        }
        printIndent(PadRight(token.parseKey(), getPrintAttributeKeyPadding()));
        if (printCPIndex) {
            if (skipComments) {
                println(indexes.toString());
            } else {
                print(PadRight(indexes.toString(), getPrintAttributeCommentPadding())).println(" // " + names);
            }
        } else {
            println(names.toString());
        }
    }

    public void jasmPrintLong() {
        String name = token.parseKey();
        String locIndent = " ".repeat(name.length());
        int lastIndex = indexes.length - 1;
        for (int i = 0; i <= lastIndex; i++) {
            if (printCPIndex) {
                if (skipComments) {
                    printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                            print("#%d".formatted(indexes[i])).println(i == lastIndex ? ";" : ",");
                } else {
                    printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                            print(PadRight("#%d%s".formatted(indexes[i], (i == lastIndex) ? ";" : ","), getPrintAttributeCommentPadding())).
                            println(" // %s".formatted(pool.StringValue(indexes[i])));
                }
            } else {
                printIndent(PadRight((i == 0) ? name : locIndent, getPrintAttributeKeyPadding())).
                        print(pool.StringValue(indexes[i])).println(i == lastIndex ? ";" : ",");
            }
        }
    }

    @Override
    protected void tablePrint() {
        jasmPrint();
    }
}
