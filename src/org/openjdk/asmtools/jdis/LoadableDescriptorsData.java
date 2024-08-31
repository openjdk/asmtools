/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.LOADABLE_DESCRIPTORS;

/**
 * The LodableDescriptors attribute (JEP 401)
 * <pre>
 * LoadableDescriptors_attribute {
 *   u2 attribute_name_index;
 *   u4 attribute_length;
 *   u2 number_of_descriptors;
 *   u2 descriptors[number_of_descriptors];
 * }
 * </pre>
 */
public class LoadableDescriptorsData extends MemberData {
    int[] descriptorIndexes;

    protected LoadableDescriptorsData(ClassData classData) {
        super(classData);
    }

    public LoadableDescriptorsData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        int number_of_descriptors = in.readUnsignedShort();
        if (attribute_length != 2 + number_of_descriptors * 2) {
            throw new ClassFormatError("ATT_LoadableDescriptors: Invalid attribute length");
        }
        descriptorIndexes = new int[number_of_descriptors];
        for (int i = 0; i < number_of_descriptors; i++) {
            descriptorIndexes[i] = in.readUnsignedShort();
        }
        return this;
    }

    @Override
    public void print() {
        StringBuilder indexes = new StringBuilder();
        StringBuilder descriptors = new StringBuilder();
        for (int descriptorIndex : descriptorIndexes) {
            if (printCPIndex) {
                indexes.append((indexes.length() == 0) ? "" : ", ").append("#").append(descriptorIndex);
            }
            descriptors.append((descriptors.length() == 0) ? "" : ", ").append(pool.StringValue(descriptorIndex));
        }
        if (printCPIndex) {
            if( skipComments ) {
                printIndentLn("%s %s;", LOADABLE_DESCRIPTORS.parseKey(), indexes);
            }  else {
                printIndent(PadRight(format("%s %s;", LOADABLE_DESCRIPTORS.parseKey(), indexes), getCommentOffset() - 1)).println(" // " + descriptors);
            }
        } else {
            printIndentLn("%s %s;", LOADABLE_DESCRIPTORS.parseKey(), descriptors.toString());
        }
    }
}
