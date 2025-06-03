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

import org.openjdk.asmtools.asmutils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.FRAMETYPE;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode;

/**
 * instruction attributes
 */
class InstructionAttr extends MemberData<MethodData> {
    short lineNum = 0;
    boolean referred = false;               // from some other instruction
    ArrayList<LocalVariableData> vars;
    ArrayList<LocalVariableTypeData> types;
    ArrayList<LocalVariableData> endVars;
    ArrayList<LocalVariableTypeData> endTypes;
    ArrayList<TrapData> handlers;
    ArrayList<TrapData> traps;
    ArrayList<TrapData> endTraps;
    List<StackMapData> stackMapWrappers;
    StackMapData stackMapEntry;
    ClassData classData;
    private int attributeOffset;

    public InstructionAttr(MethodData methodData) {
        super(methodData);
        this.classData = methodData.data;
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

    public void printBegins(int shift) {
        this.attributeOffset = shift;
        // Prints additional information for instruction:
        // source line number;
        printInlinedLineNumber();
        // begin of exception handler;
        printBeginOfExceptionHandlers(shift);
        // begin of trap scores;
        printBeginOfTrapScores(shift);
        // begin of locVar and locVarTypes
        printBeginOfLocVars(shift);
    }

    public void printEnds(int shift) {
        // Prints additional information for instruction:
        // end of local variables, local variable types and trap scopes;
        if (endTypes != null && !tableFormat) {
            print(enlargedIndent(PadRight(Opcode.opc_endtype.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
            println(endTypes.stream().map(ev -> Short.toString(ev.slot)).collect(Collectors.joining(",")) + ";");
        }
        if (endVars != null && !tableFormat) {
            print(enlargedIndent(PadRight(Opcode.opc_endvar.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
            println(endVars.stream().map(ev -> Short.toString(ev.slot)).collect(Collectors.joining(",")) + ";");
        }
        if (endTraps != null) {
            print(enlargedIndent(PadRight(Opcode.opc_endtry.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
            println(endTraps.stream().map(TrapData::id).collect(Collectors.joining(",")) + ";");
        }
    }

    /**
     * @param shift how is shifted a list of verification types of locals_map/stack_map
     * @return true if something is printed
     */
    public boolean printStackMap_Table(int shift) {
        // will the stackmap(table) be printed as table if the table is chosen?
        if (tableFormat || (stackMapEntry == null && stackMapWrappers == null)) {
            return false;
        } else if (stackMapEntry != null) {
            return stackMapEntry.belongsToStackMapTable() ? printStackMapTable(shift) : printStackMap(shift);
        } else {
            return stackMapWrappers.getFirst().belongsToStackMapTable() ? printStackMapTable(shift) : printStackMap(shift);
        }
    }

    private boolean printStackMapTable(int shift) {
        int mapShift = getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH;
        boolean wrapped = stackMapWrappers != null;
        String opCodeName = Opcode.opc_stack_frame_type.parseKey();
        String prefix = "";
        String listPrefix = INDENT_STRING;
        // print wrappers if found
        if (wrapped) {
            for (int i = 0; i < stackMapWrappers.size(); i++) {
                StackMapData smd = stackMapWrappers.get(i);
                if (i > 0) {
                    opCodeName = Opcode.opc_frame_type.parseKey();
                    prefix = INDENT_STRING;
                    listPrefix = prefix + INDENT_STRING;
                }
                printPadRight(prefix + opCodeName, STACKMAP_TYPE_PLACEHOLDER_LENGTH);
                if (printCPIndex && !skipComments) {
                    print(PadRight(smd.stackEntryType.tagName() + ";", mapShift)).
                            println(" // %s %s".formatted(FRAMETYPE.parseKey(), smd.stackEntryTypeValue));
                } else {
                    println(smd.stackEntryType.tagName() + ";");
                }
                int[] unsetFields = smd.unsetFields;
                printFields(unsetFields, shift);
                if ((unsetFields == null) || (unsetFields.length == 0)) {
                    println(this.enlargedIndent(listPrefix + Opcode.opc_unset_fields.parseKey() + ";", shift));
                }
                print(enlargedIndent(attributeOffset));
            }
        }
        opCodeName = Opcode.opc_stack_frame_type.parseKey();
        // print StackMap entry
        if (stackMapEntry != null) {
            if (wrapped) {
                opCodeName = Opcode.opc_frame_type.parseKey();
                prefix += INDENT_STRING;
//              println("{").print(enlargedIndent(attributeOffset));
            }
            listPrefix = prefix + INDENT_STRING;
            printPadRight(prefix + opCodeName, STACKMAP_TYPE_PLACEHOLDER_LENGTH);
            if (printCPIndex && !skipComments) {
                print(PadRight(stackMapEntry.stackEntryType.tagName() + ";", mapShift)).
                        println(" // %s %s".formatted(FRAMETYPE.parseKey(), stackMapEntry.stackEntryTypeValue));
            } else {
                println(stackMapEntry.stackEntryType.tagName() + ";");
            }

            int[] lockMap = stackMapEntry.lockMap;
            if ((lockMap == null) || (lockMap.length == 0)) {
                if (stackMapEntry.stackEntryType.hasLocalMap()) {
                    println(this.enlargedIndent(listPrefix + Opcode.opc_locals_map.parseKey() + ";", shift));
                }
            } else {
                mapShift = printEntries(stackMapEntry.getMapListAsString(lockMap, ""),
                        listPrefix + Opcode.opc_locals_map.parseKey(), shift, mapShift);
            }

            int[] stackMap = stackMapEntry.stackMap;
            if ((stackMap == null) || (stackMap.length == 0)) {
                if (stackMapEntry.stackEntryType.hasStackMap()) {
                    println(this.enlargedIndent(listPrefix + Opcode.opc_stack_map.parseKey() + ";", shift));
                }
            } else {
                printEntries(stackMapEntry.getMapListAsString(stackMap, ""),
                        listPrefix + Opcode.opc_stack_map.parseKey(), shift, mapShift);
            }
//        if (wrapped) {
//            println(enlargedIndent(attributeOffset) + "}");
//        }
        }
        return stackMapWrappers != null || stackMapEntry != null;
    }

    private boolean printStackMap(int shift) {
        int mapShift = getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH;
        printPadRight(Opcode.opc_stack_map_frame.parseKey() + ";", STACKMAP_TYPE_PLACEHOLDER_LENGTH);
        if (printCPIndex && !skipComments) {
            print(PadRight(" ", mapShift)).println(" // offset " + stackMapEntry.frame_pc);
        } else {
            println();
        }
        mapShift = printEntries(stackMapEntry.getMapListAsString(stackMapEntry.lockMap, ""),
                Opcode.opc_locals_map.parseKey(), shift, mapShift);
        printEntries(stackMapEntry.getMapListAsString(stackMapEntry.stackMap, ""),
                Opcode.opc_stack_map.parseKey(), shift, mapShift);
        return true;
    }

    private void printFields(int[] unsetFields, int shift) {
        int mapShift = getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH;
        if (unsetFields != null && (unsetFields.length > 0)) {
            final int limit = unsetFields.length - 1;
            Pair<List<String>, List<String>> line = stackMapEntry.getFieldListAsString(unsetFields);
            String left = line.first.stream().collect(Collectors.joining(", ")).concat(";");
            String right = line.second.stream().collect(Collectors.joining(", ")).concat(";");
            String title = enlargedIndent(PadRight(INDENT_STRING + Opcode.opc_unset_fields.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift);
            print(title);
            // empties the title to use it as indent.
            title = nCopies(title.length());
            if (printCPIndex) {
                if (skipComments) {
                    println(left);
                } else {
                    if (limit == 0) {
                        print(PadRight(left, mapShift)).println(" // " + right);
                    } else {
                        print(PadRight(line.first.get(0).concat(","), mapShift)).println(" // " + line.second.get(0).concat(","));
                        for (int i = 1; i <= limit; i++) {
                            String delim = i == limit ? ";" : ",";
                            String id = line.first.get(i).concat(delim);
                            String field = line.second.get(i).concat(delim);
                            print(title).print(PadRight(id, mapShift)).println(" // " + field);
                        }
                    }
                }
            } else {
                if (limit == 0) {
                    println(right);
                } else {
                    println(line.second.getFirst().concat(","));
                    for (int i = 1; i <= limit; i++) {
                        String delim = i == limit ? ";" : ",";
                        String field = line.second.get(i).concat(delim);
                        print(title).println(field);
                    }
                }
            }
        }
    }


    private int printEntries(Pair<String, String> entriesLine, String title, int shift, int mapShift) {
        if (entriesLine != null) {
            boolean isEmpty = entriesLine.first.isEmpty() && entriesLine.second.isEmpty();
            print(this.enlargedIndent(PadRight(title + (isEmpty ? ";" : ""), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
            if (printCPIndex) {
                if (skipComments) {
                    println(entriesLine.first);
                } else {
                    print(PadRight(entriesLine.first, mapShift));
                    mapShift = max(mapShift, entriesLine.first.length());
                    if( !isEmpty ) {
                        print(" // ");
                    }
                }
            }
            if (!printCPIndex || (printCPIndex && !skipComments)) {
                println(entriesLine.second);
            }
        }
        return mapShift;
    }

    void addVar(LocalVariableData var) {
        if (vars == null) {
            vars = new ArrayList<>(4);
        }
        vars.add(var);
    }

    void addType(LocalVariableTypeData type) {
        if (types == null) {
            types = new ArrayList<>(4);
        }
        types.add(type);
    }


    void addEndType(LocalVariableTypeData endType) {
        if (endTypes == null) {
            endTypes = new ArrayList<>(4);
        }
        endTypes.add(endType);
    }

    void addEndVar(LocalVariableData endVar) {
        if (endVars == null) {
            endVars = new ArrayList<>(4);
        }
        endVars.add(endVar);
    }

    private void printInlinedLineNumber() {
        boolean eitherOpt = data.printLineTableNumbers || data.printLineTableLines;
        boolean bothOpt = data.printLineTableNumbers && data.printLineTableLines;
        if (eitherOpt && (lineNum != 0)) {
            decIndent();
            if (bothOpt) {
                String srcLine = classData.getSrcLine(lineNum);
                printIndentLn("// " + lineNum + (srcLine != null ? "# " + srcLine : ""));
            } else if (data.printLineTableNumbers) {
                printIndentLn("// %d#", lineNum);
            } else if (data.printLineTableLines) {
                String srcLine = classData.getSrcLine(lineNum);
                printIndentLn(srcLine != null ? "// " + srcLine : "");
            }
            incIndent();
        }
    }

    private void printBeginOfExceptionHandlers(int shift) {
        if (handlers != null) {
            for (TrapData line : handlers) {
                print(this.enlargedIndent(PadRight(Opcode.opc_catch.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
                if (printCPIndex) {
                    if (skipComments) {
                        println("%s #%d;", line.id(), line.catch_cpx);
                    } else {
                        print(PadRight(format("%s #%d;", line.id(), line.catch_cpx),
                                getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH));
                        println(" // " + (line.catch_cpx == 0 ? "any" : data.pool.getClassName(line.catch_cpx)));
                    }
                } else {
                    println("%s %s;", line.id(), data.pool.getClassName(line.catch_cpx));
                }
            }
        }
    }

    private void printBeginOfTrapScores(int shift) {
        if (traps != null) {
            print(this.enlargedIndent(PadRight(Opcode.opc_try.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
            println(traps.stream().map(TrapData::id).collect(Collectors.joining(", ")) + ";");
        }
    }

    private void printBeginOfLocVars(int shift) {
        if ((vars != null) && !tableFormat) {
            for (LocalVariableData line : vars) {
                print(this.enlargedIndent(PadRight(Opcode.opc_var.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
                if (printCPIndex) {
                    if (skipComments) {
                        println("%d #%d:#%d;", line.slot, line.name_cpx, line.sig_cpx);
                    } else {
                        print(PadRight(format("%d #%d:#%d;", line.slot, line.name_cpx, line.sig_cpx),
                                getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH));
                        println(" // %s:%s", data.pool.getName(line.name_cpx), data.pool.getName(line.sig_cpx));
                    }
                } else {
                    println("%d %s:%s;", line.slot, data.pool.getName(line.name_cpx), data.pool.getName(line.sig_cpx));
                }
            }
        }
        if (types != null && !tableFormat) {
            for (LocalVariableTypeData type : types) {
                print(this.enlargedIndent(PadRight(Opcode.opc_type.parseKey(), STACKMAP_TYPE_PLACEHOLDER_LENGTH), shift));
                if (printCPIndex) {
                    if (skipComments) {
                        println("%d #%d:#%d;", type.slot, type.name_cpx, type.sig_cpx);
                    } else {
                        print(PadRight(format("%d #%d:#%d;", type.slot, type.name_cpx, type.sig_cpx),
                                getCommentOffset() - STACKMAP_TYPE_PLACEHOLDER_LENGTH));
                        println(" // %s:%s", data.pool.getName(type.name_cpx), data.pool.getName(type.sig_cpx));
                    }
                } else {
                    println("%d %s:%s;", type.slot, data.pool.getName(type.name_cpx), data.pool.getName(type.sig_cpx));
                }
            }
        }
    }
}
