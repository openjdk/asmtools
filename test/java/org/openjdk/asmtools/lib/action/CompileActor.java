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

import org.openjdk.asmtools.common.Compiler;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.TriFunction;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndReturn;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openjdk.asmtools.asmutils.StringUtils.ListToString;

public abstract sealed class CompileActor<C extends Compiler<?>,
        R extends LogAndReturn> extends Action permits Jasm, Jcoder {

    protected TriFunction<ByteOutput, StringLog, ToolInput[], C> inputsCompiler;
    protected TriFunction<ByteOutput, StringLog, String[], C> filesCompiler;

    protected CompileActor(EAsmTool tool) {
        super(tool);
    }

    protected CompileActor(EAsmTool tool, Path destDir) {
        super(tool, destDir);
    }

    /**
     * Moderator method based on reflection API to call compiler
     */
    public boolean reflectiveCompile(List<String> files) {
        boolean result = false;
        if (files.isEmpty())
            fail(toolName + ": no files");
        ArrayList<String> args = new ArrayList<>(Arrays.asList(currentToolArgs.getArgs()));
        args.addAll(getDestDirParams());
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
                result = (Boolean) r;
            } else {
                fail("unexpected result from " + toolName + ": " + r.toString());
            }
        } catch (ClassNotFoundException e) {
            fail("can't find " + toolName);
        } catch (ReflectiveOperationException t) {
            fail("error invoking " + toolName + ": " + t);
        }
        return result;
    }

    public R compile(List<String> files) {
        if (files.size() == 0)
            fail("%s: tool inputs are missing".formatted(toolName));
        ArrayList<String> args = new ArrayList<>(Arrays.asList(currentToolArgs.getArgs()));
        args.addAll(getDestDirParams());
        args.addAll(files);
        super.trace(()->"%s.compile%s".formatted(toolName, ListToString(args)));
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        C compiler = filesCompiler.apply(encodedFiles, encodeLog, args.toArray(String[]::new));
        int rc = compiler.compile();
        return (R) new LogAndBinResults(encodedFiles, encodeLog, rc);
    }

    public R compile(ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("%s: tool inputs are missing".formatted(toolName));
        ArrayList<String> args = new ArrayList<>(Arrays.asList(currentToolArgs.getArgs()));
        args.addAll(getDestDirParams());
        super.trace(()->"%s.compile%s".formatted(toolName, ListToString(args)));
        ByteOutput encodedFiles = new ByteOutput();
        StringLog encodeLog = new StringLog();
        C compiler = inputsCompiler.apply(encodedFiles, encodeLog, toolInputs);
        int rc = compiler.compile(args.toArray(String[]::new)) ? Environment.OK : Environment.FAILED;
        return (R) new LogAndBinResults(encodedFiles, encodeLog, rc);
    }
}
