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

import org.openjdk.asmtools.common.DecompilerLogger;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import static java.lang.String.format;

public class JdecEnvironment extends Environment<DecompilerLogger> {

    protected boolean printDetailsFlag;

    // Output stream or files or custom Strings
    private final ToolOutput toolOutput;

    /**
     * @param builder the jdec environment builder
     */
    private JdecEnvironment(Builder<JdecEnvironment, DecompilerLogger> builder) {
        super(builder);
        this.toolOutput = builder.toolOutput;
    }

    @Override
    public void printErrorLn(String format, Object... args) {
        getLogger().printErrorLn(format, args);
    }

    public Environment setPrintDetailsFlag(boolean value) {
        this.printDetailsFlag = value;
        return this;
    }

    @Override
    public void println(String format, Object... args) {
        getToolOutput().printlns((args == null || args.length == 0) ? format : format(format, args));
    }

    @Override
    public void println() {
        getToolOutput().printlns("");
    }

    @Override
    public void print(String format, Object... args) {
        getToolOutput().prints((args == null || args.length == 0) ? format : format(format, args));
    }

    @Override
    public void print(char ch) {
        getToolOutput().prints(ch);
    }

    static class JDecBuilder extends Builder<JdecEnvironment, DecompilerLogger> {

        public JDecBuilder(ToolOutput toolOutput, DualStreamToolOutput log) {
            super(toolOutput, new DecompilerLogger("jdec", JdecEnvironment.class, log));
        }

        @Override
        public JdecEnvironment build() {
            return new JdecEnvironment(this);
        }
    }
}
