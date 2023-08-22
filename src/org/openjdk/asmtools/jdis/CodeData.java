/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.openjdk.asmtools.asmutils.HexUtils.toHex;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_RuntimeInvisibleTypeAnnotations;
import static org.openjdk.asmtools.common.structure.EAttribute.get;
import static org.openjdk.asmtools.jasm.ClassFileConst.BasicType;
import static org.openjdk.asmtools.jasm.ClassFileConst.getBasicType;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode;
import static org.openjdk.asmtools.jasm.OpcodeTables.opcode;

/**
 * Code data for a code attribute in method members in a class of the Java Disassembler
 */
public class CodeData extends MemberData<MethodData> {

    /**
     * (parsed) reversed bytecode index hash, associates labels with ByteCode indexes
     */
    private final HashMap<Integer, InstructionAttr> instructionAttrs = new HashMap<>();
    // Raw byte array for the byte codes
    protected byte[] code;
    // Limit for the stack size
    protected int max_stack;
    // Limit for the number of local vars
    protected int max_locals;
    // The remaining attributes of this class
    protected ArrayList<AttrData> attrs = new ArrayList<>(0);        // AttrData

    // (parsed) Trap table, describes exceptions caught
    private ArrayList<TrapData> trap_table = new ArrayList<>(0);   // TrapData
    /**
     * (parsed) Line Number table, describes source lines associated with ByteCode indexes
     */
    private ArrayList<LineNumData> lin_num_tb = new ArrayList<>(0);   // LineNumData
    /**
     * (parsed) Local Variable table, describes variable scopes associated with ByteCode
     * indexes
     */
    private ArrayList<LocVarData> loc_var_tb = new ArrayList<>(0);   // LocVarData
    /**
     * (parsed) stack map table, describes compiler hints for stack rep, associated with
     * ByteCode indexes
     */
    private ArrayList<StackMapData> stack_map = null;
    /**
     * The visible type annotations for this method
     */
    private ArrayList<TypeAnnotationData<MethodData>> visibleTypeAnnotations;
    /**
     * The invisible type annotations for this method
     */
    private ArrayList<TypeAnnotationData<MethodData>> invisibleTypeAnnotations;

    public CodeData(MethodData data) {
        super(data);
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

    /* Read Methods */
    private void readLineNumTable(DataInputStream in) throws IOException {
        int len = in.readInt(); // attr_length
        int nLines = in.readUnsignedShort();
        lin_num_tb = new ArrayList<>(nLines);
        environment.traceln("CodeAttr:  LineNumTable[%d] length=%d", nLines, len);
        for (int l = 0; l < nLines; l++) {
            lin_num_tb.add(new LineNumData(in));
        }
    }

    private void readLocVarTable(DataInputStream in) throws IOException {
        int len = in.readInt(); // attr_length
        int nLines = in.readUnsignedShort();
        loc_var_tb = new ArrayList<>(nLines);
        environment.traceln("CodeAttr:  LocalVariableTable[%d] length=%d", nLines, len);
        for (int l = 0; l < nLines; l++) {
            loc_var_tb.add(new LocVarData(in));
        }
    }

    private void readTrapTable(DataInputStream in) throws IOException {
        int trap_table_len = in.readUnsignedShort();
        environment.traceln("CodeAttr:  TrapTable[%d]", trap_table_len);
        trap_table = new ArrayList<>(trap_table_len);
        for (int l = 0; l < trap_table_len; l++) {
            trap_table.add(new TrapData(in, l));
        }
    }

    private void readStackMapEntity(StackMapData.EAttributeType type, DataInputStream in) throws IOException {
        int len = in.readInt(); // attr_length
        int stack_map_len = in.readUnsignedShort();
        stack_map = new ArrayList<>(stack_map_len);
        environment.traceln("CodeAttr:  %s: attrLength=%d num=%d", type.getName(), len, stack_map_len);
        int prevFrame_pc = 0;
        for (int k = 0; k < stack_map_len; k++) {
            StackMapData stackMapData =  new StackMapData(type, k == 0, prevFrame_pc, this, in);
            prevFrame_pc = stackMapData.getFramePC();
            stack_map.add(stackMapData);
        }
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
            ConstantPool.Constant name_const = pool.getConst(name_cpx);
            if (name_const != null && name_const.tag == ConstantPool.TAG.CONSTANT_UTF8) {
                String attrName = pool.getString(name_cpx, index -> "#" + index);
                environment.traceln("CodeAttr:  attr: " + attrName);
                // process the attr
                EAttribute attrTag = get(attrName);
                switch (attrTag) {
                    case ATT_LineNumberTable -> readLineNumTable(in);
                    case ATT_LocalVariableTable -> readLocVarTable(in);
                    case ATT_StackMap -> readStackMapEntity(StackMapData.EAttributeType.STACKMAP, in);
                    case ATT_StackMapTable -> readStackMapEntity(StackMapData.EAttributeType.STACKMAPTABLE, in);
                    case ATT_RuntimeVisibleTypeAnnotations, ATT_RuntimeInvisibleTypeAnnotations ->
                            readTypeAnnotations(in, attrTag == ATT_RuntimeInvisibleTypeAnnotations);
                    default -> {
                        AttrData attr = new AttrData(environment);
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
        try {
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
        for (LineNumData entry : lin_num_tb) {
            getInstructionAttribute(entry.start_pc).lineNum = entry.line_number;
        }
    }

    private void loadStackMap() {
        for (StackMapData entry : stack_map) {
            getInstructionAttribute(entry.frame_pc).stackMapEntry = entry;
        }
    }

    private void loadLocVarTable() {
        for (LocVarData entry : loc_var_tb) {
            getInstructionAttribute(entry.start_pc).addVar(entry);
            getInstructionAttribute(entry.start_pc + entry.length).addEndVar(entry);
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
        int opc = getUByte(pc);
        int opc2;
        Opcode opcode = opcode(opc);
        Opcode opcode2;
        String mnem;
        switch (opcode) {
            case opc_nonpriv, opc_priv -> {
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
                return 2;
            }
            case opc_wide -> {
                opc2 = getUByte(pc + 1);
                int finalopcwide = (opc << 8) + opc2;
                opcode2 = opcode(finalopcwide);
                if (opcode2 == null) {
                    // nonexistent opcode - but we have to print something
                    print(PadRight("bytecode", OPERAND_PLACEHOLDER_LENGTH + 1)).println(opcode + ";");
                    return 1;
                } else {
                    mnem = opcode2.parseKey();
                }
                if (opcode2 == Opcode.opc_iinc_w) {
                    print(PadRight(mnem, OPERAND_PLACEHOLDER_LENGTH + 1));
                    println("%d, %d;", getUShort(pc + 2), getUShort(pc + 4));
                    return 6;
                } else {
                    print(PadRight(mnem, OPERAND_PLACEHOLDER_LENGTH + 1)).println("%d;", getUShort(pc + 2));
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
            case opc_aload, opc_astore, opc_fload, opc_fstore, opc_iload, opc_istore, opc_lload, opc_lstore, opc_dload, opc_dstore, opc_ret -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).println(getUByte(pc + 1) + ";");
                return 2;
            }
            case opc_iinc -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                        println("%d, %d;", getUByte(pc + 1), getByte(pc + 2));
                return 3;
            }
            case opc_tableswitch -> {
                int tb = align(pc + 1);
                int default_skip = getInt(tb); /* default skip pamount */

                int low = getInt(tb + 4);
                int high = getInt(tb + 8);
                int count = high - low;

                printPadRight(format("%s { ", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH)), getCommentOffset() - 1);
                println(printCPIndex && !skipComments ? " // " + low + " to " + high : "");
                for (int i = 0; i <= count; i++) {
                    // 9 == "default: ".length()
                    print(enlargedIndent(PadRight(format("%2d: ", i + low), 9), shift)).
                            println(data.lP + (pc + getInt(tb + 12 + 4 * i)) + ";");
                }
                print(enlargedIndent(
                        PadRight("default: " + data.lP + (default_skip + pc), OPERAND_PLACEHOLDER_LENGTH - getIndentStep() - 2), shift)
                ).println(" };");
                return tb - pc + 16 + count * 4;
            }
            case opc_lookupswitch -> {
                int tb = align(pc + 1);
                int default_skip = getInt(tb);
                int nPairs = getInt(tb + 4);

                printPadRight(format("%s { ", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH)), getCommentOffset() - 1);
                println(printCPIndex && !skipComments ? " // " + nPairs : "");
                Pair<Integer,Integer>[] lookupswitchPairs = getLookupswitchPairs(tb,nPairs);
                // 9 == "default: ".length()
                int caseLength = Math.max(9, Arrays.stream(lookupswitchPairs).
                        mapToInt(p->String.valueOf(p.first).length()).max().orElse(0) + 2);
                for (int i = 0; i < nPairs; i++) {
                    print(enlargedIndent(PadRight(format("%2d:", lookupswitchPairs[i].first), caseLength), shift)).
                            println(data.lP + (pc + lookupswitchPairs[i].second) + ";");
                }
                print(enlargedIndent(
                        PadRight(PadRight("default: ", caseLength) + data.lP + (default_skip + pc),
                                OPERAND_PLACEHOLDER_LENGTH - getIndentStep() - 2), shift)).println(" };");
                return tb - pc + (nPairs + 1) * 8;
            }
            case opc_newarray -> {
                int tp = getUByte(pc + 1);
                BasicType basicType = getBasicType(tp);
                if (basicType == null) {
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                            println("BOGUS TYPE: " + toHex(tp, 8) + ";");
                } else {
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                            println(basicType.printValue() + ";");
                }
                return 2;
            }
            case opc_ldc, opc_ldc_w, opc_ldc2_w, opc_invokedynamic -> {
                // added printing of the tag: Method/Interface to clarify
                // interpreting CONSTANT_MethodHandle_info:reference_kind
                // Example: ldc_w Dynamic REF_invokeStatic:Method CondyIndy.condy_bsm
                int index, opLength;
                List<Integer> breakPositions = new ArrayList<>();
                if (opcode == Opcode.opc_ldc) {
                    opLength = 2;
                    breakPositions.add(3);
                    index = getUByte(pc + 1);
                } else if (opcode == Opcode.opc_invokedynamic) {
                    opLength = 5;
                    breakPositions.addAll(Set.of(2, 3));
                    index = getUShort(pc + 1); // getUbyte(pc + 3); // getUbyte(pc + 4); // reserved bytes
                } else {    // opc_ldc*_w
                    opLength = 3;
                    breakPositions.add(3);
                    index = getUShort(pc + 1);
                }
                pool.setPrintTAG(true);
                if (printCPIndex) {
                    if( skipComments ) {
                        println(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index));
                    } else {
                        printPadRight(
                                format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index), getCommentOffset() - 1).
                                print(" // ");
                        println(
                                formatOperandLine(pool.ConstantStrValue(index), getCommentOffset() + shift - 1, " // ", breakPositions));
                    }
                } else {
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                    println(formatOperandLine(pool.ConstantStrValue(index), OPERAND_PLACEHOLDER_LENGTH + shift + 1, "", breakPositions) + ";");
                }
                pool.setPrintTAG(false);
                return opLength;
            }
            // Valhalla
            case opc_anewarray, opc_instanceof, opc_checkcast, opc_new, opc_putstatic, opc_getstatic, opc_putfield,
                    opc_getfield, opc_invokevirtual, opc_invokespecial, opc_invokestatic, opc_withfield,
                    opc_aconst_init ->   // Valhalla
                    {
                        int index = getUShort(pc + 1);
                        if (printCPIndex) {
                            if( skipComments ) {
                                println(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index));
                            } else {
                                printPadRight(format("%s #%d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index),
                                        getCommentOffset() - 1).println(" // " + pool.ConstantStrValue(index));
                            }
                        } else {
                            print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                            println(pool.ConstantStrValue(index) + ";");
                        }
                        return 3;
                    }
            case opc_multianewarray, opc_invokeinterface -> {
                int index = getUShort(pc + 1);
                int dimensions = getUByte(pc + 3);  // nargs in case of opc_invokeinterface
                if (printCPIndex) {
                    if( skipComments ) {
                        println(format("%s #%d, %d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index, dimensions));
                    } else {
                        printPadRight(format("%s #%d, %d;", PadRight(operand, OPERAND_PLACEHOLDER_LENGTH), index, dimensions),
                                getCommentOffset() - 1).println(" // " + pool.ConstantStrValue(index));
                    }
                } else {
                    print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1));
                    println("%s, %d;", pool.ConstantStrValue(index), dimensions);
                }
                return opcode == Opcode.opc_multianewarray ? 4 : 5;
            }
            case opc_sipush -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                        println(getShort(pc + 1) + ";");
                return 3;
            }
            case opc_bipush -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                        println(getByte(pc + 1) + ";");
                return 2;
            }
            case opc_jsr, opc_goto, opc_ifeq, opc_ifge, opc_ifgt, opc_ifle, opc_iflt, opc_ifne, opc_if_icmpeq,
                    opc_if_icmpne, opc_if_icmpge, opc_if_icmpgt, opc_if_icmple, opc_if_icmplt, opc_if_acmpeq,
                    opc_if_acmpne, opc_ifnull, opc_ifnonnull -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                        println(data.lP + (pc + getShort(pc + 1)) + ";");
                return 3;
            }
            case opc_jsr_w, opc_goto_w -> {
                print(PadRight(operand, OPERAND_PLACEHOLDER_LENGTH + 1)).
                        println(data.lP + (pc + getInt(pc + 1)) + ";");
                return 5;
            }
            default -> {
                println(operand + ";");
                return 1;
            }
        }
    } // end printInstr

    private Pair<Integer,Integer>[] getLookupswitchPairs(int pad, int count) {
        Pair<Integer,Integer>[] pairs = new Pair[count];
        for (int i = 1; i <= count; i++) {
            pairs[i-1] = new Pair<>(getInt(pad + i * 8), getInt(pad + 4 + i * 8) );
        }
        return pairs;
    }

    /**
     * Formats invokedynamic/ldc dynamic operand line
     *
     * @param str            non-formatted operand line
     * @param offset         indent for new lines
     * @param prefix         prefix placed upfront new lines
     * @param breakPositions numbers where after ":" a lineSeparator is added to wrap a very long operand lines
     * @return formatted operand line
     */
    private String formatOperandLine(String str, int offset, String prefix, List<Integer> breakPositions) {
        StringTokenizer st = new StringTokenizer(str, ":\"{}\\" + ARGUMENT_DELIMITER + LINE_SPLITTER, true);
        StringBuilder sb = new StringBuilder(80);
        boolean processTokens = true;
        String prevToken = "";
        int nItems = 0, nLevel = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            switch (token) {
                case ":":
                    sb.append(token);
                    if (processTokens) {
                        nItems++;
                        if (breakPositions.contains(nItems) && nLevel == 0) {
                            sb.append(lineSeparator()).append(nCopies(offset)).append(prefix);
                        }
                    }
                    break;
                case "}":
                    if (processTokens) {
                        nLevel--;
                        sb.append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel)).append(token);
                    } else
                        sb.append(token);
                    break;
                case "{":
                    if (processTokens) {
                        nLevel++;
                        sb.append(" {").append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                    } else {
                        sb.append(token);
                    }
                    break;
                case "\"":
                    if (!prevToken.equals("\\"))
                        processTokens = !processTokens;
                    sb.append(token);
                    break;
                case ARGUMENT_DELIMITER:
                    if (processTokens)
                        sb.append(',').append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                    else
                        sb.append(ARGUMENT_DELIMITER);
                    break;
                case LINE_SPLITTER:
                    if (processTokens)
                        sb.append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                    else
                        sb.append(ARGUMENT_DELIMITER);
                    break;
                default:
                    sb.append(token);
                    break;
            }
            prevToken = token;
        }
        return sb.toString();
    }

    /**
     * Prints the code data to the current output stream. called from MethodData.
     */
    public void print() throws IOException {

        if (!lin_num_tb.isEmpty()) {
            loadLineNumTable();
        }
        if (stack_map != null) {
            loadStackMap();
        }
        if (!data.printProgramCounter) {
            loadLabelTable();
        }
        loadTrapTable();
        if (!loc_var_tb.isEmpty()) {
            loadLocVarTable();
        }

        println().incIndent().printIndentPadRight(JasmTokens.Token.STACK.parseKey(), PROGRAM_COUNTER_PLACEHOLDER_LENGTH);
        println("%d locals  %d", max_stack, max_locals).decIndent();

        // Need to print ParamAnnotations here.
        data.incIndent();
        data.printPAnnotations();
        data.decIndent();

        printIndentLn("{");

        InstructionAttr insAttr = instructionAttrs.get(0);
        int instructionOffset, attributeOffset;

        if (data.printProgramCounter) {
            instructionOffset = PROGRAM_COUNTER_PLACEHOLDER_LENGTH;
            attributeOffset = instructionOffset;
        } else {
            instructionOffset = INSTR_PREFIX_LENGTH;
            attributeOffset = instructionOffset - getIndentStep();
        }

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
                printIndent(PadRight(((insAttr != null) && insAttr.referred) ? data.lP + pc + ":" : " ", instructionOffset));
                incIndent();
            }

            if (insAttr != null) {
                if (insAttr.printStackMap(attributeOffset)) {
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
        if (insAttr != null && insAttr.stackMapEntry != null) {
            if (data.printProgramCounter) {
                incIndent();
                printIndent(PadRight(format("%2d:", code.length), instructionOffset));
            } else {
                printIndent((PadRight((insAttr.referred) ? data.lP + code.length + ":" : "?", instructionOffset)));
                incIndent();
            }
            decIndent();
        }
        // print TypeAnnotations
        if (visibleTypeAnnotations != null) {
            println();
            for (TypeAnnotationData<MethodData> vta : visibleTypeAnnotations) {
                vta.print();
                println();
            }
        }
        if (invisibleTypeAnnotations != null) {
            println(() -> visibleTypeAnnotations == null);
            for (TypeAnnotationData<MethodData> ita : invisibleTypeAnnotations) {
                ita.print();
                println();
            }
        }
        // end of code
        printIndentLn("}");
    }

    public JdisEnvironment getEnvironment() {
        return environment;
    }

    public static class LocVarData {
        short start_pc, length, name_cpx, sig_cpx, slot;

        public LocVarData(DataInputStream in) throws IOException {
            start_pc = in.readShort();
            length = in.readShort();
            name_cpx = in.readShort();
            sig_cpx = in.readShort();
            slot = in.readShort();
        }
    }

    /* Code Data inner classes */
    static class LineNumData {
        short start_pc, line_number;

        public LineNumData(DataInputStream in) throws IOException {
            start_pc = in.readShort();
            line_number = in.readShort();
        }
    }
}
