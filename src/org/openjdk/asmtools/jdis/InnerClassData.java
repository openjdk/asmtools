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

import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;

/**
 * 4.7.6. The InnerClasses Attribute
 */
class InnerClassData extends Indenter {

    ClassData cls;
    int inner_class_info_index;
    int outer_class_info_index;
    int inner_name_index;
    int access;

    public InnerClassData(ClassData cls) {
        super(cls.toolOutput);
        this.cls = cls;
    }

    public void read(DataInputStream in) throws IOException {
        inner_class_info_index = in.readUnsignedShort();
        outer_class_info_index = in.readUnsignedShort();
        inner_name_index = in.readUnsignedShort();
        access = in.readUnsignedShort();
    }  // end read

    @Override
    public void print() throws IOException {
        String prefix = EModifier.asKeywords(access, ClassFileContext.INNER_CLASS).
                concat(JasmTokens.Token.INNERCLASS.parseKey()).concat(" ");
        if (printCPIndex) {
            if (inner_name_index != 0)
                prefix = prefix.concat("#" + inner_name_index + " = ");
            if (inner_class_info_index != 0)
                prefix = prefix.concat("#" + inner_class_info_index);
            if (outer_class_info_index != 0) {
                prefix = prefix.concat(" of #" + outer_class_info_index);
            }
            prefix = prefix.concat(";");
            if( skipComments ) {
                printIndent(prefix);
            } else {
                printIndentPadRight(prefix, getCommentOffset() - 1).print(" // ");
            }
        } else {
            printIndent(prefix);
        }
        if( !printCPIndex || (printCPIndex && !skipComments) ) {
            if (inner_name_index != 0)
                print(cls.pool.getName(inner_name_index) + " = ");
            if (inner_class_info_index != 0)
                print(cls.pool.ConstantStrValue(inner_class_info_index));
            if (outer_class_info_index != 0)
                print(format(" of %s", cls.pool.ConstantStrValue(outer_class_info_index)));
            println(cls.printCPIndex && !skipComments ? "" : ";");
        } else {
            println();
        }
    }
} // end InnerClassData
