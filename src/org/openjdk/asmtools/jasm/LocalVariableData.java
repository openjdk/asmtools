/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.ToolLogger;
import org.openjdk.asmtools.jdis.notations.Signature;

import java.io.IOException;

/**
 Class for entries: local_variable_table[i], local_variable_type_table[i]
 *
 * 4.7.13. The LocalVariableTable Entry: local_variable_table[i]
 * <p>
 * LocalVariableTable_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 local_variable_table_length;
 *     {   u2 start_pc;
 *         u2 length;
 *         u2 name_index;
 *         u2 descriptor_index;
 *         u2 index;
 *     } local_variable_table[local_variable_table_length];
 * }
 * <p>
 * 4.7.13. The LocalVariableTypeTable Entry: local_variable_type_table[i]
 * LocalVariableTypeTable_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 local_variable_type_table_length;
 *     {   u2 start_pc;
 *         u2 length;
 *         u2 name_index;
 *         u2 signature_index;
 *         u2 index;
 *     } local_variable_type_table[local_variable_type_table_length];
 * }
 */
class LocalVariableData implements DataWriter {
    private final short index;

    // assisting fields
    FieldType fieldType;
    Signature<ToolLogger> fieldSignature;
    private short start_pc;
    private short length;
    private ConstCell nameCell;
    private ConstCell descriptorCell;

    /**
     * Creates an entry of Local Variable Table
     *
     * @param index The value of the index item must be a valid index into the local variable array of the current frame.
     *              The given local variable is at index in the local variable array of the current frame
     *              var is presented in the form: var index  (#)name(_index):(#)descriptor(_index);
     */
    public LocalVariableData(short index, short curPC, ConstCell nameCell, ConstCell descriptorCell) {
        this.index = index;
        this.start_pc = curPC;
        this.nameCell = nameCell;
        this.descriptorCell = descriptorCell;
    }

    public LocalVariableData(short start_pc, short length, short slot, ConstCell nameCell, ConstCell descriptorCell) {
        this.start_pc = start_pc;
        this.length = length;
        this.index = slot;
        this.nameCell = nameCell;
        this.descriptorCell = descriptorCell;
    }

    public FieldType getFieldType() {
        if( this.fieldType == null ) {
            this.fieldType = FieldType.getFieldType(((String) descriptorCell.ref.value).charAt(0));
        }
        return this.fieldType;
    }

    public LocalVariableData setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    public Signature<ToolLogger> getSignature(ToolLogger logger) {
        if ( this.fieldSignature == null) {
            this.fieldSignature = new Signature<>(logger, (String) descriptorCell.ref.value);
        }
        return this.fieldSignature;
    }

    public LocalVariableData setFieldSignature(Signature<ToolLogger> fieldSignature) {
        this.fieldSignature = fieldSignature;
        return this;
    }

    public int getSlotsCount() {
        return (getFieldType() == null) ? 0 : this.fieldType.getSlotsCount();
    }

    public short getIndex() {
        return index;
    }

    @Override
    public int getLength() {
        return 10;
    }

    public void setLength(int CurrentPC) {
        this.length = (short) (CurrentPC - start_pc);
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(start_pc);
        out.writeShort(length);
        out.writeShort(nameCell.cpIndex);
        out.writeShort(descriptorCell.cpIndex);
        out.writeShort(index);
    }

    public void setStartPc(short start_pc) {
        this.start_pc = start_pc;
    }
}
