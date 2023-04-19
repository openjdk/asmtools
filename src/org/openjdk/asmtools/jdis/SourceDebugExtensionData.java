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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import static org.openjdk.asmtools.asmutils.StringUtils.readUtf8String;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.SOURCEDEBUGEXTENSION;

/**
 * The SourceDebugExtension attribute
 * <p>
 * SourceDebugExtension_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u1 debug_extension[attribute_length];
 * }
 */
public class SourceDebugExtensionData extends Indenter {

    List<String> debug_extension;

    public SourceDebugExtensionData(ClassData classData) {
        super(classData.toolOutput);
    }

    public SourceDebugExtensionData read(DataInputStream in, int attribute_length) throws IOException, ClassFormatError {
        debug_extension = readUtf8String(in, attribute_length, 76 - getIndentSize());
        return this;
    }

    @Override
    public void print() {
        printIndentLn(SOURCEDEBUGEXTENSION.parseKey() + " {");
        incIndent();
        debug_extension.forEach(s -> printIndentLn(s));
        decIndent();
        printIndentLn("}");
    }
}
