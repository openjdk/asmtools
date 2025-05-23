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
package org.openjdk.asmtools.common.inputs;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Base class for JasmEnvironment.InputFile and
 * JcoderEnvironment.InputFile that read the jasm, jcod text files
 */
public abstract class TextInput {

    public long position;
    protected int charPos = 0;
    protected int linepos = 1;


    // Buffer to keep the text file content
    protected final String strData;

    public TextInput(DataInputStream dataInputStream) throws IOException {
        byte[] data = new byte[dataInputStream.available()];
        dataInputStream.read(data);
        strData = new String(data, StandardCharsets.UTF_8);
        dataInputStream.close();
    }

    public abstract int readUTF();
}
