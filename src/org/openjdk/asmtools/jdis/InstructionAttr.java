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

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.structure.StackMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode;
import static org.openjdk.asmtools.jdis.StackMapData.EAttributeType.STACKMAPTABLE;

/**
 * instruction attributes
 */
class InstructionAttr extends MemberData<MethodData> {

    short lineNum = 0;
    boolean referred = false;               // from some other instruction
    ArrayList<CodeData.LocVarData> vars;
    ArrayList<CodeData.LocVarData> endVars;
    ArrayList<TrapData> handlers;
    ArrayList<TrapData> traps;
    ArrayList<TrapData> endTraps;
    StackMapData stackMapEntry;
    ClassData classData;

    public InstructionAttr(MethodData methodData) {
        super(methodData);
        this.classData = methodData.data;
    }

    void addVar(CodeData.LocVarData var) {
        if (vars == null) {
            vars = new ArrayList<>(4);
        }
        vars.add(var);
    }

    void addEndVar(CodeData.LocVarData endVar) {
        if (endVars == null) {
            endVars = new ArrayList<>(4);
        }
        endVars.add(endVar);
    }

    void addTrap(TrapData trap) {
        if (traps == null) {
            traps = new ArrayList<>(4);
        }
        traps.add(trap);
    }

    void addEndTrap(TrapData endTrap) {
        if (endTraps == null) {
            endTraps = new ArrayList<>(4);
        }
        endTraps.add(endTrap);
    }

    void add_handler(TrapData endHandler) {
        if (handlers == null) {
            handlers = new ArrayList<>(4);
        }
        handlers.add(endHandler);
    }

    public void printEnds(int shift) throws IOException {
// prints additional information for instruction:
//  end of local variable and trap scopes;
        if ((endVars != null) && data.printLocalVars) {
            print(enlargedIndent(PadRight(Opcode.opc_endvar.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            println(endVars.stream().map(ev -> Short.toString(ev.slot)).collect(Collectors.joining(",")) + ";");
        }
        if (endTraps != null) {
            print(enlargedIndent(PadRight(Opcode.opc_endtry.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            println(endTraps.stream().map(TrapData::id).collect(Collectors.joining(",")) + ";");
        }
    }

    public void printBegins(int shift) {
// prints additional information for instruction:
// source line number;
// start of exception handler;
// begin of locVar and trap scopes;
        boolean eitherOpt = data.printLineTable || data.printSourceLines;
        boolean bothOpt = data.printLineTable && data.printSourceLines;
        if (eitherOpt && (lineNum != 0)) {
            decIndent();
            if (bothOpt) {
                String srcLine = classData.getSrcLine(lineNum);
                printIndentLn("// " + lineNum + (srcLine != null ? "# " + srcLine : ""));
            } else if (data.printLineTable) {
                printIndentLn("// %d#", lineNum);
            } else if (data.printSourceLines) {
                String srcLine = classData.getSrcLine(lineNum);
                printIndentLn(srcLine != null ? "// " + srcLine : "");
            }
            incIndent();
        }

        if (handlers != null) {
            for (TrapData line : handlers) {
                print(this.enlargedIndent(PadRight(Opcode.opc_catch.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
                if (printCPIndex) {
                    if( skipComments ) {
                        println("%s #%d;", line.id(), line.catch_cpx);
                    } else {
                        print(PadRight(format("%s #%d;", line.id(), line.catch_cpx),
                                getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH - getIndentStep()));
                        println(" // " + (line.catch_cpx == 0 ? "any" : data.pool.getClassName(line.catch_cpx)));
                    }
                } else {
                    println("%s %s;", line.id(), data.pool.getClassName(line.catch_cpx));
                }
            }
        }

        if (traps != null) {
            print(this.enlargedIndent(PadRight(Opcode.opc_try.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            println(traps.stream().map(TrapData::id).collect(Collectors.joining(", ")) + ";");
        }

        if ((vars != null) && data.printLocalVars) {
            for (CodeData.LocVarData line : vars) {
                print(this.enlargedIndent(PadRight(Opcode.opc_var.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
                if (printCPIndex) {
                    if( skipComments ) {
                        println("%d #%d:#%d;", line.slot, line.name_cpx, line.sig_cpx);
                    } else {
                        print(PadRight(format("%d #%d:#%d;", line.slot, line.name_cpx, line.sig_cpx),
                                getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH - getIndentStep()));
                        println(" // %s:%s", data.pool.getName(line.name_cpx), data.pool.getName(line.sig_cpx));
                    }
                } else {
                    println("%d %s:%s;", line.slot, data.pool.getName(line.name_cpx), data.pool.getName(line.sig_cpx));
                }
            }
        }
    }

    public Pair<String, String> getMapListAsString(int[] map) {
        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        for (int k = 0; k < map.length; k++) {
            int fullMapType = map[k];
            int mtVal = fullMapType & 0xFF;
            StackMap.VerificationType mapVerificationType = StackMap.getVerificationType(mtVal,
                    Optional.of((s)-> environment.printErrorLn(s)));
            String prefix = k == 0 ? "" : " ";
            int argument = fullMapType >> 8;
            switch (mapVerificationType) {
                case ITEM_Object -> {
                    if (data.printCPIndex) {
                        left.append(prefix).append("#").append(argument);
                    }
                    right.append(prefix).append(data.pool.ConstantStrValue(argument));
                }
                case ITEM_NewObject -> {
                    if (data.printCPIndex) {
                        left.append(prefix).append(mtVal);
                        left.append(" ").append(data.lP).append(argument);
                    }
                    right.append(prefix).append(mapVerificationType.printName());
                    right.append(" ").append(data.lP).append(argument);
                }
                default -> {
                    if (data.printCPIndex) {
                        left.append(prefix).append(mtVal);
                    }
                    right.append(prefix).append(mapVerificationType.printName());
                }
            }
            if (data.printCPIndex) {
                left.append((k == (map.length - 1) ? ';' : ','));
            }
            right.append((k == (map.length - 1) ? ';' : ','));
        }
        return new Pair<>(left.toString(), right.toString());
    }

    /**
     * @param shift how are shifted a list of verification types of locals_map/stack_map
     * @return true if something is printed
     */
    public boolean printStackMap(int shift) {
        if (stackMapEntry == null) {
            return false;
        }
        boolean printed = false;
        int mapShift = getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH - getIndentStep();
        if (stackMapEntry.stackFrameType != null) {
            printPadRight(Opcode.opc_stack_frame_type.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1);
            if( printCPIndex && !skipComments ) {
                print(PadRight(stackMapEntry.stackFrameType.tagName() + ";", mapShift)).
                        println(" // frame_type " + stackMapEntry.stackFrameTypeValue);
            } else {
                println(stackMapEntry.stackFrameType.tagName() + ";");
            }
            printed = true;
        }
        int[] map = stackMapEntry.lockMap;
        if ((map != null) && (map.length > 0)) {
            Pair<String, String> line = getMapListAsString(map);
            if (stackMapEntry.type == STACKMAPTABLE) {  // StackMapTable exists
                print(this.enlargedIndent(PadRight(Opcode.opc_locals_map.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            } else {                                    // == StackMap version < 50 Class file has an implicit stack map attribute
                print(PadRight(Opcode.opc_locals_map.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1));
            }
            if (printCPIndex) {
                if( skipComments ) {
                    println(line.first);
                } else {
                    print(PadRight(line.first, mapShift));
                    mapShift = max(mapShift, line.first.length());
                    print(" // ");
                }
            }
            if( !printCPIndex || (printCPIndex && !skipComments) ) {
                println(line.second);
            }
            printed = true;
        }
        map = stackMapEntry.stackMap;
        if ((map != null) && (map.length > 0)) {
            Pair<String, String> line = getMapListAsString(map);
            if (stackMapEntry.type == STACKMAPTABLE) {
                print(this.enlargedIndent(
                        PadRight(Opcode.opc_stack_map.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            } else {   // version < 50 Class file has an implicit stack map attribute
                print(this.enlargedIndent(
                        PadRight(Opcode.opc_stack_map.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1), shift));
            }
            if (printCPIndex) {
                if( skipComments ) {
                    println(line.first);
                } else {
                    print(PadRight(line.first, mapShift));
                    print(" // ");
                }
            }
            if( !printCPIndex || (printCPIndex && !skipComments) ) {
                println(line.second);
            }
            printed = true;
        }
        if (!printed) {
            // empty attribute should be printed anyway - it should not be eliminated after jdis/jasm cycle
            if (stackMapEntry.type == STACKMAPTABLE) {
                println(this.enlargedIndent(Opcode.opc_locals_map.parseKey() + ";", STACKMAP_TYPE_PLACEHOLDER_LENGTH + 1));
            } else {
                println(Opcode.opc_locals_map.parseKey() + ";");
            }
        }
        return true;
    }
}
