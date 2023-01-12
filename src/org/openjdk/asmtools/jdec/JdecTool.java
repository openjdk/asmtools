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
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.EscapedPrintStreamOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;

import java.io.PrintStream;

public abstract class JdecTool extends Tool<JdecEnvironment> {

    protected JdecTool(ToolOutput toolOutput, DualStreamToolOutput log) {
        super(toolOutput, log);
    }

    protected JdecTool(ToolOutput toolOutput) {
        super(toolOutput, new StderrLog());
    }

    protected JdecTool(PrintStream toolOutput) {
        this(new EscapedPrintStreamOutput(toolOutput));
    }

    @Override
    public JdecEnvironment getEnvironment(ToolOutput toolOutput, DualStreamToolOutput log) {
        JdecEnvironment.JDecBuilder builder = new JdecEnvironment.JDecBuilder(toolOutput, log);
        return builder.build();
    }
}
