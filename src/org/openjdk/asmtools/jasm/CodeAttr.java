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
package org.openjdk.asmtools.jasm;


import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.OpcodeTables.Opcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.max;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_StackMap;
import static org.openjdk.asmtools.common.structure.EAttribute.ATT_StackMapTable;
import static org.openjdk.asmtools.jasm.OpcodeTables.MAX_LOOKUPSWITCH_LENGTH;
import static org.openjdk.asmtools.jasm.OpcodeTables.MAX_TABLESWITCH_LENGTH;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode.opc_lookupswitch;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode.opc_tableswitch;

/**
 * 4.7.3. The Code Attribute
 * <p>
 * Code_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 max_stack;
 * u2 max_locals;
 * u4 code_length;
 * u1 code[code_length];
 * u2 exception_table_length;
 * {   u2 start_pc;
 * u2 end_pc;
 * u2 handler_pc;
 * u2 catch_type;
 * } exception_table[exception_table_length];
 * u2 attributes_count;
 * attribute_info attributes[attributes_count];
 * }
 */
class CodeAttr extends AttrData {
    protected final List<LocalVariableData> locVarSlots;
    protected final List<LocalVariableData> locVarTypeSlots;
    private final LocalVariableData VACANT = null;
    // reference to the surrounding data containers
    protected ClassData classData;
    protected MethodData methodData;
    protected JasmEnvironment environment;

    protected Indexer max_stack, max_locals;
    protected Instr zeroInstr, lastInstr;
    protected int curPC = 0;
    protected DataVector<ExceptionData> exceptionTable;
    protected DataVectorAttr<LineNumberData> lineNumberTable;
    protected long lastLineNumber = 0;
    protected DataVectorAttr<LocalVariableData> localVariableTable;
    protected DataVectorAttr<LocalVariableData> localVariableTypeTable;

    protected DataVector<DataVectorAttr<? extends DataWriter>> attributes;
    protected HashMap<String, Label> labelsHash;
    protected HashMap<String, RangePC> trapsHash;
    protected List<StackMapData> stackMapEntries = new ArrayList<>();
    protected DataVectorAttr<StackMapData> stackMapTable;
    // type annotations
    protected DataVectorAttr<TypeAnnotationData> visTypeAnnotations = null;
    protected DataVectorAttr<TypeAnnotationData> inVisTypeAnnotations = null;

    public CodeAttr(MethodData methodData, int paramCount, Indexer max_stack, Indexer max_locals) {
        super(methodData.pool, EAttribute.ATT_Code);
        this.methodData = methodData;
        this.classData = methodData.classData;
        this.environment = methodData.getEnvironment();
        this.max_stack = max_stack;
        this.max_locals = max_locals;
        this.locVarSlots = new ArrayList<>(Collections.nCopies(max_locals != null ? max_locals.value() : paramCount, VACANT));
        this.locVarTypeSlots = new ArrayList<>(Collections.nCopies(max_locals != null ? max_locals.value() : paramCount, VACANT));
        lastInstr = zeroInstr = new Instr(methodData, environment);
        exceptionTable = new DataVector<>(0); // TrapData
        attributes = new DataVector<>();
        if (environment.getVerboseFlag()) {
            lineNumberTable = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_LineNumberTable);
            attributes.add(lineNumberTable);
        }
    }

    void endCode() {
        checkTraps();
        checkLocVars(Opcode.opc_var);
        checkLocVars(Opcode.opc_type);
        checkLabels();
        //
        if (visTypeAnnotations != null) {
            attributes.add(visTypeAnnotations);
        }
        if (inVisTypeAnnotations != null) {
            attributes.add(inVisTypeAnnotations);
        }
    }

    public void addAnnotations(ArrayList<AnnotationData> list) {
        for (AnnotationData item : list) {
            boolean invisible = item.invisible;
            if (item instanceof TypeAnnotationData typeAnnotationData) {
                // Type Annotations
                if (invisible) {
                    if (inVisTypeAnnotations == null) {
                        inVisTypeAnnotations = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_RuntimeInvisibleTypeAnnotations);
                    }
                    inVisTypeAnnotations.add(typeAnnotationData);
                } else {
                    if (this.visTypeAnnotations == null) {
                        this.visTypeAnnotations = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_RuntimeVisibleTypeAnnotations);
                    }
                    this.visTypeAnnotations.add(typeAnnotationData);
                }
            }
        }
    }

    public void fillLineTable(List<LineNumberData> list) {
        if (lineNumberTable != null) {
            // Remove the automatically generated LineTable by jasm and instead,
            // include a table inline within the jasm source.
            lineNumberTable.clear();
        } else {
            lineNumberTable = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_LineNumberTable);
            attributes.add(lineNumberTable);
        }
        lineNumberTable.addAll(list);
    }

    /**
     * Fills either localVariableTable or localVariableTypeTable according to the boolean parameter isTypeTable
     *
     * @param isTypeTable defines which localVariableTypeTable or localVariableTable is filled
     * @param list        list of local_variable_table[i] or local_variable_type_table[i] entries
     */
    public void fillLocalVariableTable(boolean isTypeTable, List<LocalVariableData> list) {
        DataVectorAttr<LocalVariableData> vector = (isTypeTable) ? localVariableTypeTable : localVariableTable;
        if (vector == null) {
            vector = new DataVectorAttr<>(methodData.pool, (isTypeTable) ? EAttribute.ATT_LocalVariableTypeTable : EAttribute.ATT_LocalVariableTable);
            attributes.add(vector);
        }
        vector.addAll(list);
    }

    public void fillStackMapTable(List<StackMapData> list) {
        if (stackMapTable == null) {
            DataVectorAttr<StackMapData> table = (DataVectorAttr<StackMapData>) attributes.
                    findFirst(item -> item.getAttribute().
                            isOneOf(ATT_StackMapTable, ATT_StackMap)).orElse(null);
            if (table == null) {
                stackMapTable = new DataVectorAttr<>(classData.pool, classData.cfv.isTypeCheckingVerifier() ? ATT_StackMapTable : ATT_StackMap);
                attributes.add(stackMapTable);
            } else {
                stackMapTable = table;
            }
        }
        stackMapTable.addAll(list);
    }

    /* -------------------------------------- Traps */
    RangePC trapDecl(long pos, String name) {
        RangePC local;
        if (trapsHash == null) {
            trapsHash = new HashMap<>(10);
            local = null;
        } else {
            local = trapsHash.get(name);
        }
        if (local == null) {
            local = new RangePC(pos, name);
            trapsHash.put(name, local);
        }
        return local;
    }

    void beginTrap(long pos, String name) {
        RangePC rangePC = trapDecl(pos, name);
        if (rangePC.start_pc != Indexer.NotSet) {
            environment.error("err.trap.tryredecl", name);
            return;
        }
        rangePC.start_pc = curPC;
    }

    void endTrap(long pos, String name) {
        RangePC rangePC = trapDecl(pos, name);
        if (rangePC.end_pc != Indexer.NotSet) {
            environment.error("err.trap.endtryredecl", name);
            return;
        }
        rangePC.end_pc = curPC;
    }

    void trapHandler(long pos, String name, Indexer type) {
        RangePC rangePC = trapDecl(pos, name);
        rangePC.isReferred = true;
        ExceptionData exceptionData = new ExceptionData(pos, rangePC, curPC, type);
        exceptionTable.addElement(exceptionData);
    }

    void checkTraps() {
        if (trapsHash == null) {
            return;
        }
        for (RangePC rangePC : trapsHash.values()) {
            if (!rangePC.isReferred) {
                environment.warning(rangePC.pos, "warn.trap.notref", rangePC.name);
            }
        }

        for (ExceptionData exceptionData : exceptionTable) {
            RangePC rangePCLabel = exceptionData.rangePC;
            if (rangePCLabel.start_pc == Indexer.NotSet) {
                environment.error(exceptionData.pos, "err.trap.notry", rangePCLabel.name);
            }
            if (rangePCLabel.end_pc == Indexer.NotSet) {
                environment.error(exceptionData.pos, "err.trap.noendtry", rangePCLabel.name);
            }
        }
    }

    // Labels
    Label labelDecl(String name) {
        Label local;
        if (labelsHash == null) {
            labelsHash = new HashMap<>(10);
            local = null;
        } else {
            local = labelsHash.get(name);
        }
        if (local == null) {
            local = new Label(name);
            labelsHash.put(name, local);
        }
        return local;
    }

    public Label LabelDef(long pos, String name) {
        Label label = labelDecl(name);
        if (label.isDefined) {
            environment.error(pos, "err.label.redecl", name);
            return null;
        }
        label.isDefined = true;
        label.cpIndex = curPC;
        return label;
    }

    public Label LabelRef(String name) {
        Label label = labelDecl(name);
        label.isReferred = true;
        return label;
    }

    void checkLabels() {
        if (labelsHash == null) {
            return;
        }
        for (Label local : labelsHash.values()) {
            // check that every label is defined
            if (!local.isDefined) {
                environment.error("err.label.undecl", local.name);
            }
        }
    }

// LocalVariables

    /**
     * Constructs the local variable nameCell:descriptorCell assigned to the slot index.
     *
     * @param opcode         var or type opcode that defines type of filled table -LocalVariableTypeTable or LocalVariableTable
     * @param position       scanners' position to navigate where a syntax error happened if any
     * @param index          a valid index into the local variable array of the current frame
     * @param nameCell       valid unqualified name denoting a local variable
     * @param descriptorCell a field descriptor which encodes a type of local variable in the source program
     */
    public void LocVarDataDef(OpcodeTables.Opcode opcode, long position, int index, ConstCell<?> nameCell, ConstCell<?> descriptorCell) {
        FieldType fieldType = null;
        List<LocalVariableData> slots;
        LocalVariableData localVariableData = new LocalVariableData((short) index, (short) curPC, nameCell, descriptorCell);
        if (opcode == Opcode.opc_var) {
            slots = locVarSlots;
            fieldType = localVariableData.getFieldType();
            if (localVariableTable == null) {
                localVariableTable = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_LocalVariableTable);
                attributes.add(localVariableTable);
            }
            localVariableTable.add(localVariableData);
        } else {
            slots = locVarTypeSlots;
            if (localVariableTable != null) {
                LocalVariableData lvd = localVariableTable.findFirst(lv -> lv.getIndex() == index).orElse(null);
                if (lvd != null) {
                    fieldType = lvd.getFieldType();
                }
            }
            if (localVariableTypeTable == null) {
                localVariableTypeTable = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_LocalVariableTypeTable);
                attributes.add(localVariableTypeTable);
            }
            localVariableTypeTable.add(localVariableData);
        }
        if (fieldType == null) {
            environment.throwErrorException(position, "err.fieldType.undecl", index);
        } else {
            localVariableData.setFieldType(fieldType);
            // check slot availability
            //If the given local variable is of type double or long, it occupies both index and index + 1
            for (int i = 0; i < fieldType.getSlotsCount(); i++) {
                if (!max_locals.inRange(index + i)) {
                    environment.throwErrorException(position, "err.locvar.wrong.index", index + i, max_locals.value() - 1);
                }

                if (slots.get(index + i) != VACANT) {
                    environment.throwErrorException(position, "err.locvar.slot.occupied", index + i);
                }
                slots.set(index + i, localVariableData); // OCCUPIED
            }
        }
    }

    /**
     * Marks the end of Local Variable (Type) presented in the form endVar index: locVarSlots[slot] = VACANT
     * and sets the Length of the Local Var
     *
     * @param position the position of the scanner
     * @param slot     The value of the index item is a valid index into the local variable array of the current frame.
     */
    public void LocVarDataEnd(OpcodeTables.Opcode opcode, short slot, long position) {
        if (!max_locals.inRange(slot)) {
            environment.throwErrorException(position, "err.locvar.wrong.index", slot, max_locals.value() - 1);
        }
        final LocalVariableData localVariableData = (opcode == Opcode.opc_var) ? locVarSlots.get(slot) : locVarTypeSlots.get(slot);
        if (localVariableData == VACANT) {
            environment.throwErrorException(position, "err.locvar.undecl", slot);
        } else {
            localVariableData.setLength(curPC);
            // Check slot availability and clean up appropriate locVarSlots[slot{,slot+1}] or locVarTypeSlots[slot{,slot+1}]
            // If the given local variable is of type double or long, it occupies both index and index + 1
            List<LocalVariableData> slots = (opcode == Opcode.opc_var) ? locVarSlots : locVarTypeSlots;

            for (int i = 0; i < localVariableData.getSlotsCount(); i++) {
                if (i > 0 && !max_locals.inRange(slot + i)) {
                    environment.error(position, "err.locvar.wrong.index", slot + i, max_locals.value() - 1);
                    throw new SyntaxError();
                }
                if (i > 0 && slots.get(slot + i) == VACANT) {
                    environment.error(position, "err.locvar.undecl", slot + i);
                    throw new SyntaxError();
                }
                slots.set(slot + i, VACANT);
            }
        }
    }

    void checkLocVars(OpcodeTables.Opcode opcode) {
        List<LocalVariableData> slots = (opcode == Opcode.opc_var) ? locVarSlots : locVarTypeSlots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) != VACANT) {
                slots.get(i).setLength(curPC);
                environment.warning(environment.getPosition(), (opcode == Opcode.opc_var) ? "warn.locvar.ambiqous" :
                        "warn.loctype.ambiqous", i);
            }
        }
    }

    // The StackMap
    public StackMapData getStackMapTable() {
        StackMapData entry;
        int len = stackMapEntries.size();
        if (len == 0) {
            entry = new StackMapData(environment, isTypeCheckingVerifier());
            stackMapEntries.add(entry);
        } else {
            entry = stackMapEntries.get(len - 1);
        }
        return entry;
    }

    public StackMapData getNextStackMapTable() {
        StackMapData entry = new StackMapData(environment, isTypeCheckingVerifier());
        stackMapEntries.add(entry);
        return entry;
    }

    // A class file whose version number is 50.0 or above (§4.1) must be verified using the type checking rules given
    // in the section 4.10.1. Verification by Type Checking
    public boolean isTypeCheckingVerifier() {
        return classData.cfv.isTypeCheckingVerifier();
    }

    // Instructions
    void addInstr(long mnenoc_pos, Opcode opcode, Indexer arg, Object arg2) {
        Instr newInstr = new Instr(methodData, environment).set(curPC, environment.getPosition(), opcode, arg, arg2);
        lastInstr.next = newInstr;
        lastInstr = newInstr;
        int len = opcode.length();
        switch (opcode) {
            case opc_tableswitch:
                len = ((SwitchTable) arg2).recalcTableSwitch(curPC);
                if (len >= MAX_TABLESWITCH_LENGTH) {
                    environment.error(mnenoc_pos, "err.instr.oversize",
                            opc_tableswitch.parseKey(), len, MAX_TABLESWITCH_LENGTH);
                }
                break;
            case opc_lookupswitch:
                len = ((SwitchTable) arg2).calcLookupSwitch(curPC);
                if (len >= MAX_LOOKUPSWITCH_LENGTH) {
                    environment.error(mnenoc_pos, "err.instr.oversize",
                            opc_lookupswitch.parseKey(), len, MAX_LOOKUPSWITCH_LENGTH);
                }
                break;
            case opc_ldc:
                ((ConstCell<?>) arg).setRank(ConstantPool.ReferenceRank.LDC);
                break;
            default:
                if (arg instanceof ConstCell) {
                    ConstantPool.ReferenceRank rank = ((ConstCell<?>) arg).rank;
                    if (rank != ConstantPool.ReferenceRank.LDC) {
                        ((ConstCell<?>) arg).setRank(ConstantPool.ReferenceRank.ANY);
                    }
                }
        }
        if (environment.getVerboseFlag()) {
            long ln = environment.lineNumber(mnenoc_pos);
            if (ln != lastLineNumber) { // only one entry in lineNumberTable per line
                lineNumberTable.add(new LineNumberData(curPC, ln));
                lastLineNumber = ln;
            }
        }
        if (!stackMapEntries.isEmpty()) {
            StackMapData prevStackFrame = getPreviousStackMapEntry();
            for (StackMapData entry : stackMapEntries) {
                if (!entry.isWrapper()) {
                    entry.setPC(curPC);
                    entry.setOffset(prevStackFrame);
                }
            }
            stackMapTable.addAll(stackMapEntries);
            stackMapEntries.clear();
        }
        curPC += len;
    }

    private StackMapData getPreviousStackMapEntry() {
        if (stackMapTable == null) {
            stackMapTable = new DataVectorAttr<>(classData.pool,
                    classData.cfv.isTypeCheckingVerifier() ? ATT_StackMapTable : ATT_StackMap);
            attributes.add(stackMapTable);
        } else if (!stackMapTable.isEmpty()) {
            for (int i = stackMapTable.size() - 1; i >= 0; i--) {
                StackMapData entry = stackMapTable.get(i);
                if (!entry.isWrapper()) {
                    return entry;
                }
            }
        }
        return null;
    }


    @Override
    public int attrLength() {
        return 2 + 2 + 4    // for max_stack, max_locals, and cur_pc
                + curPC     //      + 2+trap_table.size()*8
                + exceptionTable.getLength() + attributes.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out)
            throws IOException, Parser.CompilerError {
        int maxStack = (max_stack != null) ? max_stack.cpIndex : 0;
        int maxLocals = (max_locals != null) ? max_locals.cpIndex : max(locVarSlots.size(), locVarTypeSlots.size());
        super.write(out);  // attr name, attr len
        out.writeShort(maxStack);
        out.writeShort(maxLocals);
        out.writeInt(curPC);
        for (Instr instr = zeroInstr.next; instr != null; instr = instr.next) {
            instr.write(out);
        }
        exceptionTable.write(out);
        attributes.write(out);
    }

    /**
     * Checks if the code attribute is empty, i.e., it contains no instructions.
     *
     * @return true if the code attribute is empty, false otherwise
     */
    public boolean isEmpty() {
        return curPC == 0;
    }

    /* CodeAttr inner classes */
    static public class Local extends Indexer {
        String name;
        boolean isDefined = false, isReferred = false;

        public Local(String name) {
            this.name = name;
        }
    }

    static public class Label extends Local {
        public Label(String name) {
            super(name);
        }
    }

    static class RangePC extends Local {
        int start_pc = Indexer.NotSet;
        int end_pc = Indexer.NotSet;
        long pos;

        RangePC(long pos, String name) {
            super(name);
            this.pos = pos;
        }
    }

} // end CodeAttr
