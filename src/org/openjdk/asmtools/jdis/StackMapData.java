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
import org.openjdk.asmtools.asmutils.Range;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.StackMap;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openjdk.asmtools.asmutils.StringUtils.mapToHexString;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_StackMap;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_StackMapTable;
import static org.openjdk.asmtools.common.structure.StackMap.EntryType.EARLY_LARVAL;
import static org.openjdk.asmtools.common.structure.StackMap.EntryType.FULL_FRAME;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.STACK_MAP;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.STACK_MAP_TABLE;

/**
 * Represents one entry of StackMapTable  attribute:
 * <p>
 * union stack_map_entry {
 * same_frame;
 * same_locals_1_stack_item_frame;
 * same_locals_1_stack_item_frame_extended;
 * chop_frame;
 * same_frame_extended;
 * append_frame;
 * full_frame;
 * }
 * <p>
 * or StackMap attribute
 * <p>
 * stack_map_entry {
 * u2 offset;
 * u2 number_of_locals;
 * verification_type_info locals[number_of_locals];
 * u2 number_of_stack_items;
 * verification_type_info stack[number_of_stack_items];
 * }
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.3">Code Attribute</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.4">StackMapTable Attribute</a>
 */
public class StackMapData extends MemberData<CodeData> {

    static Range<Integer> range = new Range<>(247, 255);

    private final EAttribute attribute;

    StackMap.EntryType stackEntryType;
    // stack frame type value
    int stackEntryTypeValue;
    boolean isWrapped = false;
    int wrapLevel = 0;   // isWrapped == false
    int frame_pc;
    int offset;
    int[] lockMap;
    int[] stackMap;
    int[] unsetFields;

    private Printer printer;
    private int doubleIndent = getIndentStep() * 2;
    private int tripleIndent = getIndentStep() * 3;
    private String intLine = "";
    private String strLine = "";
    private int shift;
    private String tableHeader = "";

    @FunctionalInterface
    public interface Printer {
        void accept(Integer t, Integer u) throws IOException;
    }

    /**
     * Prints the StackMap data in Jasm format.
     *
     * @param index the index of the current entry
     * @param size  the total number of entries
     * @throws IOException if an I/O error occurs while printing
     */
    @Override
    protected void jasmPrint(int index, int size) throws IOException {
        incIndent();
        if (index == 0) {
            printIndentLn(tableHeader);
        }
        printer.accept(index, size);
        decIndent();
    }

    private void stackMapPrinter(int index, int size) throws IOException {
        println(enlargedIndent(intLine.formatted(BYTECODEOFFSET.parseKey(), frame_pc), getIndentStep()));
        for (Map.Entry<String, Pair<Boolean, int[]>> entry : Map.of(
                "%-10s".formatted(STACKMAP.parseKey()), new Pair<>(stackMap != null, stackMap),
                "%-10s".formatted(LOCALSMAP.parseKey()), new Pair<>(lockMap != null, lockMap)).entrySet()) {
            if (entry.getValue().first) {
                int[] map = entry.getValue().second;
                Pair<String, String> line = getMapListAsString(map, " ");
                String record = strLine.formatted(entry.getKey(), "[" + (printCPIndex ? line.first : line.second) + "]");
                if (printCPIndex) {
                    if (skipComments) {
                        println(enlargedIndent(record, getIndentStep() * 2));
                    } else {
                        print(PadRight(enlargedIndent(record, getIndentStep()), getCommentOffsetFor(27, 2)));
                        println(map.length == 0 ? "" : " //" + line.second);
                    }
                } else {
                    println(enlargedIndent(record, getIndentStep() * 2));
                }
            }
        }
    }

    private void stackMapTablePrinter(int index, int size) {
        String strFrameType = intLine.formatted(FRAMETYPE.parseKey(), stackEntryTypeValue);
        int padding = 0;

        if (isWrapped) {
            incIndent(wrapLevel);
        }

        if (skipComments && printCPIndex) {
            printIndentLn(strFrameType);
        } else {
            padding = doubleIndent + ((printCPIndex) ?
                    getCommentOffsetFor(strFrameType, 2) :
                    strFrameType.length() + padding * getIndentStep());
            if (printCPIndex) {
                printIndent(PadRight(strFrameType, padding - getIndentStep() * (wrapLevel + 2)));
                println(skipComments ? "" : " // " + stackEntryType.printName());
            } else {
                printIndent(PadRight(strFrameType, padding - getIndentStep() * wrapLevel));
                println(skipComments ? "" : " // " + stackEntryType.printName());
            }
        }
        if (range.in(stackEntryTypeValue)) {
            println(enlargedIndent(intLine.formatted(OFFSETDELTA.parseKey(), offset), doubleIndent));
        }

        if (stackEntryType == EARLY_LARVAL) {
            if (unsetFields != null) {
                final int limit = unsetFields.length - 1;
                String delim = limit >= 0 ? "; " : "";
                String prefix = "%-12s".formatted(UNSETFIELDS.parseKey());
                Pair<List<String>, List<String>> line = getFieldListAsString(unsetFields);
                String left = line.first.stream().collect(Collectors.joining(", ")).concat(delim);
                String right = line.second.stream().collect(Collectors.joining(", ")).concat(delim);
                String record = strLine.formatted(prefix, "[ " + (printCPIndex ? left : right) + "] {");
                if (printCPIndex) {
                    if (skipComments) {
                        println(enlargedIndent(record, doubleIndent));
                    } else {
                        if (limit <= 0) {
                            print(PadRight(enlargedIndent(record, doubleIndent), padding));
                            println(unsetFields.length == 0 ? "" : " // " + right);
                        } else {
                            String str = PadRight(enlargedIndent(strLine.formatted(
                                    prefix, "[ " + line.first.get(0).concat(",")), doubleIndent), padding);
                            int offs = str.indexOf('[') - INDENT_STEP * (wrapLevel + 1);
                            print(str).println(" // " + line.second.get(0));
                            for (int i = 1; i <= limit; i++) {
                                delim = i == limit ? "; ]  {" : ",";
                                String id = line.first.get(i);
                                String field = line.second.get(i);
                                print(PadRight(enlargedIndent(id.concat(delim), offs), padding)).
                                        println(" // " + field);
                            }
                        }
                    }
                } else {
                    if (limit <= 0) {
                        println(enlargedIndent(record, doubleIndent));
                    } else {
                        // first element
                        println(enlargedIndent(strLine.formatted(
                                prefix, "[ " + line.second.get(0).concat(",")), doubleIndent));
                        for (int i = 1; i <= limit; i++) {
                            delim = i == limit ? "; ] {" : ",";
                            String field = line.second.get(i);
                            println(enlargedIndent(" %s%s".formatted(field, delim), padding - 2));
                        }
                    }
                }
            }
        } else {
            for (Map.Entry<String, Pair<Boolean, int[]>> entry : Map.of(
                    STACKMAP.parseKey(), new Pair<>(stackMap != null, stackMap),
                    LOCALSMAP.parseKey(), new Pair<>(lockMap != null, lockMap)).entrySet()) {
                if (entry.getValue().first) {
                    // map found
                    int[] map = entry.getValue().second;
                    Pair<String, String> line = getMapListAsString(map, " ");
                    String record = "[" + (printCPIndex ? line.first : line.second) + "]";
                    if (printCPIndex) {
                        if (skipComments) {
                            println(enlargedIndent(strLine.formatted(entry.getKey(), record), doubleIndent));
                        } else {
                            print(PadRight(enlargedIndent(strLine.formatted(entry.getKey(), record), doubleIndent), padding)).
                                    println(" //" + line.second);
                        }
                    } else {
                        println(enlargedIndent(strLine.formatted(entry.getKey(), record), doubleIndent));
                    }
                }
            }
            if (isWrapped) {
                for (int i = 0; i < wrapLevel; i++) {
                    println(enlargedIndent(" }", (printCPIndex) ? tripleIndent : doubleIndent));
                    decIndent();
                }
            }
        }
    }

    @Override
    protected void tablePrint(int index, int size) throws IOException {
        //There are no differences between the simple (jasm) and extended (table) presentations of StackMapTable attribute.
        this.jasmPrint(index, size);
    }

    @Override
    public boolean isPrintable() {
        return tableFormat;
    }

    /**
     * Constructor for ATT_StackMap (Java 5,6)
     *
     * @param code the code attribute where this attribute is located
     * @param in   the input stream
     * @throws IOException the exception if something went wrong
     */
    public StackMapData(CodeData code, DataInputStream in) throws IOException {
        super(code);
        this.attribute = ATT_StackMap;

        super.memberType = ATT_StackMap.parseKey();
        super.tableToken = STACK_MAP;
        this.printer = this::stackMapPrinter;
        this.stackEntryType = FULL_FRAME;
        this.stackEntryTypeValue = FULL_FRAME.fromTag();
        this.frame_pc = in.readUnsignedShort();
        this.lockMap = readMap(in);
        this.stackMap = readMap(in);
        environment.traceln(() -> " stack_map_entry:pc=%d numloc=%s  numstack=%s".formatted(frame_pc,
                mapToHexString(lockMap), mapToHexString(stackMap)));
    }

    /**
     * Constructor for ATT_StackMapTable (Java 6, 7+)
     *
     * @param firstStackMap is it an entries[0] in the stack_map_entry structure? i.e. Does the StackMapData describe
     *                      the second stack map frame of the method?
     * @param prevFrame_pc  the bytecode offset of the previous entry (entries[current_index-1])
     * @param code          the code attribute where this attribute is located
     * @param in            the input stream
     * @throws IOException the exception if something went wrong
     */
    public StackMapData(boolean firstStackMap, int prevFrame_pc, CodeData code, DataInputStream in) throws IOException {
        super(code);
        this.attribute = ATT_StackMapTable;

        super.memberType = ATT_StackMapTable.parseKey();
        super.tableToken = STACK_MAP_TABLE;
        stackEntryTypeValue = in.readUnsignedByte();
        stackEntryType = StackMap.stackMapEntryType(stackEntryTypeValue);
        printer = this::stackMapTablePrinter;
        switch (stackEntryType) {
            case EARLY_LARVAL -> {
                // Valhalla: The entry type is early_larval_frame (246)
                offset = UNDEFINED;
                this.unsetFields = readFields(in);
                environment.traceln(() -> " early_larval_frame=%d".formatted(stackEntryTypeValue));
            }
            case SAME_FRAME -> {
                // The entry type is same_frame;
                offset = stackEntryTypeValue;
                environment.traceln(() -> " same_frame=%d".formatted(stackEntryTypeValue));
            }
            case SAME_FRAME_EXTENDED -> {
                // The entry type is same_frame_extended;
                offset = in.readUnsignedShort();
                environment.traceln(() -> " same_frame_extended=%d, offset=%d".formatted(stackEntryTypeValue, offset));
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                // The entry type is same_locals_1_stack_item_frame
                offset = stackEntryTypeValue - 64;
                stackMap = readMapElements(in, 1);
                environment.traceln(() -> " same_locals_1_stack_item_frame=%d, offset=%d, numstack=%s".formatted(
                        stackEntryTypeValue, offset, mapToHexString(stackMap)));
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED -> {
                // The entry type is same_locals_1_stack_item_frame_extended
                offset = in.readUnsignedShort();
                stackMap = readMapElements(in, 1);
                environment.traceln(() -> " same_locals_1_stack_item_frame_extended=%d, offset=%d, numstack=%s".formatted(
                        stackEntryTypeValue, offset, mapToHexString(stackMap)));
            }
            case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME -> {
                // The entry type is chop_frame
                offset = in.readUnsignedShort();
                environment.traceln(() -> " chop_frame=%d offset=%d".formatted(stackEntryTypeValue, offset));
            }
            case APPEND_FRAME -> {
                // The entry type is append_frame
                offset = in.readUnsignedShort();
                lockMap = readMapElements(in, stackEntryTypeValue - 251);
                environment.traceln(() -> " append_frame=%d offset=%d numlock=%s".formatted(
                        stackEntryTypeValue, offset, mapToHexString(lockMap)));
            }
            case FULL_FRAME -> {
                // The entry type is full_frame
                offset = in.readUnsignedShort();
                lockMap = readMap(in);
                stackMap = readMap(in);
                environment.traceln(() -> " full_frame=%d offset=%d numloc=%s  numstack=%s".formatted(
                        stackEntryTypeValue, offset, mapToHexString(lockMap), mapToHexString(stackMap)));
            }
            default -> environment.traceln(() -> "incorrect entry_type argument");
        }
        if (prevFrame_pc <= 0 && firstStackMap) {
            frame_pc = offset;
        } else {
            frame_pc = prevFrame_pc + offset + 1;
        }
    }

    /**
     * @return the bytecode offset at which a stack map frame applies
     */
    public int getFramePC() {
        return frame_pc;
    }

    public StackMap.EntryType getStackEntryType() {
        return stackEntryType;
    }

    public Pair<List<String>, List<String>> getFieldListAsString(int[] fields) {
        int count = fields.length;
        ArrayList<String> left = new ArrayList<>(count);
        ArrayList<String> right = new ArrayList<>(count);
        for (int index : fields) {
            if (data.printCPIndex) {
                left.add("#%d".formatted(index));
            }
            right.add(data.pool.getFieldNameTypeAsString(index));
        }
        return new Pair<>(left, right);
    }

    public Pair<String, String> getMapListAsString(int[] map, String delimiter) {
        if (map == null || map.length == 0) {
            return new Pair<>("", "");
        }
        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        int count = map.length - 1;
        for (int k = 0; k <= count; k++) {
            if (k == 0) {
                left.append(delimiter);
                right.append(delimiter);
            }
            int fullMapType = map[k];
            int mtVal = fullMapType & 0xFF;
            StackMap.VerificationType mapVerificationType = StackMap.getVerificationType(mtVal, Optional.of((s, a) -> environment.error(s, a)));
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
                        left.append(" ").append(getLabelPrefix()).append(argument);
                    }
                    right.append(prefix).append(mapVerificationType.printName());
                    right.append(" ").append(getLabelPrefix()).append(argument);
                }
                default -> {
                    if (data.printCPIndex) {
                        left.append(prefix).append(mtVal);
                    }
                    right.append(prefix).append(mapVerificationType.printName());
                }
            }
            if (data.printCPIndex) {
                left.append(k == count ? ';' : ',');
            }
            right.append((k == count ? ';' : ','));
            if (k == count) {
                left.append(delimiter);
                right.append(delimiter);
            }
        }
        return new Pair<>(left.toString(), right.toString());
    }

    /**
     * @return true if the entity presents a StackMapTable since Java 6
     */
    public boolean belongsToStackMapTable() {
        return attribute == ATT_StackMapTable;
    }

    /**
     * Sets helper printing particles
     */
    public StackMapData setPrintParticles(String intLine, String strLine, int shift) {
        this.intLine = intLine;
        this.strLine = strLine;
        this.shift = shift;
        return this;
    }

    /**
     * Sets table header
     */
    public StackMapData setHeader(String header) {
        this.tableHeader = header;
        return this;
    }

    private int[] readMap(DataInputStream in) throws IOException {
        int num = in.readUnsignedShort();
        return readMapElements(in, num);
    }

    private int[] readFields(DataInputStream in) throws IOException {
        int num = in.readUnsignedShort();
        int[] fields = new int[num];
        for (int i = 0; i < num; i++) {
            fields[i] = in.readUnsignedShort();
        }
        return fields;
    }

    private int[] readMapElements(DataInputStream in, int num) throws IOException {
        int[] map = new int[num];
        for (int k = 0; k < num; k++) {
            int mt_val;
            mt_val = in.readUnsignedByte();
            StackMap.VerificationType stackMapVerificationType = StackMap.getVerificationType(mt_val, Optional.of((s, a) -> environment.error(s, a)));
            switch (stackMapVerificationType) {
                case ITEM_Object -> mt_val = mt_val | (in.readUnsignedShort() << 8);
                case ITEM_NewObject -> {
                    int pc = in.readUnsignedShort();
                    data.getInstructionAttribute(pc).referred = true;
                    mt_val = mt_val | (pc << 8);
                }
            }
            map[k] = mt_val;
        }
        return map;
    }

    private int getCommentOffsetFor(String line, int shiftCount) {
        return owner.getPrintAttributeCommentPadding() + shift + line.length() - shiftCount * getIndentStep() - 2;
    }

    private int getCommentOffsetFor(int lineLength, int shiftCount) {
        return owner.getPrintAttributeCommentPadding() + shift + lineLength - shiftCount * getIndentStep() - 2;
    }
}
