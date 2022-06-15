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
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.PrintWriter;

public class JdecEnvironment extends Environment<DecompilerLogger> {

    protected boolean printDetailsFlag;

    // Output stream
    private final PrintWriter toolOutput;

    private JdecEnvironment(Builder<JdecEnvironment, DecompilerLogger> builder, I18NResourceBundle i18n) {
        super(builder, i18n);
        this.toolOutput = builder.toolOutput;
    }

    @Override
    public void printErrorLn(String format, Object... args) {
        getLogger().printErrorLn(format, args);
    }

    public PrintWriter getToolOutput() {
        return toolOutput;
    }

    public Environment setPrintDetailsFlag(boolean value) {
        this.printDetailsFlag = value;
        return this;
    }

    @Override
    public void println(String format, Object... args) {
        super.println(format, args);
    }

    @Override
    public void println() {
        super.println();
    }

    @Override
    public void print(String format, Object... args) {
        super.print(format, args);
    }


    static class JDecBuilder extends Builder<JdecEnvironment, DecompilerLogger> {

        public JDecBuilder(PrintWriter toolOutput, PrintWriter errorLogger, PrintWriter outputLogger) {
            super("jdec", toolOutput, new DecompilerLogger(errorLogger, outputLogger));
        }

        @Override
        public JdecEnvironment build() {
            return new JdecEnvironment(this, I18NResourceBundle.getBundleForClass(this.getClass()));
        }
    }
}
