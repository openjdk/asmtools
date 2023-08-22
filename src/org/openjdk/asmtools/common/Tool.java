/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common;


import org.openjdk.asmtools.common.inputs.StdinInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.DirOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.File;
import java.util.ArrayList;

public abstract class Tool<T extends Environment<? extends ToolLogger>> {

    protected final ArrayList<ToolInput> fileList = new ArrayList<>();
    protected T environment;

    protected Tool(ToolOutput toolOutput, DualStreamToolOutput outerLog) {
        this.environment = getEnvironment(toolOutput, outerLog);
    }

    public Environment<?> setVerboseFlag(boolean value) {
        environment.setVerboseFlag(value);
        return environment;
    }

    public Environment<?> setTraceFlag(boolean value) {
        environment.setTraceFlag(value);
        return environment;
    }

    public T getEnvironment() {
        return environment;
    }

    // Build environment
    public T getEnvironment(ToolOutput toolOutput, DualStreamToolOutput outerLog) {
        throw new NotImplementedException();
    }

    // Usage
    protected abstract void usage();

    // Parse arguments. Tool will be left using System.Exit if error found.
    protected abstract void parseArgs(String... argv);

    protected void setDestDir(int index, String... argv) {
        File destDir;
        if ((index) >= argv.length) {
            environment.error("err.d_requires_argument");
            usage();
            throw new IllegalArgumentException();
        }
        destDir = new File(argv[index]);
        if (!destDir.exists()) {
            environment.error("err.does_not_exist", destDir.getPath());
            throw new IllegalArgumentException();
        }
        environment.setToolOutput(new DirOutput(destDir));
    }

    protected void addStdIn() {
        for (ToolInput toolInput: fileList) {
            if (toolInput instanceof StdinInput) {
                //or throw?
                return;
            }
        }
        fileList.add(new StdinInput());
    }
}
