/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.lib.action;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.fail;

public class Action {

    private final EAsmTool tool;
    protected final String toolName;
    private FSAction fsAction = new FSAction();

    protected EToolArguments currentToolArgs;

    public Action(EAsmTool tool) {
        this.tool = tool;
        toolName = tool.getName();
        currentToolArgs = EToolArguments.getArgumentsByPriority(tool, 0);
    }

    public Action(EAsmTool tool, Path destDir) {
        this(tool);
        this.fsAction = new FSAction().setupDestDir(destDir);
    }

    public Action setArgs(EToolArguments args) {
        if (args.tool != this.tool) {
            fail("Arguments mismatch for the tool " + args.tool);
        }
        currentToolArgs = args;
        return this;
    }

    public final List<EToolArguments> getListOfToolArgs() {
        return EToolArguments.ofTool(tool);
    }

    /**
     * Sets up the output directory.
     *
     * @param destDir the directory to be used for output
     * @return the instance
     */
    public Action setDestDir(Path destDir) {
        fsAction.setupDestDir(destDir);
        return this;
    }

    /**
     * Sets the type of output, either to the file system or a memory buffer.
     *
     * @param value true if the output destination is the file system; otherwise, it is a memory buffer.
     * @return the instance
     */
    public Action FSOtput(boolean value) {
        if (value) {
            fsAction.setupDestDir(null);
        } else {
            fsAction.createDestDir();
        }
        return this;
    }

    public boolean isFSOutput() {
        return fsAction.getDestDir() != null;
    }

    /**
     * Sets up the temporary output directory that will be created
     *
     * @return the instance
     */
    public Action setDestDir() {
        fsAction.createDestDir();
        return this;
    }

    public List<String> getDestDirParams() {
        return fsAction.getDestDirParams();
    }


    public String getDestDir() {
        return fsAction.getDestDir().toString();
    }

    public Action setDebug(boolean value) {
        DebugHelper.setDebug(value);
        return this;
    }

    public void trace(Supplier<String> info) {
        DebugHelper.trace(info);
    }

}
