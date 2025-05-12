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

import org.openjdk.asmtools.asmutils.HexUtils;
import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.StackMap;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static org.openjdk.asmtools.asmutils.HexUtils.toHex;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_RuntimeInvisibleTypeAnnotations;
import static org.openjdk.asmtools.common.structure.EAttribute.get;
import static org.openjdk.asmtools.jasm.ClassFileConst.BasicType;
import static org.openjdk.asmtools.jasm.ClassFileConst.getBasicType;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.LOCAL;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.STACK;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode.opc_bytecode;
import static org.openjdk.asmtools.jasm.OpcodeTables.opcode;

/**
 * Code data for a code attribute in method members in a class of the Java Disassembler
 */
public class CodeData extends MemberData<MethodData> {

    /**
     * reversed bytecode index hash, associates labels with ByteCode indexes
     */
    private final HashMap<Integer, InstructionAttr> instructionAttrs = new HashMap<>();
    // Raw byte array for the byte codes
    protected byte[] code;
    // Limit for the stack size
    protected int max_stack;
    // Limit for the number of local vars
    protected int max_locals;
    // The remaining attributes of this class
    protected ArrayList<AttrData> attrs = new ArrayList<>(0);                       // AttrData
    // Trap table, describes exceptions caught
    private ArrayList<TrapData> trap_table = new ArrayList<>(0);                    // TrapData

    // Line Number table, describes source lines associated with ByteCode indexes
    private Container<LineNumberData, CodeData> lineNumberTable;
    // Local Variable table, describes variable scopes associated with ByteCode indexes
    private Container<LocalVariableData, CodeData> localVariableTable;

    // Local Variable Type table, describes variable types associated with variables
    private Container<LocalVariableTypeData, CodeData> localVariableTypeTable;

    // stack map table, describes compiler hints for stack rep, associated with  ByteCode indexes
    private StackMapTable stackMapTable;
    // The boolean firstStackEntry is calculated based on the index of the StackMap entry in the table.
    // Is firstStackEntry the entries[0] in the stack_map_entry structure?
    // In other words, does StackMapData[i] describe the second stack map entry of the method ignoring leading modifiers?
    private boolean firstStackEntry = true;

    //The visible type annotations for this method
    private ArrayList<TypeAnnotationData<MethodData>> visibleTypeAnnotations;

    // The invisible type annotations for this method
    private ArrayList<TypeAnnotationData<MethodData>> invisibleTypeAnnotations;

    private int instructionOffset, attributeOffset;

    public CodeData(MethodData data) {
        super(data);
        if (data.printProgramCounter) {
            instructionOffset = PROGRAM_COUNTER_PLACEHOLDER_LENGTH;
            attributeOffset = instructionOffset;
        } else {
            instructionOffset = INSTR_PREFIX_LENGTH;
            attributeOffset = instructionOffset - getIndentStep();
        }
    }

    private static int align(int n) {
        return (n + 3) & ~3;
    }

    private int getByte(int pc) {
        return code[pc];
    }

    private int getUByte(int pc) {
        return code[pc] & 0xFF;
    }

    private int getShort(int pc) {
        return (code[pc] << 8) | (code[pc + 1] & 0xFF);
    }

    private int getUShort(int pc) {
        return ((code[pc] << 8) | (code[pc + 1] & 0xFF)) & 0xFFFF;
    }

    private int getInt(int pc) {
        return (getShort(pc) << 16) | (getShort(pc + 2) & 0xFFFF);
    }

    protected InstructionAttr getInstructionAttribute(int pc) {
        Integer PC = pc;
        InstructionAttr res = instructionAttrs.get(PC);
        if (res == null) {
            res = new InstructionAttr(this.data);
            res.setTheSame(this).incIndent();
            instructionAttrs.put(PC, res);
        }
        return res;
    }

    protected InstructionAttr getLastInstruction() {
        return instructionAttrs.get(Collections.max(instructionAttrs.keySet()));
    }

    protected InstructionAttr getFirstInstruction() {
        return instructionAttrs.get(Collections.min(instructionAttrs.keySet()));
    }

    /* Read Methods */
    private Container<LineNumberData, CodeData> readLineNumberTable(DataInputStream in, boolean ignoreMemorization) throws IOException {
        int len = in.readInt(); // attr_length
        int nLines = in.readUnsignedShort();
        Container<LineNumberData, CodeData> table = ignoreMemorization ? null : new Container<>(this, LineNumberData.class, nLines);
        environment.traceln("CodeAttr:  LineNumberTable[%d] length=%d", nLines, len);
        for (int l = 0; l < nLines; l++) {
            LineNumberData data = new LineNumberData(in, this.data);
            if (!ignoreMemorization) {
                table.add(data);
            }
        }
        return table;
    }

    private Container<LocalVariableData, CodeData> readLocalVariableTable(DataInputStream in, boolean ignoreMemorization) throws IOException {
        int len = in.readInt(); // attr_length
        int nLines = in.readUnsignedShort();
        Container<LocalVariableData, CodeData> table = ignoreMemorization ? null : new Container<>(this, LocalVariableData.class, nLines);
        environment.traceln("CodeAttr:  LocalVariableTable[%d] length=%d", nLines, len);
        for (int l = 0; l < nLines; l++) {
            LocalVariableData data = new LocalVariableData(owner, in, this.data);
            if (!ignoreMemorization) {
                table.add(data);
            }
        }
        return table;
    }

    private Container<LocalVariableTypeData, CodeData> readLocalVariableTypeTable(DataInputStream in, boolean ignoreMemorization) throws IOException {
        int len = in.readInt(); // attr_length
        int nLines = in.readUnsignedShort();
        Container<LocalVariableTypeData, CodeData> table = ignoreMemorization ? null : new Container<>(this, LocalVariableTypeData.class, nLines);
        environment.traceln("CodeAttr:  LocalVariableTypeTable[%d] length=%d", nLines, len);
        for (int l = 0; l < nLines; l++) {
            LocalVariableTypeData data = new LocalVariableTypeData(owner, in, this.data);
            if (!ignoreMemorization) {
                table.add(data);
            }
        }
        return table;
    }

    private void readTrapTable(DataInputStream in) throws IOException {
        int trap_table_len = in.readUnsignedShort();
        environment.traceln("CodeAttr:  TrapTable[%d]", trap_table_len);
        trap_table = new ArrayList<>(trap_table_len);
        for (int l = 0; l < trap_table_len; l++) {
            trap_table.add(new TrapData(in, l));
        }
    }

    private void readStackMapEntity(EAttribute attribute, DataInputStream in) throws IOException {
        int len = in.readInt(); // attr_length
        int stackMapLength = in.readUnsignedShort();
        stackMapTable = new StackMapTable(attribute, this, stackMapLength);
        firstStackEntry = true;
        environment.traceln(() -> "CodeAttr:  %s: attrLength=%d num=%d".formatted(attribute.name(), len, stackMapLength));
        int prevFrame_pc = 0;
        int idx = 0;
        boolean nextIsWrapped = false;
        int wrapLevel = 0;
        while (idx < stackMapLength) {
            StackMapData stackMapData = switch (attribute) {
                case ATT_StackMap -> new StackMapData(this, in);
                case ATT_StackMapTable -> new StackMapData(calculateFirstPosition(idx), prevFrame_pc, this, in);
                default -> throw new IllegalStateException("Unexpected value: " + attribute);
            };
            prevFrame_pc = stackMapData.getFramePC();
            if (stackMapData.getStackEntryType() == StackMap.EntryType.EARLY_LARVAL) {
                stackMapData.isWrapped = nextIsWrapped; // Negative test case:EARLY_LARVAL belongs to EARLY_LARVAL
                stackMapData.wrapLevel = wrapLevel;
                stackMapTable.add(stackMapData, true);
                nextIsWrapped = true;
                wrapLevel++;
            } else {
                stackMapData.isWrapped = nextIsWrapped;
                stackMapData.wrapLevel = wrapLevel;
                stackMapTable.add(stackMapData, false);
                nextIsWrapped = false;
                wrapLevel = 0;
                idx++;
            }
        }
    }

    /**
     * Calculates whether a StackMapTable[index] entry is the first in the table, ignoring wrappers
     *
     * @param index index of the entry in the StackMapTable
     * @return true if index refers to the first entry that isn't wrapper.
     */
    private boolean calculateFirstPosition(int index) {
        if (firstStackEntry) {
            if (index == 0 || stackMapTable.wrappers.get(index - 1)) {
                return true;
            }
            firstStackEntry = false;
        }
        return false;
    }


    private void readTypeAnnotations(DataInputStream in, boolean isInvisible) throws IOException {
        int attrLength = in.readInt();
        // Read Type Annotations Attr
        int count = in.readShort();
        ArrayList<TypeAnnotationData<MethodData>> tannots = new ArrayList<>(count);
        environment.traceln("CodeAttr:   Runtime%sisibleTypeAnnotation: attrLength=%d num= %d",
                (isInvisible ? "Inv" : "V"), attrLength, count);
        for (int index = 0; index < count; index++) {
            TypeAnnotationData<MethodData> tannot = new TypeAnnotationData<>(this.data, isInvisible);
            tannot.read(in);
            tannots.add(tannot);
        }
        if (isInvisible) {
            invisibleTypeAnnotations = tannots;
        } else {
            visibleTypeAnnotations = tannots;
        }
    }

    /**
     * Read and resolve the code attribute data called from MethodData. precondition:
     * NumFields has already been read from the stream.
     */
    public void read(DataInputStream in, int codeAttrLength) throws IOException {
        // Read the code in the Code Attribute
        max_stack = in.readUnsignedShort();
        max_locals = in.readUnsignedShort();
        int codelen = in.readInt();
        environment.traceln("CodeAttr:  CodeLength=%d FullLength=%d max_stack=%d max_locals=%d",
                codelen, codeAttrLength, max_stack, max_locals);

        // read the raw code bytes
        code = new byte[codelen];
        in.read(code, 0, codelen);

        //read the trap table
        readTrapTable(in);

        // Read any attributes of the Code Attribute
        int nattr = in.readUnsignedShort();
        environment.traceln("CodeAttr: add.attr: %d", nattr);
        for (int k = 0; k < nattr; k++) {
            int name_cpx = in.readUnsignedShort();
            // verify the Attrs name
            ConstantPool.Constant<?> name_const = pool.getConst(name_cpx);
            if (name_const != null && name_const.tag == ConstantPool.TAG.CONSTANT_UTF8) {
                String attrName = pool.getString(name_cpx, index -> "#" + index);
                environment.traceln("CodeAttr:  attr: " + attrName);
                // process the attr
                EAttribute attrTag = get(attrName);
                switch (attrTag) {
                    case ATT_LineNumberTable -> lineNumberTable = readLineNumberTable(in, !printLineNumber);
                    case ATT_LocalVariableTable ->
                            localVariableTable = readLocalVariableTable(in, !printLocalVariables);
                    case ATT_LocalVariableTypeTable ->
                            localVariableTypeTable = readLocalVariableTypeTable(in, !printLocalVariableTypes);
                    case ATT_StackMap, ATT_StackMapTable -> readStackMapEntity(attrTag, in);
                    case ATT_RuntimeVisibleTypeAnnotations, ATT_RuntimeInvisibleTypeAnnotations ->
                            readTypeAnnotations(in, attrTag == ATT_RuntimeInvisibleTypeAnnotations);
                    default -> {
                        AttrData attr = new AttrData(environment, attrTag);
                        int attrLen = in.readInt(); // attr_length
                        attr.read(name_cpx, attrLen, in);
                        attrs.add(attr);
                    }
                }
            }
        }
    }

    /* Code Resolution Methods */
    private int checkForLabelRef(int pc) {
        int opc = getUByte(pc);
        Opcode opcode = opcode(opc);
        try {
            switch (opcode) {
                case opc_tableswitch -> {
                    int tb = align(pc + 1);
                    int default_skip = getInt(tb); /* default skip pamount */

                    int low = getInt(tb + 4);
                    int high = getInt(tb + 8);
                    int count = high - low;
                    for (int i = 0; i <= count; i++) {
                        getInstructionAttribute(pc + getInt(tb + 12 + 4 * i)).referred = true;
                    }
                    getInstructionAttribute(default_skip + pc).referred = true;
                    return tb - pc + 16 + count * 4;
                }
                case opc_lookupswitch -> {
                    int tb = align(pc + 1);
                    int default_skip = getInt(tb); /* default skip pamount */

                    int npairs = getInt(tb + 4);
                    for (int i = 1; i <= npairs; i++) {
                        getInstructionAttribute(pc + getInt(tb + 4 + i * 8)).referred = true;
                    }
                    getInstructionAttribute(default_skip + pc).referred = true;
                    return tb - pc + (npairs + 1) * 8;
                }
                case opc_jsr, opc_goto, opc_ifeq, opc_ifge, opc_ifgt, opc_ifle, opc_iflt, opc_ifne, opc_if_icmpeq,
                     opc_if_icmpne, opc_if_icmpge, opc_if_icmpgt, opc_if_icmple, opc_if_icmplt, opc_if_acmpeq,
                     opc_if_acmpne, opc_ifnull, opc_ifnonnull -> {
                    getInstructionAttribute(pc + getShort(pc + 1)).referred = true;
                    return 3;
                }
                case opc_jsr_w, opc_goto_w -> {
                    getInstructionAttribute(pc + getInt(pc + 1)).referred = true;
                    return 5;
                }
                case opc_wide, opc_nonpriv, opc_priv -> {
                    int opc2 = (opcode.value() << 8) + getUByte(pc + 1);
                    opcode = opcode(opc2);
                }
            }
            int opclen = opcode.length();
            return opclen == 0 ? 1 : opclen;  // bugfix for 4614404
        } catch (ArrayIndexOutOfBoundsException e) {
            return 1;
        }
    } // end checkForLabelRef

    private void loadLabelTable() {
        for (int pc = 0; pc < code.length; ) {
            pc = pc + checkForLabelRef(pc);
        }
    }

    private void loadLineNumTable() {
        for (LineNumberData entry : lineNumberTable) {
            getInstructionAttribute(entry.start_pc).lineNum = entry.line_number;
        }
    }

    private void loadStackMap() {
        ArrayList<StackMapData> wrappers = null;
        boolean firstWrapper = true;
        for (int i = 0; i < stackMapTable.size(); i++) {
            StackMapData entry = stackMapTable.get(i);
            if (stackMapTable.wrappers.get(i)) {
                if (wrappers == null) {
                    wrappers = new ArrayList<>();
                }
                wrappers.add(entry);
            } else {
                firstWrapper = false;
                InstructionAttr instr = getInstructionAttribute(entry.frame_pc);
                instr.stackMapEntry = entry;
                instr.stackMapWrappers = wrappers;
                wrappers = null;
            }
        }
        if (wrappers != null) {
            // get either first or last instruction and assign modifiers to it.
            InstructionAttr instr = (firstWrapper) ? getFirstInstruction() : getLastInstruction();
            instr.stackMapWrappers = wrappers;
        }
    }

    private void loadLocalVariableTable() {
        for (LocalVariableData entry : localVariableTable) {
            getInstructionAttribute(entry.start_pc).addVar(entry);
            getInstructionAttribute(entry.start_pc + entry.length).addEndVar(entry);
        }
    }

    private void loadLocalVariableTypeTable() {
        for (LocalVariableTypeData entry : localVariableTypeTable) {
            getInstructionAttribute(entry.start_pc).addType(entry);
            getInstructionAttribute(entry.start_pc + entry.length).addEndType(entry);
        }
    }

    private void loadTrapTable() {
        for (TrapData entry : trap_table) {
            getInstructionAttribute(entry.start_pc).addTrap(entry);
            getInstructionAttribute(entry.end_pc).addEndTrap(entry);
            getInstructionAttribute(entry.handler_pc).add_handler(entry);
        }
    }

    /* Print Methods */
    private int printInstrLn(int pc, int shift) {
        int opc = getUByte(pc), opc2;
        Opcode opcode = opcode(opc), opcode2;
        String mnem;
        switch (opcode) {
            case opc_nonpriv, opc_priv -> {
                int count = 1;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count) {
                    opc2 = getUByte(pc + 1);
                    int finalopc = (opc << 8) + opc2;
                    opcode2 = opcode(finalopc);
                    if (opcode2 == null) {
                        // assume all (even nonexistent) priv and nonpriv instructions
                        // are 2 bytes long
                        mnem = opcode.parseKey() + " " + opc2;
                    } else {
                        mnem = opcode2.parseKey();
                    }
                    println(mnem);
                } else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 2;
            }
            case opc_wide -> {
                int count = 1;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() != count) {
                    printBytes(opcode.byteValue(), validBytes, shift);
                    return 1;
                }
                opc2 = getUByte(pc + 1);
                int finalopcwide = (opc << 8) + opc2;
                opcode2 = opcode(finalopcwide);
                if (opcode2 == null) {
                    // nonexistent opcode - but we have to print something
                    print(PadRight(opc_bytecode.parseKey(), OPERAND_PLACEHOLDER_LENGTH + 1)).println(opcode + ";");
                    return 1;
                } else {
                    mnem = opcode2.parseKey();
                }
                if (opcode2 == Opcode.opc_iinc_w) {
                    count = 5;
                    validBytes = checkCodeBounds(pc, 5);
                    if (validBytes.size() == count) {
                        print(PadRight(mnem, OPERAND_PLACEHOLDER_LENGTH + 1));
                        println("%d, %d;", getUShort(pc + 2), getUShort(pc + 4));
                    } else
                        printBytes(opcode.byteValue(), validBytes, shift);
                    return 6;
                } else {
                    count = 3;
                    validBytes = checkCodeBounds(pc, 3);
                    if (validBytes.size() == count) {
                        print(PadRight(mnem, OPERAND_PLACEHOLDER_LENGTH + 1)).println("%d;", getUShort(pc + 2));
                    } else
                        printBytes(opcode.byteValue(), validBytes, shift);
                    return 4;
                }
            }
        }
        mnem = opcode.parseKey();
        if (mnem == null) {
            // nonexistent opcode - but we have to print something
            print(PadRight("bytecode", OPERAND_PLACEHOLDER_LENGTH + 1)).println(opcode + ";");
            return 1;
        }
        if (!opcode.isReservedOpcode()) { // == opcode.value() >= Opcode.opc_bytecode.value();
            // pseudo opcodes should be printed as bytecodes
            print(PadRight("bytecode", OPERAND_PLACEHOLDER_LENGTH + 1)).println(opcode + ";");
            return 1;
        }
        String operand = opcode.parseKey();
        switch (opcode) {
            case opc_aload, opc_astore, opc_fload, opc_fstore, opc_iload, opc_istore, opc_lload, opc_lstore, opc_dload,
                 opc_dstore, opc_ret -> {
                int count = 1;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).println(getUByte(pc + 1) + ";");
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 2;
            }
            case opc_iinc -> {
                int count = 2;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                            println("%d, %d;", getUByte(pc + 1), getByte(pc + 2));
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 3;
            }
            case opc_tableswitch -> {
                // TODO:  add checkBounds
                int tb = align(pc + 1);
                int default_skip = getInt(tb); /* default skip pamount */

                int low = getInt(tb + 4);
                int high = getInt(tb + 8);
                int count = high - low;

                printPadRight(format("%s { ", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH)), getCommentOffset());
                println(printCPIndex && !skipComments ? " // " + low + " to " + high : "");
                for (int i = 0; i <= count; i++) {
                    // 9 == "default: ".length()
                    print(enlargedIndent(PadRight(format("%2d: ", i + low), 9), shift)).
                            println(data.getLabelPrefix() + (pc + getInt(tb + 12 + 4 * i)) + ";");
                }
                print(enlargedIndent(
                        PadRight("default: " + data.getLabelPrefix() + (default_skip + pc),
                                OPERAND_PLACEHOLDER_LENGTH - getIndentStep() - 2), shift)
                ).println(" };");
                return tb - pc + 16 + count * 4;
            }
            case opc_lookupswitch -> {
                // TODO:  add checkBounds
                int tb = align(pc + 1);

                int default_skip = getInt(tb);
                int nPairs = getInt(tb + 4);

                printPadRight(format("%s { ", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH)), getCommentOffset());
                println(printCPIndex && !skipComments ? " // " + nPairs : "");
                Pair<Integer, Integer>[] lookupswitchPairs = getLookupswitchPairs(tb, nPairs);
                // 9 == "default: ".length()
                int caseLength = Math.max(9, Arrays.stream(lookupswitchPairs).
                        mapToInt(p -> String.valueOf(p.first).length()).max().orElse(0) + 2);
                for (int i = 0; i < nPairs; i++) {
                    print(enlargedIndent(PadRight(format("%2d:", lookupswitchPairs[i].first), caseLength), shift)).
                            println(data.getLabelPrefix() + (pc + lookupswitchPairs[i].second) + ";");
                }
                print(enlargedIndent(
                        PadRight(PadRight("default: ", caseLength) + data.getLabelPrefix() + (default_skip + pc),
                                OPERAND_PLACEHOLDER_LENGTH - getIndentStep() - 2), shift)).println(" };");
                return tb - pc + (nPairs + 1) * 8;
            }
            case opc_newarray -> {
                int count = 1;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count) {
                    int tp = getUByte(pc + 1);
                    BasicType basicType = getBasicType(tp);
                    if (basicType == null) {
                        print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                                println("BOGUS TYPE: " + toHex(tp, 8) + ";");
                    } else {
                        print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                                println(basicType.printValue() + ";");
                    }
                } else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 2;
            }
            case opc_ldc, opc_ldc_w, opc_ldc2_w, opc_invokedynamic -> {
                // TODO:  add checkBounds
                // added printing of the tag: Method/Interface to clarify
                // interpreting CONSTANT_MethodHandle_info:reference_kind
                // Example: ldc_w Dynamic REF_invokeStatic:Method CondyIndy.condy_bsm
                int index, opLength;
                Map<Integer, List<Integer>> breakPositions = new HashMap<>();
                if (opcode == Opcode.opc_ldc) {
                    opLength = 2;
                    breakPositions = LdwBreakPositions;
                    index = getUByte(pc + 1);
                } else if (opcode == Opcode.opc_invokedynamic) {
                    opLength = 5;
                    breakPositions = InvokeDynamicBreakPositions;
                    index = getUShort(pc + 1); // getUbyte(pc + 3); // getUbyte(pc + 4); // reserved bytes
                } else {    // opc_ldc*_w
                    opLength = 3;
                    breakPositions = LdwBreakPositions;
                    index = getUShort(pc + 1);
                }
                pool.setPrintTAG(true);
                if (printCPIndex) {
                    if (skipComments) {
                        println(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index));
                    } else {
                        printPadRight(
                                format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index), getCommentOffset()).
                                print(" // ");
                        println(
                                formatOperandLine(pool.ConstantStrValue(index), getCommentOffset() + shift,
                                        " // ", breakPositions));
                    }
                } else {
                    // TODO: Check Offset calculation
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                    println(formatOperandLine(pool.ConstantStrValue(index), OPERAND_PLACEHOLDER_LENGTH + shift + 1,
                            "", breakPositions) + ";");
                }
                pool.setPrintTAG(false);
                return opLength;
            }
            case opc_anewarray, opc_instanceof, opc_checkcast, opc_new, opc_putstatic, opc_getstatic, opc_putfield,
                 opc_getfield, opc_invokevirtual, opc_invokespecial, opc_invokestatic -> {
                int count = 2;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count) {
                    int index = getUShort(pc + 1);
                    if (printCPIndex) {
                        if (skipComments) {
                            println(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index));
                        } else {
                            printPadRight(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index),
                                    getCommentOffset()).println(" // " + pool.ConstantStrValue(index));
                        }
                    } else {
                        print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                        println(pool.ConstantStrValue(index) + ";");
                    }
                } else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 3;
            }
            case opc_multianewarray, opc_invokeinterface -> {
                int count = opcode == Opcode.opc_multianewarray ? 3 : 4;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count) {
                    int index = getUShort(pc + 1);
                    int dimensions = getUByte(pc + 3);  // nargs in case of opc_invokeinterface
                    if (printCPIndex) {
                        if (skipComments) {
                            println(format("%s #%d, %d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index, dimensions));
                        } else {
                            printPadRight(format("%s #%d, %d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index, dimensions),
                                    getCommentOffset()).println(" // " + pool.ConstantStrValue(index));
                        }
                    } else {
                        print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                        println("%s, %d;", pool.ConstantStrValue(index), dimensions);
                    }
                } else {
                    printBytes(opcode.byteValue(), validBytes, shift);
                }
                return count + 1;
            }
            case opc_sipush -> {
                int count = 2;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).println(getShort(pc + 1) + ";");
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 3;
            }
            case opc_bipush -> {
                int count = 1;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).println(getByte(pc + 1) + ";");
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 2;
            }
            case opc_jsr, opc_goto, opc_ifeq, opc_ifge, opc_ifgt, opc_ifle, opc_iflt, opc_ifne, opc_if_icmpeq,
                 opc_if_icmpne, opc_if_icmpge, opc_if_icmpgt, opc_if_icmple, opc_if_icmplt, opc_if_acmpeq,
                 opc_if_acmpne, opc_ifnull, opc_ifnonnull -> {
                int count = 2;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                            println(data.getLabelPrefix() + (pc + getShort(pc + 1)) + ";");
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 3;
            }
            case opc_jsr_w, opc_goto_w -> {
                int count = 4;
                List<Byte> validBytes = checkCodeBounds(pc, count);
                if (validBytes.size() == count)
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                            println(data.getLabelPrefix() + (pc + getInt(pc + 1)) + ";");
                else
                    printBytes(opcode.byteValue(), validBytes, shift);
                return 5;
            }
            default -> {
                println(operand + ";");
                return 1;
            }
        }
    } // end printInstr

    /**
     * @param opcode byte encoding of an instruction
     * @param bytes  bytes corresponding to the instruction's parameters
     * @param shift  printing indentation for output bytes
     */
    private void printBytes(byte opcode, List<Byte> bytes, int shift) {
        print(PadRight(opc_bytecode.parseKey(), OPERAND_PLACEHOLDER_LENGTH + 1)).
                println(HexUtils.toHex(opcode) + ";");
        for (byte b : bytes) {
            printPadLeft(" ", shift).
                    print(PadRight(opc_bytecode.parseKey(), OPERAND_PLACEHOLDER_LENGTH + 1)).
                    println(HexUtils.toHex(b) + ";");
        }
    }

    /**
     * Checks whether the bytes corresponding to the instruction's parameters belong to the code attribute.
     *
     * @param ind   code index of the instruction
     * @param count number of bytes corresponding to the instruction's parameters
     * @return The list of bytes corresponds to the instruction's parameters that belong to the code attribute.
     * If the count does not match the list size, it indicates that the instruction is truncated.
     */
    private List<Byte> checkCodeBounds(int ind, int count) {
        List<Byte> list = new ArrayList<>();
        int codeLength = code.length;
        for (int i = 1; i <= count; i++) {
            if (ind + i < codeLength)
                list.add(code[i]);
            else
                return list;
        }
        return list;
    }

    private Pair<Integer, Integer>[] getLookupswitchPairs(int pad, int count) {
        Pair[] pairs = new Pair[count];
        for (int i = 1; i <= count; i++) {
            pairs[i - 1] = new Pair<>(getInt(pad + i * 8), getInt(pad + 4 + i * 8));
        }
        return pairs;
    }


    /**
     * Prints the code data to the current output stream if code exists (method isn't abstract)
     */
    public void print() throws IOException {

        if (lineNumberTable != null) {
            loadLineNumTable();
        }

        // Preparing the inlined StackMapTable to have early_larval_frame tied to other frame types for which it is a wrapper.
        if (stackMapTable != null && !tableFormat) {
            loadStackMap();
        }
        if (!data.printProgramCounter) {
            loadLabelTable();
        }
        loadTrapTable();
        if (localVariableTable != null) {
            loadLocalVariableTable();
        }
        if (localVariableTypeTable != null) {
            loadLocalVariableTypeTable();
        }
        // stack 3  locals 1
        incIndent().printIndentLn("%s %d  %s %d", STACK.parseKey(), max_stack, LOCAL.parseKey(), max_locals).decIndent();

        // Print ParamAnnotations if found.
        data.printMethodParameters();

        // Print Code Attribute
        printIndentLn("{");

        InstructionAttr insAttr = instructionAttrs.get(0);

        setCommentOffset(getCommentOffset() - instructionOffset - getIndentSize());

        for (int pc = 0; pc < code.length; ) {

            if (insAttr != null) {
                incIndent();
                insAttr.setCommentOffset(this.getCommentOffset());
                insAttr.printBegins(attributeOffset);
                decIndent();
            }

            if (data.printProgramCounter) {
                incIndent();
                printIndent(PadRight(format("%2d:", pc), instructionOffset));
            } else {
                printIndent(PadRight(((insAttr != null) && insAttr.referred) ? data.getLabelPrefix() + pc + ":" : " ", instructionOffset));
                incIndent();
            }

            if (insAttr != null) {
                if (insAttr.printStackMap_Table(attributeOffset)) {
                    print(enlargedIndent(attributeOffset));
                }
            }

            pc = pc + printInstrLn(pc, attributeOffset + getIndentSize());
            insAttr = instructionAttrs.get(pc);
            if (insAttr != null) {
                insAttr.printEnds(attributeOffset);
                decIndent();
            }
            decIndent();
        }

        // the right brace can be labelled:
        if (insAttr != null && insAttr.stackMapEntry != null && insAttr.referred) {
            if (data.printProgramCounter) {
                incIndent();
                printIndent(PadRight("%2d:".formatted(code.length), 0));
                decIndent();
            } else {
                printIndent(PadRight("%s%d:".formatted(data.getLabelPrefix(), code.length), code.length), 0);
            }
        }
        // Print code's type annotations
        printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);

        // Print
        printAttributes(lineNumberTable, localVariableTable, localVariableTypeTable, stackMapTable);
        printIndentLn("}");
    }

    @Override
    public MemberData<MethodData> setSignature(SignatureData signatureData) {
        throw new RuntimeException("The Code Attribute does not have a signature attribute");
    }

    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Prints ClassData annotations
     *
     * @throws IOException signals that an exception of some sort has occurred
     */
    @Override
    protected <T extends AnnotationData> void printAnnotations(List<T>... annotationLists) throws IOException {
        boolean firstTime = true;
        for (List<T> list : annotationLists) {
            if (list != null) {
                println(firstTime);
                for (T annotation : list) {
                    firstTime = false;
                    annotation.print();
                    println();
                }
            }
        }
    }
}
