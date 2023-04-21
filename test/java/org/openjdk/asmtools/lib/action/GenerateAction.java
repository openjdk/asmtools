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
package org.openjdk.asmtools.lib.action;

import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.TextOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.lib.LogAndReturn;
import org.openjdk.asmtools.lib.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class GenerateAction {
    private static GenerateAction entry;
    private final File destDir;
    private final List<String> toolArgs = new ArrayList<>();

    public GenerateAction() throws IOException {
        destDir = Files.createTempDirectory("generate").toFile();
        destDir.deleteOnExit();
    }

    public static LogAndReturn JDis(List<String> files, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jdis(files);
    }

    public static LogAndReturn JDec(List<String> files, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jdec(files);
    }

    public void reflectionJdis(List<String> files) {
        reflectionAction("jdis", files);
    }

    public void reflectionJdec(List<String> files) {
        reflectionAction("jdec", files);
    }

    public LogAndReturn jdis(List<String> files) {
        return getLogAndReturn("jdis", files);
    }

    public LogAndReturn jdis(ToolInput... toolInputs) {
        return getLogAndReturn("jdis", toolInputs);
    }

    public LogAndReturn jdec(List<String> files) {
        return getLogAndReturn("jdec", files);
    }

    public LogAndReturn jdec(ToolInput... toolInputs) {
        return getLogAndReturn("jdec", toolInputs);
    }

    public LogAndTextResults getJdecResult(List<String> files) {
        return getLogAndTextResult("jdec", files);
    }

    public LogAndTextResults getJdisResult(List<String> files) {
        return getLogAndTextResult("jdis", files);
    }

    public LogAndTextResults getJdecResult(ToolInput... toolInputs) {
        return getLogAndTextResult("jdec", toolInputs);
    }

    public LogAndTextResults getJdisResult(ToolInput... toolInputs) {
        return getLogAndTextResult("jdis", toolInputs);
    }

    /**
     * Moderator method based on reflection API to call tools
     */
    private void reflectionAction(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
        args.add("-d");
        args.add(destDir.getPath());
        args.addAll(files);
        try {
            String toolClassName = "org.openjdk.asmtools." + toolName + ".Main";
            Class<?> toolClass = Class.forName(toolClassName);
            Constructor<?> constr = toolClass.getConstructor(PrintStream.class, String.class);
            PrintStream ps = new PrintStream(System.out);
            Object tool = constr.newInstance(ps, toolName);
            Method m = toolClass.getMethod("compile", String[].class);
            Object r = m.invoke(tool, new Object[]{args.toArray(new String[0])});
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
    private LogAndReturn getLogAndReturn(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
        args.add("-d");
        args.add(destDir.getPath());
        args.addAll(files);
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jdec")) {
            org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(encodedFiles, encodeLog,
                    args.toArray(new String[0]));
            rc = jdec.decode();
        } else if (toolName.equals("jdis")) {
            org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(encodedFiles, encodeLog,
                    args.toArray(new String[0]));
            rc = jdis.disasm();
        } else {
            fail(new IllegalArgumentException("Unknown tools name: " + toolName));
        }
        return new LogAndReturn(encodeLog, rc);
    }

    /**
     * @return InputOutputTests.LogAndReturn wrapping both a log stream as a string and return code
     */
    private LogAndReturn getLogAndReturn(String toolName, ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("no tool input");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
        args.add("-d");
        args.add(destDir.getPath());
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jdec")) {
            org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(encodedFiles,encodeLog, toolInputs);
            rc = jdec.decode(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else if (toolName.equals("jdis")) {
            org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(encodedFiles,encodeLog, toolInputs);
            rc = jdis.disasm(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else {
            fail(new IllegalArgumentException("Unknown tools name: " + toolName));
        }
        return new LogAndReturn(encodeLog, rc);
    }

    /**
     * @return InputOutputTests.LogAndBinResults wrapping the text result, a log stream as a string and return code
     */
    private LogAndTextResults getLogAndTextResult(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
//        args.add("-d");
//        args.add(destDir.getPath());
        args.addAll(files);
        int rc = 0;
        TextOutput encodedFiles = new TextOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jdec")) {
            org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(encodedFiles,encodeLog,
                    args.toArray(String[]::new));
            rc = jdec.decode();
        } else if (toolName.equals("jdis")) {
            org.openjdk.asmtools.jdis.Main jdis = new org.openjdk.asmtools.jdis.Main(encodedFiles,encodeLog,
                    args.toArray(String[]::new));
            rc = jdis.disasm();
        } else {
            fail(new IllegalArgumentException("Either unknown tools name or the tool doesn't return a text result: " + toolName));
        }
        return new LogAndTextResults(encodedFiles, encodeLog, rc);
    }

    /**
     * @return InputOutputTests.LogAndBinResults wrapping the text result, a log stream as a string and return code
     */
    public LogAndTextResults getLogAndTextResult(String toolName, ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("no tool input");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
//        args.add("-d");
//        args.add(destDir.getPath());
        int rc = 0;
        TextOutput encodedFiles = new TextOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jdec")) {
            org.openjdk.asmtools.jdec.Main jdec = new org.openjdk.asmtools.jdec.Main(encodedFiles,encodeLog, toolInputs);
            rc = jdec.decode(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else if (toolName.equals("jdis")) {
            org.openjdk.asmtools.jdis.Main jdis =
                    new org.openjdk.asmtools.jdis.Main(encodedFiles, encodeLog, toolInputs);
            rc = jdis.disasm(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else {
            fail(new IllegalArgumentException("Either unknown tools name or the tool doesn't return a text result: " + toolName));
        }
        return new LogAndTextResults(encodedFiles, encodeLog, rc);
    }


    public GenerateAction setToolArgs(String... args) {
        if (args != null && args.length > 0) {
            Collections.addAll(this.toolArgs, args);
        } else {
            this.toolArgs.clear();
        }
        return this;
    }

    private static GenerateAction getEntry() {
        if (GenerateAction.entry == null) {
            try {
                entry = new GenerateAction();
            } catch (IOException e) {
                fail(e.toString());
                throw new RuntimeException(e); // // appeasing the compiler: this line will never be executed.
            }
        }
        return entry;
    }
}
