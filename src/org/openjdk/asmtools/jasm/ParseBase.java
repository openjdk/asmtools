/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;



/**
 * Base helper class for a Parser.
 */
public class ParseBase {

    // debug flag
    protected boolean debugFlag;
    protected Scanner scanner;
    protected Parser parser;
    protected JasmEnvironment environment;

    public ParseBase() {
    }

    public void init(Parser parentParser) {
        this.environment = parentParser.environment;
        this.scanner = parentParser.scanner;
        this.parser = parentParser;
    }

    public void init(JasmEnvironment environment, Parser parser) {
        this.environment = environment;
        this.scanner = new Scanner(environment);
        this.parser = parser;
    }

    public void init(JasmEnvironment environment) {
        this.environment = environment;
    }

    public void setDebugFlag(boolean value) {
        debugFlag = value;
    }

    protected void traceMethodInfoLn() {
        traceMethodInfoLn(null);
    }

    protected void traceMethodInfoLn(String str) {
        if (debugFlag) {
            StackTraceElement elem = Thread.currentThread().getStackTrace()[str == null ? 3 : 2];
            String msg = String.format("%s::%s[%d]%s", elem.getClassName().substring(elem.getClassName().lastIndexOf('.') + 1),
                    elem.getMethodName(), elem.getLineNumber(), str == null ? "" : " " + str);
            environment.traceln(msg);
        }
    }
}
