/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.SOURCEFILE;

/**
 * SourceFile_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 sourcefile_index;
 * }
 */
public class SourceFileData extends Indenter {
    private ConstantPool pool;
    // Constant Pool index to a file reference to the Java source
    private int source_cpx = 0;
    private String sourceName = null;

    public SourceFileData(ClassData classData) {
        super(classData.toolOutput);
        pool = classData.pool;
    }

    public SourceFileData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        this.source_cpx = in.readUnsignedShort();
        return this;
    }

    @Override
    public boolean isPrintable() {
        return getSourceName() != null;
    }

    @Override
    public void print() {
        if (printCPIndex) {
            if(skipComments ) {
                printIndentLn(format("%s #%d;", SOURCEFILE.parseKey(), source_cpx));
            } else {
                printIndent(PadRight(format("%s #%d;", SOURCEFILE.parseKey(), source_cpx), getCommentOffset() - 1)).
                        println((sourceName != null) ? " // " + sourceName : "");
            }
        } else {
            printIndent(PadRight(SOURCEFILE.parseKey(),OPERAND_PLACEHOLDER_LENGTH + INSTR_PREFIX_LENGTH + 1)).
                    println( "\"" + (sourceName != null ? sourceName : "???") + "\";");
        }
    }

    public String getSourceName() {
        if( sourceName == null ) {
            this.sourceName = pool.getString(source_cpx, index -> null);
        }
        return this.sourceName;
    }

    public SourceFileData setSourceName() {
        this.sourceName = pool.getString(source_cpx, index -> "#" + index);
        return this;
    }
}
