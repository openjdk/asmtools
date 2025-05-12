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

import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.ELocation;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import static java.lang.Math.max;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.SIGNATURE;
import static org.openjdk.asmtools.jdis.ConstantPool.TAG.*;

/**
 * Field data for field members in a class of the Java Disassembler
 */
public class FieldData extends MemberData<ClassData> implements Measurable {

    // CP index to the field name
    protected int name_cpx;
    // CP index to the field type
    protected int type_cpx;
    // CP index to the field value
    protected int value_cpx = UNDEFINED;

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
                    environment.warning("warn.one.attribute.required",
                            EAttribute.ATT_Signature.printValue(), ELocation.field_info.toString());
                }
                signature = new SignatureData(data).read(in, attributeLength);
            }
            case ATT_ConstantValue -> {
                if (attributeLength != 2) {
                    if (bestEffort) {
                        environment.error("err.invalid.attribute.length", EAttribute.ATT_ConstantValue.printValue(), attributeLength);
                    } else {
                        throw new FormatError(environment.getLogger(),
                                "err.invalid.attribute.length", EAttribute.ATT_ConstantValue.printValue(), attributeLength);
                    }
                }
                if (getListOf(EAttribute.ATT_ConstantValue).size() > 1) {
                    environment.warning("warn.one.attribute.required",
                            EAttribute.ATT_ConstantValue.printValue(), ELocation.field_info.toString());
                }
                value_cpx = in.readUnsignedShort();
                if (!pool.inRange(value_cpx)) {
                    environment.warning("warn.attribute.constantvalue.incorrect", value_cpx);
                } else {
                    if (!pool.CheckEntryType(value_cpx, CONSTANT_INTEGER, CONSTANT_FLOAT, CONSTANT_DOUBLE, CONSTANT_LONG, CONSTANT_STRING)) {
                        ConstantPool.TAG tag = pool.getTag(value_cpx);
                        String tagName = tag == null ? "unknown" : tag.printValue();
                        environment.warning("warn.attribute.type.incorrect", tagName);
                    }
                }
            }
            default -> handled = false;
        }
        return handled;
    }

    @Override
    protected boolean handleUnrecognizedAttributes(DataInputStream in, int attributeNameCpx, int attributeLength) throws IOException {
        if (!data.pool.inRange(attributeNameCpx)) {
            environment.warning("warn.attribute.name.corrupted", attributeNameCpx);
        } else {
            environment.warning("warn.attribute.name.incorrect", attributeNameCpx);
        }
        return false;
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
        environment.traceln(() ->
                "FieldData: name[%d]=%s type[%d]=%s%s".formatted(
                        name_cpx, data.pool.getString(name_cpx, index -> "#%d?".formatted(index)),
                        type_cpx, data.pool.getString(type_cpx, index -> "#%d?".formatted(index)),
                        signature != null ? signature : ""));
    }

    /**
     * Prints the field data to the current output stream called from ClassData.
     */
    @Override
    protected void jasmPrint(int index, int size) throws IOException {
        boolean printSignature = signature != null && signature.isPrintable() && tableFormat;
        // Print annotations first
        super.printAnnotations(visibleAnnotations, invisibleAnnotations);
        super.printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
        if (getListOf(EAttribute.ATT_Unrecognized).size() > 0) {
            List<AttrData> list = getListOf(EAttribute.ATT_Unrecognized);
            for (AttrData attr : list) {
                printIndentLn("// Ignored unrecognized attribute: { u2 #%d; u4 %d; u1[ %s ]; }".
                        formatted(attr.getNameCpx(), attr.getLength(), attr.dataAsString()));
            }
        }

        if (!printCPIndex && value_cpx != UNDEFINED && !pool.CheckEntryType(value_cpx, CONSTANT_INTEGER, CONSTANT_FLOAT,
                CONSTANT_DOUBLE, CONSTANT_LONG, CONSTANT_STRING)) {
            ConstantPool.TAG tag = pool.getTag(value_cpx);
            String tagName = tag == null ? "unknown" : tag.printValue();
            printIndentLn("// ".concat(environment.getInfo("warn.attribute.constantvalue.unrecognized",value_cpx, tagName)));
        }
        // print field
        StringBuilder prefix = new StringBuilder(getIndentString()).
                append(EModifier.asKeywords(access, ClassFileContext.FIELD));
        // add synthetic, deprecated if necessary
        prefix.append(getPseudoFlagsAsString());
        // field
        prefix.append(JasmTokens.Token.FIELDREF.parseKey()).append(' ');
        int keywordPadding = max(prefix.length() - getIndentSize() * 2, SIGNATURE.parseKey().length() + getIndentSize());
        printVar(prefix,
                (value_cpx != UNDEFINED) ? getConstantValue(" = ") : null,
                printSignature ? ":" : ";",
                name_cpx, type_cpx, value_cpx);
        // print Signature if necessary
        if (printSignature) {
            signature.setKeywordPadding(keywordPadding).
                    incIndent().
                    setCommentOffset(this.getCommentOffset() - getIndentStep() * 2);
            signature.print();
        }
    }

    private String getConstantValue(String prefix) {
        if (!printCPIndex &&
                value_cpx != UNDEFINED &&
                !pool.CheckEntryType(value_cpx, CONSTANT_INTEGER, CONSTANT_FLOAT, CONSTANT_DOUBLE, CONSTANT_LONG, CONSTANT_STRING)) {
            return "";
        } else {
            return prefix.concat(data.pool.ConstantStrValue(value_cpx));
        }
    }


    /**
     * Prints the field data to the current output stream called from ClassData.
     */
    @Override
    protected void tablePrint(int index, int size) throws IOException {
        //There are no differences between the simple (jasm) and extended (table) presentations of field info.
        this.jasmPrint(index, size);
    }

    private String getFieldDefinitionString() {
        if (printCPIndex && !skipComments) {
            StringBuilder sb = new StringBuilder(EModifier.asKeywords(access, ClassFileContext.FIELD));
            // add synthetic, deprecated if necessary
            sb.append(getPseudoFlagsAsString());
            // field
            sb.append(JasmTokens.Token.FIELDREF.parseKey()).append(' ');
            sb.append('#').append(name_cpx).append(":#").append(type_cpx);
            if (signature != null && !signature.isPrintable()) {
                sb.append(":#").append(signature.getCPIndex());
            }
            if (value_cpx != UNDEFINED) {
                sb.append(" = #").append(value_cpx);
            }
            sb.append(';');
            return sb.toString();
        }
        return "";
    }

    @Override
    public int getCommentOffset() {
        return Math.max(super.getCommentOffset(), this.maxSize + getIndentSize());
    }

    @Override
    public int getPrintSize() {
        String line = getFieldDefinitionString();
        return line.length();
    }

    @Override
    public void setMaxPrintSize(int size) {
        this.maxSize = size;
    }

    @Override
    public int getMaxPrintSize() {
        return this.maxSize;
    }
} // end FieldData
