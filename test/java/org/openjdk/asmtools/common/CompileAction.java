/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.InputOutputTests;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class CompileAction {

    private final File destDir;

    public CompileAction() throws IOException {
        destDir = Files.createTempDirectory("compile").toFile();
        destDir.deleteOnExit();
    }

    public CompileAction(File destDir) {
        this.destDir = destDir;
    }

    public void reflectionJasm(List<String> files) {
        reflectionAction("jasm", files);
    }

    public void reflectionJcoder(List<String> files) {
        reflectionAction("jcoder", files);
    }

    public InputOutputTests.LogAndReturn jasm(List<String> files) {
        return action("jasm", files);
    }

    public InputOutputTests.LogAndReturn jcoder(List<String> files) {
        return action("jcoder", files);
    }

    /**
     * Moderator method based on reflection API to call tools
     */
    private void reflectionAction(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> toolArgs = new ArrayList<>();
        toolArgs.add("-d");
        toolArgs.add(destDir.getPath());
        toolArgs.addAll(files);
        try {
            String toolClassName = "org.openjdk.asmtools." + toolName + ".Main";
            Class<?> toolClass = Class.forName(toolClassName);
            Constructor<?> constr = toolClass.getConstructor(PrintStream.class, String.class);
            PrintStream ps = new PrintStream(System.out);
            Object tool = constr.newInstance(ps, toolName);
            Method m = toolClass.getMethod("compile", String[].class);
            Object r = m.invoke(tool, new Object[]{toolArgs.toArray(new String[0])});
            if (r instanceof Boolean) {
                boolean ok = (Boolean) r;
                if (!ok) {
                    fail(toolName + " failed");
                }
                System.out.println(toolName + " OK");
            } else
                fail("unexpected result from " + toolName + ": " + r.toString());
        } catch (ClassNotFoundException e) {
            fail("can't find " + toolName);
        } catch (ReflectiveOperationException t) {
            fail("error invoking " + toolName + ": " + t);
        }
    }

    /**
     * @return InputOutputTests.LogAndReturn wrapping both a log stream as a string and return code
     */
    private InputOutputTests.LogAndReturn action(String toolName, List<String> files) {
        int rc = 0;
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> toolArgs = new ArrayList<>();
        toolArgs.add("-d");
        toolArgs.add(destDir.getPath());
        toolArgs.addAll(files);
        ToolOutput.ByteOutput encodedFiles = new ToolOutput.ByteOutput();
        ToolOutput.StringLog encodeLog = new ToolOutput.StringLog();
        if( toolName.equals("jcoder") ) {
            org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, toolArgs.toArray(new String[0]));
            rc = jcod.compile();
        } else if (toolName.equals("jasm")) {
            org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog, toolArgs.toArray(new String[0]));
            rc = jasm.compile();
        } else {
            fail(new IllegalArgumentException("Unknown tools name: " + toolName));
        }
        return new InputOutputTests.LogAndReturn(encodeLog, rc);
    }

}
