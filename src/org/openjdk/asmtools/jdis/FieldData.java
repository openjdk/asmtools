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

import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Field data for field members in a class of the Java Disassembler
 */
public class FieldData extends MemberData<ClassData> {

    // CP index to the field name
    protected int name_cpx;
    // CP index to the field type
    protected int type_cpx;
    // CP index to the field value
    protected int value_cpx = 0;

    public FieldData(ClassData classData) {
        super(classData);
        memberType = "FieldData";
    }

    @Override
    protected boolean handleAttributes(DataInputStream in, EAttribute attributeTag, int attributeLength) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attributeTag) {
            case ATT_Signature -> {
                if (signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "field_info");
                }
                signature = new SignatureData(data).read(in, attributeLength);
            }
            case ATT_ConstantValue -> {
                if (attributeLength != 2) {
                    throw new FormatError(environment.getLogger(),
                            "err.invalid.attribute.length",
                            EAttribute.ATT_ConstantValue.printValue(), attributeLength);
                }
                value_cpx = in.readUnsignedShort();
            }
            default -> handled = false;
        }
        return handled;
    }

    /**
     * Read and resolve the field data called from ClassData.
     * Precondition: NumFields has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Fields CP indexes
        access = in.readUnsignedShort();
        name_cpx = in.readUnsignedShort();
        type_cpx = in.readUnsignedShort();
        // Read the attributes
        readAttributes(in);
        //
        environment.traceln("FieldData: name[%d]=%s type[%d]=%s%s",
                name_cpx, data.pool.getString(name_cpx, index -> "#" + index + "?"),
                type_cpx, data.pool.getString(type_cpx, index -> "#" + index + "?"),
                signature != null ? signature : "");
    }

    /**
     * Prints the field data to the current output stream. called from ClassData.
     */
    @Override
    public void print() throws IOException {
        // Print annotations first
        super.printAnnotations();
        // print field
        StringBuilder prefix = new StringBuilder(getIndentString()).
                append(EModifier.asKeywords(access, ClassFileContext.FIELD));
        // add synthetic, deprecated if necessary
        prefix.append(getPseudoFlagsAsString());
        // field
        prefix.append(JasmTokens.Token.FIELDREF.parseKey()).append(' ');
        printVar(prefix, (value_cpx != 0) ? (" = ").concat(data.pool.ConstantStrValue(value_cpx)) : null,
                name_cpx, type_cpx, value_cpx);
    }
} // end FieldData
