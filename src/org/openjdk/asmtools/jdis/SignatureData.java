/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jdis.notations.Signature;
import org.openjdk.asmtools.jdis.notations.Type;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.SIGNATURE;

/**
 * The Signature attribute data since class file 49.0
 * <p>
 * Signature_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 signature_index;
 * }
 */
public class SignatureData extends MemberData<ClassData> {
    private int cpIndex;
    private Type signatureType = null;
    private int keywordPadding = -1;

    // in some cases, the new line should not end the printed signature
    private String eol = System.getProperty("line.separator");

    public SignatureData(ClassData classData) {
        super(classData);
        this.tableToken = SIGNATURE;
    }

    public int getCPIndex() {
        return cpIndex;
    }

    @Override
    public boolean isPrintable() {
        return !dropSignatures;
    }

    public SignatureData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        if (attribute_length != 2) {
            if (bestEffort) {
                environment.getLogger().error(
                        "err.invalid.attribute.length", EAttribute.ATT_Signature.printValue(), attribute_length);
            } else {
                throw new FormatError(environment.getLogger(),
                        "err.invalid.attribute.length", EAttribute.ATT_Signature.printValue(), attribute_length);
            }
        }
        cpIndex = in.readUnsignedShort();
        return this;
    }

    @Override
    public String toString() {
        return format("signature[%d]=%s", cpIndex, pool.StringValue(cpIndex));
    }

    public String getJavaSignature() {
        if (signatureType == null) {
            signatureType = new Signature(environment.getLogger(), cpIndex).getType(pool);
        }
        return signatureType != null ?
                signatureType.toString().replace('/', '.') :
                pool.StringValue(cpIndex);
    }

    public Type getSignatureType() {
        if (signatureType == null) {
            signatureType = new Signature(environment.getLogger(), cpIndex).getType(pool);
        }
        return signatureType;
    }

    public SignatureData setKeywordPadding(int keywordPadding) {
        this.keywordPadding = keywordPadding;
        return this;
    }

    @Override
    protected int getPrintAttributeKeyPadding() {
        return keywordPadding == -1 ? super.getPrintAttributeKeyPadding() : keywordPadding;
    }

    public SignatureData disableNewLine() {
        this.eol = "";
        return this;
    }

    public SignatureData enableNewLine() {
        this.eol = System.getProperty("line.separator");
        return this;
    }

    @Override
    protected void tablePrint() {
        printIndent(PadRight(tableToken.printKey(), getPrintAttributeKeyPadding()));
        String sign = pool.StringValue(cpIndex);
        if (printCPIndex) {
            if (skipComments) {
                print("#%d;".formatted(cpIndex).concat(eol));
            } else {
                print(PadRight("#%d;".formatted(cpIndex), getPrintAttributeCommentPadding())).
                        print(" // ".concat(sign).concat(eol));
            }
        } else {
            print(sign.concat(";").concat(eol));
        }
    }

    @Override
    protected void jasmPrint() {
        printIndent(PadRight(SIGNATURE.parseKey(), getPrintAttributeKeyPadding()));
        String sign = pool.StringValue(cpIndex);
        if (printCPIndex) {
            if (skipComments) {
                print("#%d;".formatted(cpIndex).concat(eol));
            } else {
                print(PadRight("#%d;".formatted(cpIndex), getPrintAttributeCommentPadding())).
                        print(" // ".concat(sign).concat(eol));
            }
        } else {
            print(sign.concat(";").concat(eol));
        }
    }

    /**
     * @param checkRange function to check that index belongs CP
     * @return a string representation of the index and signature used to print the JASM-specific signature
     * of ClassFile, field_info, method_info, or record_component_info.
     * Alternatively, return an empty pair (new Pair("", "")) if extended (table-specific) printing is requested.
     */
    public Pair<String, String> getJasmPrintInfo(Function<Integer, Boolean> checkRange) {
        return (tableFormat) ?
                new Pair<>("", "") :
                new Pair<>(format(":#%d", cpIndex),
                        checkRange.apply(cpIndex) ? ":" + pool.StringValue(cpIndex) :
                                ":?%d Invalid constant_pool index".formatted(cpIndex));
    }
}
