/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.openjdk.asmtools.Main.sharedI18n;
import static org.openjdk.asmtools.common.EMessageKind.ERROR;
import static org.openjdk.asmtools.common.EMessageKind.INFO;

public abstract class ToolLogger implements ILogger {

    protected boolean ignoreWarnings = false;         // do not print / ignore warnings
    protected boolean strictWarnings  = false;        // consider warnings as errors

    class ToolResources {
        private final static HashMap<String, I18NResourceBundle> resources = new HashMap<>();

        public ToolResources(String programName, Class cls) {
            if (!ToolResources.resources.containsKey(programName)) {
                ToolResources.resources.put(programName, I18NResourceBundle.getBundleForClass(cls));
            }
        }

        public void setWarn(boolean value) {
            ToolResources.resources.get(ToolLogger.this.programName).setWarn(value);
        }

        public String getString(String id, Object... args) {
            return ToolResources.resources.get(ToolLogger.this.programName).getString(id, args);
        }

        public boolean containsKey(String key) {
            return ToolResources.resources.get(ToolLogger.this.programName).containsKey(key);
        }
    }

    private final String programName;

    private ToolResources toolResources;

    DualStreamToolOutput outerLog;
    // Input file name is needed for logging purposes
    private String inputFileName;
    private String simpleInputFileName;

    static {
        sharedI18n.setWarn(false);
    }

    /**
     * @param programName the tool name
     * @param cls         the environment class of the tool for which to obtain the resource bundle
     * @param outerLog    the logger stream
     */
    public ToolLogger(String programName, Class cls, DualStreamToolOutput outerLog) {
        // Set Resource bundle for the tool
        this.toolResources = new ToolResources(programName, cls);
        this.programName = programName;
        this.outerLog = outerLog;
    }

    public String getResourceString(String id, Object... args) {
        String resString;
        toolResources.setWarn(false);
        try {
            resString = toolResources.getString(id, args);
        } finally {
            toolResources.setWarn(true);
        }
        if (resString == null || resString.equals(id)) {
            resString = sharedI18n.getString(id, args);
        }
        if (resString == null || resString.equals(id)) {
            //to get proper error message
            resString = toolResources.getString(id, args);
        }
        return resString;
    }

    public void setInputFileName(ToolInput inputFileName) throws IOException {
        this.inputFileName = inputFileName.getFileName();
        this.simpleInputFileName = Paths.get(inputFileName.getFileName()).getFileName().toString();
        // content of the input file will be loaded only if the file will be parsed by jasm/jcoder
    }

    public Message getResourceString(EMessageKind kind, String id, Object... args) {
        String str;
        for (String prefix : Set.of("", kind.prefix)) {
            if (toolResources.containsKey(prefix + id) || sharedI18n.containsKey(prefix + id)) {
                str = getResourceString(id, args);
                if (str != null) {
                    return new Message(kind, str);
                }
            }
        }
        return new Message(ERROR, null);
    }

    @Override
    public DualStreamToolOutput getOutputs() {
        return outerLog;
    }

    @Override
    public void setOutputs(DualStreamToolOutput nwoutput) {
        this.outerLog = nwoutput;
    }

    public String getSimpleInputFileName() {
        return simpleInputFileName;
    }

    @Override
    public void printException(Throwable throwable) {
        getOutputs().stacktrace(throwable);
    }

    public String getProgramName() {
        return programName;
    }

    public abstract void usage(List<String> ids);

    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    public enum EMessageFormatter {
        SHORT((severity, name, message) -> format("%s", message)),
        LONG((severity, name, message) -> format("%s: %s", severity.longForm(), message)),
        VERBOSE((severity, name, message) -> severity == INFO ? message : format("%-7s-%6s: %s",
                name, severity.shortForm(), message));
        final private TriFunction<EMessageKind, String, String, String> triFunc;

        EMessageFormatter(TriFunction<EMessageKind, String, String, String> func) {
            this.triFunc = func;
        }

        public String apply(EMessageKind kind, String name, String format, Object... args) {
            return triFunc.apply(kind, name, format(format, args));
        }

        public String apply(String name, Message message) {
            return message.notFound() ? "" : triFunc.apply(message.kind(), name, message.text());
        }
    }

    public record Message(EMessageKind kind, String text) {

        Message(EMessageKind kind, String format, Object... args) {
            this(kind, format(format, args));
        }

        public boolean notFound() {
            return kind == ERROR && text == null;
        }
    }
}
