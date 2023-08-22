/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Base class of the "classes[]" data of attributes
 * <p>
 * JEP 181 (Nest-based Access Control): class file 55.0
 * NestMembers_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * <p>
 * JEP 360 (Sealed types): class file 59.65535
 * PermittedSubclasses_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes;
 * u2 classes[number_of_classes];
 * }
 * </p>
 */
public class ClassArrayData extends MemberData {
    String name;
    int[] classIndexes;

    protected ClassArrayData(ClassData classData, String attrName) {
        super(classData);
        this.name = attrName;
    }

    public ClassArrayData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        int number_of_classes = in.readUnsignedShort();
        if (attribute_length != 2 + number_of_classes * 2) {
            throw new ClassFormatError(name + "_attribute: Invalid attribute length");
        }
        classIndexes = new int[number_of_classes];
        for (int i = 0; i < number_of_classes; i++) {
            classIndexes[i] = in.readUnsignedShort();
        }
        return this;
    }

    @Override
    public void print() {
        StringBuilder indexes = new StringBuilder();
        StringBuilder names = new StringBuilder();
        for (int classIndex : classIndexes) {
            if (printCPIndex) {
                indexes.append((indexes.length() == 0) ? "" : ", ").append("#").append(classIndex);
            }
            names.append((names.length() == 0) ? "" : ", ").append(pool.StringValue(classIndex));
        }
        if (printCPIndex) {
            if( skipComments ) {
                printIndentLn("%s %s;", name, indexes);
            }  else {
                printIndent(PadRight(format("%s %s;", name, indexes), getCommentOffset() - 1)).println(" // " + names);
            }
        } else {
            printIndentLn("%s %s;", name, names.toString());
        }
    }
}
