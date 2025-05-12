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

import org.openjdk.asmtools.common.Decoder;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.TriFunction;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.StdoutOutput;
import org.openjdk.asmtools.common.outputs.TextOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;
import org.openjdk.asmtools.lib.log.LogAndReturn;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openjdk.asmtools.asmutils.StringUtils.ListToString;
import static org.openjdk.asmtools.common.Environment.OK;

public abstract sealed class DecodeActor<D extends Decoder<?>,
        R extends LogAndReturn> extends Action permits Jdis, Jdec {

    protected TriFunction<TextOutput, StringLog, ToolInput[], D> inputsDecoder;
    protected TriFunction<TextOutput, StringLog, String[], D> filesDecoder;

    protected DecodeActor(EAsmTool tool) {
        super(tool);
    }

    protected DecodeActor(EAsmTool tool, Path destDir) {
        super(tool, destDir);
    }

    /**
     * Moderator method based on reflection API to call tools
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
            Constructor<?> constr = toolClass.getConstructor(ToolOutput.class, String[].class);
            Object tool = constr.newInstance(new StdoutOutput(), args.toArray(String[]::new));
            Method m = toolClass.getMethod("decode");
            int r = (int) m.invoke(tool);
            return r == OK;
        } catch (ClassNotFoundException e) {
            fail("can't find " + toolName);
        } catch (ReflectiveOperationException t) {
            fail("error invoking " + toolName + ": " + t);
        }
        return result;
    }

    public R decode(List<String> files) {
        if (files.size() == 0)
            fail("%s: tool inputs are missing".formatted(toolName));
        ArrayList<String> args = new ArrayList<>(Arrays.asList(currentToolArgs.getArgs()));
        args.addAll(getDestDirParams());
        args.addAll(files);
        super.trace(()->"%s.decode%s".formatted(toolName, ListToString(args)));
        TextOutput encodedFiles = new TextOutput();
        StringLog encodeLog = new StringLog();
        D decoder = filesDecoder.apply(encodedFiles, encodeLog, args.toArray(String[]::new));
        int rc = decoder.decode();
        return (R) new LogAndTextResults(encodedFiles, encodeLog, rc);
    }

    public R decode(ToolInput... toolInputs) {
        if (toolInputs.length == 0)
            fail("%s: tool inputs are missing".formatted(toolName));
        ArrayList<String> args = new ArrayList<>(Arrays.asList(currentToolArgs.getArgs()));
        args.addAll(getDestDirParams());
        super.trace(()->"%s.decode%s".formatted(toolName, ListToString(args)));
        TextOutput encodedFiles = new TextOutput();
        StringLog encodeLog = new StringLog();
        D decoder = inputsDecoder.apply(encodedFiles, encodeLog, toolInputs);
        int rc = decoder.decode(args.toArray(String[]::new)) ? OK : Environment.FAILED;
        return (R) new LogAndTextResults(encodedFiles, encodeLog, rc);
    }
}
