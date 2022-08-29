/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdec;

import org.openjdk.asmtools.common.Tool;
import org.openjdk.asmtools.common.ToolOutput;
import org.openjdk.asmtools.common.uEscWriter;

import java.io.PrintStream;
import java.io.PrintWriter;

public abstract class JdecTool extends Tool<JdecEnvironment> {

    protected JdecTool(PrintWriter toolOutput, PrintWriter errorOutput, PrintWriter loggerOutput) {
        super(toolOutput, errorOutput, loggerOutput);
    }

    protected JdecTool(PrintWriter toolOutput) {
        super(toolOutput, new PrintWriter(System.err, true), new PrintWriter(System.out, true));
    }

    protected JdecTool(PrintStream toolOutput) {
        this(new PrintWriter(new uEscWriter(toolOutput)));
    }

    @Override
    public JdecEnvironment getEnvironment(ToolOutput toolOutput, PrintWriter errorLogger, PrintWriter outputLogger) {
        JdecEnvironment.JDecBuilder builder = new JdecEnvironment.JDecBuilder(toolOutput, errorLogger, outputLogger);
        return builder.build();
    }
}
