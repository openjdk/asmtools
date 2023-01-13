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

import org.openjdk.asmtools.common.NotImplementedException;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class PrintWriterOutput extends NamedToolOutput {

    protected PrintWriter os;

    public PrintWriterOutput(OutputStream os) {
        //although it is usually System.out, it is set from Environment, or custom
        this.os = new PrintWriter(os, true);
    }

    public PrintWriterOutput(Writer os) {
        //although it is usually System.out, it is set from Environment, or custom
        this.os = new PrintWriter(os, true);
    }

    @Override
    public void printlns(String line) {
        os.println(line);
    }

    @Override
    public void prints(String line) {
        os.print(line);
    }

    @Override
    public void prints(char line) {
        os.print(line);
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        super.finishClass(fqn);
        os.flush();
    }

    @Override
    public void flush() {
        os.flush();
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        throw new NotImplementedException("Use EscapedPrintStreamOutput");
    }


}
