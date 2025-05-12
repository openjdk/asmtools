/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.INNERCLASS;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.INNER_CLASSES;

/**
 * 4.7.6. The InnerClasses Attribute
 */
public class InnerClassData extends Element<ClassData> implements Measurable {
    int inner_class_info_index;
    int outer_class_info_index;
    int inner_name_index;
    int access;

    private int calculatedCommentOffset = 0;
    private String jasmPrefix = Indent(PadRight(INNERCLASS.parseKey(), TABLE_PADDING));

    public InnerClassData(ClassData classData, Container<InnerClassData, ClassData> container) {
        super(classData, container);
        this.tableToken = INNER_CLASSES;
    }

    @Override
    public int getCommentOffset() {
        if (this.calculatedCommentOffset == 0) {
            if (isTableOutput()) {
                this.calculatedCommentOffset = container.getCommentOffset() - getIndentStep();
                this.calculatedCommentOffset = max(this.calculatedCommentOffset, getMaxPrintSize());
            } else {
                this.calculatedCommentOffset = container.getCommentOffset() - getIndentStep();
                calculatedCommentOffset = max(this.calculatedCommentOffset, getMaxPrintSize() + jasmPrefix.length());
            }
        }
        return calculatedCommentOffset;
    }

    public void read(DataInputStream in) throws IOException {
        inner_class_info_index = in.readUnsignedShort();
        outer_class_info_index = in.readUnsignedShort();
        inner_name_index = in.readUnsignedShort();
        access = in.readUnsignedShort();
    }  // end read

    @Override
    public void jasmPrint(int index, int size) throws IOException {
        String line = jasmPrefix.concat(EModifier.asKeywords(access, ClassFileContext.INNER_CLASS));
        if (printCPIndex) {
            line = getClassDefinitionString(line);
            if (skipComments) {
                print(line);
            } else {
                print(PadRight(line, this.getCommentOffset())).print(" // ");
            }
        } else {
            print(line);
        }
        if (!printCPIndex || (printCPIndex && !skipComments)) {
            if (inner_name_index != 0)
                print(owner.pool.getName(inner_name_index) + " = ");
            if (inner_class_info_index != 0)
                print(owner.pool.ConstantStrValue(inner_class_info_index));
            if (outer_class_info_index != 0)
                print(format(" of %s", owner.pool.ConstantStrValue(outer_class_info_index)));
            println(owner.printCPIndex && !skipComments ? "" : ";");
        } else {
            println();
        }
    }

    @Override
    public void tablePrint(int index, int size) throws IOException {
        if (index == 0) {
            printIndentLn("%s {".formatted(INNERCLASS.alias()));
        }
        incIndent();
        String prefix = EModifier.asKeywords(access, ClassFileContext.INNER_CLASS);
        if (printCPIndex) {
            prefix = getClassDefinitionString(prefix);
            if (skipComments) {
                printIndent(prefix);
            } else {
                printIndentPadRight(prefix, this.getCommentOffset()).print(" // ");
            }
        } else {
            printIndent(prefix);
        }

        if (!printCPIndex || (printCPIndex && !skipComments)) {
            if (inner_name_index != 0)
                print("%s = ".formatted(owner.pool.getName(inner_name_index)));
            if (inner_class_info_index != 0)
                print(owner.pool.ConstantStrValue(inner_class_info_index));
            if (outer_class_info_index != 0)
                print(format(" of %s", owner.pool.ConstantStrValue(outer_class_info_index)));
            println(owner.printCPIndex && !skipComments ? "" : ";");
        } else {
            println();
        }
        decIndent();
        if (index == size - 1) {
            printIndentLn("}");
        }
    }

    private String getClassDefinitionString(String line) {
        if (inner_name_index != 0)
            line = line.concat("#%d = ".formatted(inner_name_index));
        if (inner_class_info_index != 0)
            line = line.concat("#%d".formatted(inner_class_info_index));
        if (outer_class_info_index != 0) {
            line = line.concat(" of #%d".formatted(outer_class_info_index));
        }
        return line.concat(";");
    }

    @Override
    public int getPrintSize() {
        String line = getClassDefinitionString(EModifier.asKeywords(access, ClassFileContext.INNER_CLASS));
        return line.length() + 1;
    }

    @Override
    public void setMaxPrintSize(int size) {
        this.maxSize = size;
    }

    @Override
    public int getMaxPrintSize() {
        return this.maxSize;
    }
} // end InnerClassData
