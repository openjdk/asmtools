/*
 * Copyright (c) 2023, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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

package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This class is a generic interface, symbolising any output from jdis/jasm/jdec/jcoder.
 * Asmtools as application internally uses DirOutput and StdoutOutput (via EscapedPrintStreamOutput).
 * UnitTests for asmtools uses mainly ByteOutput for assemblers  and TextOutput for disasemblers.
 *
 * Text/Byte/EscapedPrintStream outputs can be used as any 3rd part code which do not need files, aka IDE, instrumetations or similar.
 *
 * The interface methods goes in favor of asmtools, and for details and help see individual implementations
 */
public interface ToolOutput {

    DataOutputStream getDataOutputStream() throws FileNotFoundException;

    String getCurrentClassName();

    void startClass(String fullyQualifiedName, Optional<String> suffix, Environment logger) throws IOException;

    void finishClass(String fullyQualifiedName) throws IOException;

    void printlns(String line);

    void prints(String line);

    void prints(char line);

    void flush();


    public static String exToString(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}

