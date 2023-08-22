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
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.lib.LogAndBinResults;
import org.openjdk.asmtools.lib.LogAndReturn;

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

public class CompileAction {
    private static CompileAction entry;
    private final File destDir;
    private List<String> toolArgs = new ArrayList<>();

    public CompileAction() throws IOException {
        destDir = Files.createTempDirectory("compile").toFile();
        destDir.deleteOnExit();
    }

    public static LogAndReturn JAsm(List<String> files, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jasm(files);
    }

    public static LogAndReturn JCoder(List<String> files, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jcoder(files);
    }

    public static LogAndReturn JAsm(ToolInput[] inputs, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jasm(inputs);
    }

    public static LogAndReturn JCoder(ToolInput[] inputs, String... args) {
        return getEntry().setToolArgs().setToolArgs(args).jcoder(inputs);
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

    public LogAndReturn jasm(List<String> files) {
        return getLogAndReturn("jasm", files);
    }

    public LogAndReturn jcoder(List<String> files) {
        return getLogAndReturn("jcoder", files);
    }

    public LogAndReturn jasm(ToolInput... toolInputs) {
        return getLogAndReturn("jasm", toolInputs);
    }

    public LogAndReturn jcoder(ToolInput... toolInputs) {
        return getLogAndReturn("jcoder", toolInputs);
    }


    public LogAndBinResults getJasmResult(List<String> files) {
        return getLogAndBinResults("jasm", files);
    }

    public LogAndBinResults getJcoderResult(List<String> files) {
        return getLogAndBinResults("jcoder", files);
    }

    public LogAndBinResults getJasmResult(ToolInput... toolInputs) {
        return getLogAndBinResults("jasm", toolInputs);
    }

    public LogAndBinResults getJcoderResult(ToolInput... toolInputs) {
        return getLogAndBinResults("jcoder", toolInputs);
    }

    /**
     * Moderator method based on reflection API to call tools
     */
    public void reflectionAction(String toolName, List<String> files) {
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
            Object r = m.invoke(tool, new Object[]{args.toArray(String[]::new)});
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
    public LogAndReturn getLogAndReturn(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
        args.add("-d");
        args.add(destDir.getPath());
        args.addAll(files);
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jcoder")) {
            org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog,
                    args.toArray(String[]::new));
            rc = jcod.compile();
        } else if (toolName.equals("jasm")) {
            org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog,
                    args.toArray(String[]::new));
            rc = jasm.compile();
        } else {
            fail(new IllegalArgumentException("Unknown tools name: " + toolName));
        }
        return new LogAndReturn(encodeLog, rc);
    }

    /**
     * @return InputOutputTests.LogAndReturn wrapping both a log stream as a string and return code
     */
    public LogAndReturn getLogAndReturn(String toolName, ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("no tool input");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
        args.add("-d");
        args.add(destDir.getPath());
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jcoder")) {
            org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, toolInputs);
            rc = jcod.compile(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else if (toolName.equals("jasm")) {
            org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog, toolInputs);
            rc = jasm.compile(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else {
            fail(new IllegalArgumentException("Unknown tools name: " + toolName));
        }
        return new LogAndReturn(encodeLog, rc);
    }

    /**
     * @return InputOutputTests.LogAndBinResults wrapping the binary result, a log stream as a string and return code
     */
    private LogAndBinResults getLogAndBinResults(String toolName, ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("no tool input");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
//        args.add("-d");
//        args.add(destDir.getPath());
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jcoder")) {
            org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog, toolInputs);
            rc = jcod.compile(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else if (toolName.equals("jasm")) {
            org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog, toolInputs);
            rc = jasm.compile(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        } else {
            fail(new IllegalArgumentException("Either unknown tools name or tool doesn't return a binary result: " + toolName));
        }
        return new LogAndBinResults(encodedFiles, encodeLog, rc);
    }


    /**
     * @return InputOutputTests.LogAndBinResults wrapping the binary result, a log stream as a string and return code
     */
    private LogAndBinResults getLogAndBinResults(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> args = toolArgs.stream().collect(Collectors.toList());
//        args.add("-d");
//        args.add(destDir.getPath());
        args.addAll(files);
        int rc = 0;
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        if (toolName.equals("jcoder")) {
            org.openjdk.asmtools.jcoder.Main jcod = new org.openjdk.asmtools.jcoder.Main(encodedFiles, encodeLog,
                    args.toArray(String[]::new));
            rc = jcod.compile();
        } else if (toolName.equals("jasm")) {
            org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(encodedFiles, encodeLog,
                    args.toArray(String[]::new));
            rc = jasm.compile();
        } else {
            fail(new IllegalArgumentException("Either unknown tools name or tool doesn't return a binary result: " + toolName));
        }
        return new LogAndBinResults(encodedFiles, encodeLog, rc);
    }

    public CompileAction setToolArgs(String... args) {
        if (args != null && args.length > 0) {
            Collections.addAll(this.toolArgs, args);
        } else {
            this.toolArgs.clear();
        }
        return this;
    }

    private static CompileAction getEntry() {
        if (CompileAction.entry == null) {
            try {
                entry = new CompileAction();
            } catch (IOException e) {
                fail(e.toString());
                throw new RuntimeException(e); // // appeasing the compiler: this line will never be executed.
            }
        }
        return entry;
    }
}
