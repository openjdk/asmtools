/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static org.openjdk.asmtools.asmutils.HexUtils.toHex;

public class AttrData {

    private final int MAX_DATA_PRINT_SIZE = 10;

    private EAttribute attributeInfo;
    //
    private int name_cpx;

    private int length;
    private byte[] data;
    final Environment environment;

    public AttrData(Environment environment, EAttribute attributeInfo) {
        this.environment = environment;
        this.attributeInfo = attributeInfo;
    }

    public void read(int name_cpx, int attrLength, DataInputStream in) throws IOException {
        this.name_cpx = name_cpx;
        this.length = attrLength;
        try {
            data = new byte[attrLength];
            in.readFully(data);
            environment.traceln("AttrData:#%d length=%d", name_cpx, attrLength);
        } catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException ex) {
            throw new FormatError(environment.getLogger(),
                    "err.invalid.attribute.length", attributeInfo.printValue(), attrLength);
        }
    }

    public String dataAsString() {
        if (data != null && data.length > 0) {
            int maxLength = min( MAX_DATA_PRINT_SIZE, data.length );
            String res = IntStream.range(0, maxLength).mapToObj(i -> HexUtils.toHex(i)).collect(Collectors.joining(", "));
            if (data.length > MAX_DATA_PRINT_SIZE) {
                res += ", ...";
            }
            return res;
        }
        return "";
    }

    /**
     * ATTRIBUTE_NAME_attribute {
     * u2 attribute_name_index;
     * u4 attribute_length;
     * ...
     * }
     *
     * @param name_cpx set the ConstantPool index of the attribute name
     * @return the current instance
     */
    public AttrData setNameCpx(int name_cpx) {
        this.name_cpx = name_cpx;
        return this;
    }

    public AttrData setLength(int length) {
        this.length = length;
        return this;
    }

    /**
     * ATTRIBUTE_NAME_attribute {
     * u2 attribute_name_index;
     * u4 attribute_length;
     * ...
     * }
     *
     * @return attribute_name_index
     */
    public int getNameCpx() {
        return name_cpx;
    }

    public int getLength() {
        return length;
    }

    public EAttribute getAttributeInfo() {
        return attributeInfo;
    }
}
