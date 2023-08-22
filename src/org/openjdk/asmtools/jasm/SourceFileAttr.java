/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.function.Function;

/**
 * SourceFile_attribute {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u2 sourcefile_index;
 * }
 */
public class SourceFileAttr extends AttrData {
    private String    sourceFileName;
    private ConstCell sourceFileNameCell = null;

    /**
     * Constructs SourceFile Attribute without creating Constant Pool UTF8 SourceFile
     *
     * @param pool constant pool of parsed class data
     * @param sourceFileName  a name of a parsed file
     */
    public SourceFileAttr(ConstantPool pool, String sourceFileName) {
        super(pool, EAttribute.ATT_SourceFile);
        this.sourceFileName = sourceFileName;
    }

    /**
     * Constructs SourceFile Attribute without creating Constant Pool UTF8 SourceFile
     *
     * @param pool constant pool of parsed class data
     * @param sourceFileNameCell  ConstantPool UTF8 cell
     */
    public SourceFileAttr(ConstantPool pool, ConstCell sourceFileNameCell) {
        super(pool, EAttribute.ATT_SourceFile);
        this.sourceFileNameCell = sourceFileNameCell;
    }


    /**
     * Finds CP UTF cell with string that applies to the rule.
     * If such UTF8 string exists then replaces it with a new source file name
     * otherwise creates a new UTF8 cell to fill out Source File Attribute
     */
    public SourceFileAttr updateIfFound(ConstantPool pool, Function<String, Boolean> rule) {
        sourceFileNameCell = pool.lookupUTF8Cell(rule);
        if( sourceFileNameCell != null ) {
            sourceFileNameCell.ref.value = sourceFileName;
        } else {
            sourceFileNameCell = pool.findUTF8Cell(this.sourceFileName);
        }
        return this;
    }

    @Override
    public int attrLength() { return 2; }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        if( sourceFileNameCell != null ) {
            super.write(out);  // attribute name & length
            out.writeShort(sourceFileNameCell.cpIndex);
        }
    }
}
