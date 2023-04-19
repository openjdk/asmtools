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
package org.openjdk.asmtools.common.outputs.log;

import org.openjdk.asmtools.common.NotImplementedException;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;

/**
 * This is special case of output - for log and trace outputs form compilation, not for bytecode/sources themselves.
 * Historically, asmtools had duals stream logger, where tracing was polluting stdout.
 * This logic is by default off, but can be turned on by secret switch if needed.
 * For application, although logging is still done in two streams, the log is united into stderr via StderrLog.
 *
 * UnitTest and 3rd party applications such as IDE or instrumentation providers s should be using unified StringLog.
 *
 * Once (if ever) the historical dependants on duality of log are removed, the logger should be simple and direct to a single buffer.
 */
public interface DualStreamToolOutput extends ToolOutput {
    void printlne(String line);

    void printe(String line);

    void printe(char line);

    void stacktrace(Throwable ex);

    ToolOutput getSToolObject();

    ToolOutput getEToolObject();

    @Override
    default DataOutputStream getDataOutputStream() throws FileNotFoundException {
        throw new NotImplementedException("Not going to happen");
    }
}
