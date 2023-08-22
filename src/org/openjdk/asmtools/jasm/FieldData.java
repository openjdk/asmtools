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

import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.IOException;

/**
 *  field_info
 */
class FieldData extends MemberData<JasmEnvironment> {

    /* FieldData Fields */
    private ConstantPool.ConstValue_FieldRef fieldRef;
    private AttrData initialValue;

    public FieldData(ClassData classData, int access, ConstantPool.ConstValue_FieldRef fieldRef) {
        super(classData.pool, classData.getEnvironment(), access);
        this.fieldRef = fieldRef;
        if (EModifier.hasPseudoMod(access)) {
            createPseudoMod();
        }
    }

    public ConstantPool.ConstValue_FieldRef getNameDesc() {
        return fieldRef;
    }

    public void SetInitialValue(ConstCell<?>  cell) {
        initialValue = new CPXAttr(pool, EAttribute.ATT_ConstantValue, cell);
    }

    public AttrData getInitialValue() {
        return initialValue;
    }

    @Override
    protected DataVector getAttrVector() {
        return getDataVector(initialValue, syntheticAttr, deprecatedAttr, signatureAttr);
    }

    public void write(CheckedDataOutputStream out) throws IOException, Parser.CompilerError {
        out.writeShort(access);
        out.writeShort(fieldRef.value.first.cpIndex);
        out.writeShort(fieldRef.value.second.cpIndex);
        DataVector attrs = getAttrVector();
        attrs.write(out);
    }
} // end FieldData
