/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.BOOTSTRAPMETHOD;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.BOOTSTRAP_METHOD;

/**
 * BootstrapMethods_attribute {
 * ...
 * { u2 bootstrap_method_ref;
 * u2 num_bootstrap_arguments;
 * u2 bootstrap_arguments[num_bootstrap_arguments];
 * }
 * }
 */
public class BootstrapMethodData extends Element<ClassData> {
    int bsmRef;                              //  u2 bootstrap_method_ref;
    ArrayList<Integer> bsmArguments;         //  u2 bootstrap_arguments[num_bootstrap_arguments];
    int numBsmArgs;                          //  num_bootstrap_arguments
    int indexOffset;

    public BootstrapMethodData(ClassData classData, Container<BootstrapMethodData, ClassData> container) {
        super(classData, container);
        tableToken = BOOTSTRAP_METHOD;
        indexOffset = classData.printProgramCounter ?
                PROGRAM_COUNTER_PLACEHOLDER_LENGTH :
                INSTR_PREFIX_LENGTH - getIndentStep();
    }

    @Override
    public boolean isPrintable() {
        return bsmArguments != null && !bsmArguments.isEmpty();
    }

    /**
     * Read and resolve the bootstrap method data called from ClassData.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        bsmRef = in.readUnsignedShort();
        numBsmArgs = in.readUnsignedShort();
        bsmArguments = new ArrayList<>(numBsmArgs);
        for (int i = 0; i < numBsmArgs; i++) {
            bsmArguments.add(in.readUnsignedShort());
        }
    }

    @Override
    protected void jasmPrint(int index, int size) throws IOException {
        int commentOffset = getCommentOffset() - TABLE_PADDING;
        final BsmInfo bsmInfo = getBsmInfo();

        printIndent(PadRight(BOOTSTRAPMETHOD.parseKey(), TABLE_PADDING));
        if (printCPIndex) {
            if (skipComments) {
                println("#%d;".formatted(bsmRef));
            } else {
                printPadRight("#%d;".formatted(bsmRef), commentOffset).
                        println(" // %s".formatted(formatOperandLine(bsmInfo.strBsm(),
                                getCommentOffset() + getIndentSize(),
                                " // ", InvokeDynamicBreakPositions)));
            }
        } else {
            println(formatOperandLine(
                    "%s".formatted(bsmInfo.strBsm()), TABLE_PADDING + getIndentStep(), "",
                    BootstrapMethodBreakPositions)
            );
        }
        printIndent(PadRight(" ", TABLE_PADDING)).print("{ ");
        if (numBsmArgs > 0) {
            println();
            incIndent(2);
            if (printCPIndex) {
                for (int i = 0; i < numBsmArgs; i++) {
                    int argRef = bsmArguments.get(i);
                    String delim = (i != numBsmArgs - 1) ? "," : "";
                    boolean notLastIdx = i != numBsmArgs - 1;
                    printIndent(PadRight(" ", TABLE_PADDING));
                    if (skipComments) {
                        println("#%d%s".formatted(argRef, delim));
                    } else {
                        String strArg = bsmInfo.cpx2Const().bsmArgWithoutDelimitersAsString(argRef, notLastIdx);
                        strArg = formatOperandLine(strArg, getCommentOffset() + getIndentStep(),
                                " // ",
                                InvokeDynamicBreakPositions);
                        printPadRight("#%d%s".formatted(argRef, delim), commentOffset - getIndentStep() * 2).
                                println(" // %s".formatted(strArg));
                    }
                }
            } else {
                String strArgs = bsmInfo.cpx2Const().bsmArgsAsString(this);
                printIndent(PadRight(" ", TABLE_PADDING - getIndentStep())).
                        println(formatOperandLine(strArgs, TABLE_PADDING + getIndentStep() * 2, "",
                                BootstrapArgumentsBreakPositions));
            }
            decIndent(2);
            printIndent(PadRight(" ", TABLE_PADDING));
        }
        println("}");
        if (index < size - 1)
            println();
    }

    @Override
    protected void tablePrint(int index, int size) throws IOException {
        final int commentOffset = getCommentOffset() - indexOffset - getIndentSize();
        final BsmInfo bsmInfo = getBsmInfo();

        if (index == 0) {
            printIndentLn("%s {".formatted(BOOTSTRAPMETHOD.alias()));
        }
        incIndent();
        printIndent(PadRight(format("%2d:", index), indexOffset));
        if (printCPIndex) {
            if (skipComments) {
                println("#%d;".formatted(bsmRef));
            } else {
                printPadRight("#%d;".formatted(bsmRef), commentOffset).
                        println(" // %s".formatted(formatOperandLine(
                                "%s".formatted(bsmInfo.strBsm()),
                                getCommentOffset() + getIndentStep(),
                                " // ",
                                BootstrapMethodBreakPositions)));
            }
        } else {
            println(formatOperandLine("%s".formatted(bsmInfo.strBsm()), indexOffset + getIndentStep() * 2, "",
                    BootstrapMethodBreakPositions)
            );
        }

        if (numBsmArgs > 0) {
            printIndentLn(" Arguments:");
            if (printCPIndex) {
                int argOffs = commentOffset + indexOffset + getIndentStep() * 2;
                for (int i = 0; i < numBsmArgs; i++) {
                    int argRef = bsmArguments.get(i);
                    boolean notLastIdx = i != numBsmArgs - 1;
                    String delim = (i != numBsmArgs - 1) ? "," : ";";
                    printIndent(PadRight("", indexOffset));
                    if (printCPIndex) {
                        if (skipComments) {
                            println("#%d%s".formatted(argRef, delim));
                        } else {
                            String strArg = bsmInfo.cpx2Const().bsmArgWithoutDelimitersAsString(argRef, notLastIdx);
                            strArg = formatOperandLine(strArg, argOffs, " // ", InvokeDynamicBreakPositions);
                            printPadRight("#%d%s".formatted(argRef, delim), commentOffset).println(" // %s".formatted(strArg));
                        }
                    }
                }
            } else {
                String strArgs = bsmInfo.cpx2Const().bsmArgsAsString(this);
                printIndent(PadRight(" ", indexOffset));
                println(formatOperandLine(strArgs, indexOffset + getIndentStep() * 2, "",
                        BootstrapArgumentsBreakPositions) + ";");
            }
        }
        if (index < size - 1)
            println();
        else {
            decIndent();
            printIndentLn("}");
        }
    }

    private BsmInfo getBsmInfo() {
        String strBsm;
        ConstantPool.Constant<?> cnt = owner.pool.getConst(bsmRef);
        ConstantPool.CPX2 cpx2Const = owner.pool.getCPX2(ConstantPool.TAG.CONSTANT_NULL);
        if (cnt instanceof ConstantPool.CPX2 cpx) {
            cpx2Const = cpx;
            strBsm = cpx.stringVal() + ";";
        } else {
            strBsm = owner.environment.getInfo("info.corrupted_bootstrap_method_ref");
            if (!printCPIndex) {
                strBsm = PadRight("#%d".formatted(bsmRef), CIRCULAR_COMMENT_OFFSET).concat(" // ").concat(strBsm);
            }
        }
        BsmInfo bsmInfo = new BsmInfo(strBsm, cpx2Const);
        return bsmInfo;
    }

    private record BsmInfo(String strBsm, ConstantPool.CPX2 cpx2Const) {
    }
}
