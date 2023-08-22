/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * The Signature attribute data
 * <p>
 * since class file 49.0
 */
public class SignatureData extends MemberData<ClassData> {

    private int index;

    public SignatureData(ClassData classData) {
        super(classData);
    }

    public SignatureData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        if (attribute_length != 2) {
            throw new FormatError(environment.getLogger(),
                    "err.invalid.attribute.length", EAttribute.ATT_Signature.printValue(), attribute_length);
        }
        index = in.readUnsignedShort();
        return this;
    }

    @Override
    public String toString() {
        return format("signature[%d]=%s", getIndex(), pool.StringValue(getIndex()));
    }

    public int getIndex() {
        return index;
    }

    public String asString() {
        return pool.StringValue(index);
    }

    /**
     * @param checkRange function to check that index belongs CP
     * @return string presentation of index and signature used to print
     * ClassFile, field_info, method_info, or record_component_info
     */
    public Pair<String, String> getPrintInfo(Function<Integer, Boolean> checkRange) {
        return new Pair<>(format(":#%d", index),
                checkRange.apply(index) ? format(":%s", pool.StringValue(index)) : ":?? invalid index");
    }
}
