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

import static org.openjdk.asmtools.jasm.JasmTokens.Token.LOCALVARIABLETYPES_HEADER;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.SIGNATURE;

/**
 * LocalVariableTypeTable_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 local_variable_type_table_length;
 * {   u2 start_pc;
 * u2 length;
 * u2 name_index;
 * u2 signature_index;
 * u2 index;
 * } local_variable_type_table[local_variable_type_table_length];
 * }
 */
public class LocalVariableTypeData<M extends MemberData<?>> extends LocalData {

    public boolean isPrintable() {
        return printLocalVariableTypes && tableFormat;
    }

    public LocalVariableTypeData(M owner, DataInputStream in, MethodData methodData) throws IOException {
        super(owner, in, methodData, SIGNATURE.parseKey());
    }

    @Override
    protected String getTitle() {
        return LOCALVARIABLETYPES_HEADER.parseKey() + ":";
    }
}
