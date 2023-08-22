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
package org.openjdk.asmtools.jasm;


import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.OpcodeTables.Opcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    protected final List<LocVarData> locVarSlots;
    private final LocVarData VACANT = null;

    protected ClassData classData;              // reference to the surrounding data containers
    protected MethodData methodData;
    protected JasmEnvironment environment;

    protected Indexer max_stack, max_locals;
    protected Instr zeroInstr, lastInstr;
    protected int curPC = 0;
    protected DataVector<ExceptionData> exceptionTable;             // TrapData
    protected DataVectorAttr<LineNumberData> lineNumberTable;       // LineNumData
    protected int lastLineNumber = 0;
    protected DataVectorAttr<LocVarData> localVariableTable;        // LocVarData
    protected DataVector<DataVectorAttr<? extends DataWriter>> attributes;
    protected HashMap<String, Label> labelsHash;
    protected HashMap<String, RangePC> trapsHash;
    protected StackMapData curMapEntry = null;
    protected DataVectorAttr<StackMapData> stackMap;
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
        checkLocVars();
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
                        inVisTypeAnnotations = new DataVectorAttr(methodData.pool, EAttribute.ATT_RuntimeInvisibleTypeAnnotations);
                    }
                    inVisTypeAnnotations.add(typeAnnotationData);
                } else {
                    if (this.visTypeAnnotations == null) {
                        this.visTypeAnnotations = new DataVectorAttr(methodData.pool, EAttribute.ATT_RuntimeVisibleTypeAnnotations);
                    }
                    this.visTypeAnnotations.add(typeAnnotationData);
                }
            }
        }
    }

    /* -------------------------------------- Traps */
    RangePC trapDecl(int pos, String name) {
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

    void beginTrap(int pos, String name) {
        RangePC rangePC = trapDecl(pos, name);
        if (rangePC.start_pc != Indexer.NotSet) {
            environment.error("err.trap.tryredecl", name);
            return;
        }
        rangePC.start_pc = curPC;
    }

    void endTrap(int pos, String name) {
        RangePC rangePC = trapDecl(pos, name);
        if (rangePC.end_pc != Indexer.NotSet) {
            environment.error("err.trap.endtryredecl", name);
            return;
        }
        rangePC.end_pc = curPC;
    }

    void trapHandler(int pos, String name, Indexer type) {
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

    public Label LabelDef(int pos, String name) {
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
     * @param position       scanners' position to navigate where a syntax error happened if any
     * @param index          a valid index into the local variable array of the current frame
     * @param nameCell       valid unqualified name denoting a local variable
     * @param descriptorCell a field descriptor which encodes a type of local variable in the source program
     */
    public void LocVarDataDef(int position, int index, ConstCell<?> nameCell, ConstCell<?> descriptorCell) {
        LocVarData locVarData = new LocVarData((short) index, (short) curPC, nameCell, descriptorCell);
        FieldType fieldType = locVarData.getFieldType();
        // check slot availability
        //If the given local variable is of type double or long, it occupies both index and index + 1
        for (int i = 0; i < fieldType.getSlotsCount(); i++) {
            if (!max_locals.inRange(index + i)) {
                environment.error(position, "err.locvar.wrong.index", index + i, max_locals.value() - 1);
                throw new SyntaxError();
            }
            if (locVarSlots.get(index + i) != VACANT) {
                environment.error(position, "err.locvar.slot.occupied", index + i);
                throw new SyntaxError();
            }
            locVarSlots.set(index + i, locVarData); // OCCUPIED
        }
        if (localVariableTable == null) {
            localVariableTable = new DataVectorAttr<>(methodData.pool, EAttribute.ATT_LocalVariableTable);
            attributes.add(localVariableTable);
        }
        localVariableTable.add(locVarData);
    }

    /**
     * Marks the end of Local Variable presented in the form endVar index: locVarSlots[slot] = VACANT
     * and sets the Length of the Local Var
     *
     * @param position the position of the scanner
     * @param slot     The value of the index item is a valid index into the local variable array of the current frame.
     */
    public void LocVarDataEnd(short slot, int position) {
        if (!max_locals.inRange(slot)) {
            environment.error(position, "err.locvar.wrong.index", slot, max_locals.value() - 1);
            throw new SyntaxError();
        }
        final LocVarData locVarData = locVarSlots.get(slot);
        if (locVarData == VACANT) {
            environment.error(position, "err.locvar.undecl", slot);
            throw new SyntaxError();
        }
        locVarData.setLength(curPC);
        // Check slot availability and clean up appropriate locVarSlots[slot{,slot+1}]
        // If the given local variable is of type double or long, it occupies both index and index + 1
        for (int i = 0; i < locVarData.getSlotsCount(); i++) {
            if (i > 0 && !max_locals.inRange(slot + i)) {
                environment.error(position, "err.locvar.wrong.index", slot + i, max_locals.value() - 1);
                throw new SyntaxError();
            }
            if (i > 0 && locVarSlots.get(slot + i) == VACANT) {
                environment.error(position, "err.locvar.undecl", slot + i);
                throw new SyntaxError();
            }
            locVarSlots.set(slot + i, VACANT);
        }
    }

    void checkLocVars() {
        for (int i = 0; i < locVarSlots.size(); i++) {
            if (locVarSlots.get(i) != VACANT) {
                locVarSlots.get(i).setLength(curPC);
                environment.warning(environment.getPosition(), "warn.locvar.ambiqous", i);
            }
        }
    }

    // The StackMap
    public StackMapData getStackMap() {
        if (curMapEntry == null) {
            curMapEntry = new StackMapData(environment);
            curMapEntry.setIsStackMapTable(classData.cfv.isTypeCheckingVerifier());
        }
        return curMapEntry;
    }

    // Instructions
    void addInstr(int mnenoc_pos, Opcode opcode, Indexer arg, Object arg2) {
        Instr newInstr = new Instr(methodData, environment).set(curPC, environment.getPosition(), opcode, arg, arg2);
        lastInstr.next = newInstr;
        lastInstr = newInstr;
        int len = opcode.length();
        switch (opcode) {
            case opc_tableswitch:
                len = ((SwitchTable) arg2).recalcTableSwitch(curPC);
                break;
            case opc_lookupswitch:
                len = ((SwitchTable) arg2).calcLookupSwitch(curPC);
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
            int ln = environment.lineNumber(mnenoc_pos);
            if (ln != lastLineNumber) { // only one entry in lineNumberTable per line
                lineNumberTable.add(new LineNumberData(curPC, ln));
                lastLineNumber = ln;
            }
        }
        if (curMapEntry != null) {
            curMapEntry.setPC(curPC);
            StackMapData prevStackFrame = null;
            if (stackMap == null) {
                if (classData.cfv.isTypeCheckingVerifier()) {
                    stackMap = new DataVectorAttr<>(classData.pool, EAttribute.ATT_StackMapTable);
                } else {
                    stackMap = new DataVectorAttr<>(classData.pool, EAttribute.ATT_StackMap);
                }
                attributes.add(stackMap);
            } else if (stackMap.size() > 0) {
                prevStackFrame = stackMap.get(stackMap.size() - 1);
            }
            curMapEntry.setOffset(prevStackFrame);
            stackMap.add(curMapEntry);
            curMapEntry = null;
        }
        curPC += len;
    }

    @Override
    public int attrLength() {
        return 2 + 2 + 4 // for max_stack, max_locals, and cur_pc
                + curPC //      + 2+trap_table.size()*8
                + exceptionTable.getLength() + attributes.getLength();
    }

    @Override
    public void write(CheckedDataOutputStream out)
            throws IOException, Parser.CompilerError {
        int maxStack = (max_stack != null) ? max_stack.cpIndex : 0;
        int maxLocals = (max_locals != null) ? max_locals.cpIndex : locVarSlots.size();
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

    /* CodeAttr inner classes */
    static public class Local extends Indexer {
        String name;
        boolean isDefined = false, isReferred = false;

        public Local(String name) {
            this.name = name;
        }
    }

    /**
     *
     */
    static public class Label extends Local {
        public Label(String name) {
            super(name);
        }
    }

    /**
     *
     */
    static class RangePC extends Local {
        int start_pc = Indexer.NotSet;
        int end_pc = Indexer.NotSet;
        int pos;

        RangePC(int pos, String name) {
            super(name);
            this.pos = pos;
        }
    }

} // end CodeAttr
