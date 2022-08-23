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
package org.openjdk.asmtools.common;

import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.EMessageKind.ERROR;
import static org.openjdk.asmtools.common.EMessageKind.INFO;

public class ToolLogger implements ILogger {

    private static String programName;
    private static I18NResourceBundle i18n;
    // Logger's streams
    final private PrintWriter errLog;
    final private PrintWriter outLog;
    // Input file name is needed for logging purposes
    private String inputFileName;
    private String simpleInputFileName;

    protected ToolLogger(PrintWriter errLog, PrintWriter outLog) {
        this.errLog = errLog;
        this.outLog = outLog;
    }

    static void setResources(String programName, I18NResourceBundle i18n) {
        ToolLogger.programName = programName;
        ToolLogger.i18n = i18n;
    }

    public static I18NResourceBundle getI18n() {
        return i18n;
    }

    public static String getProgramName() {
        return programName;
    }

    public static String getResourceString(String id, Object... args) {
        return i18n.getString(id, args);
    }

    public void setInputFileName(String inputFileName) throws IOException {
        this.inputFileName = inputFileName;
        this.simpleInputFileName = Paths.get(inputFileName).getFileName().toString();
        // content of the input file will be loaded only if the file will be parsed by jasm/jcoder
    }

    public Message getResourceString(EMessageKind kind, String id, Object... args) {
        String str;
        for (String prefix : Set.of("", kind.prefix)) {
            if (ToolLogger.i18n.containsKey(prefix + id)) {
                str = getResourceString(id, args);
                if (str != null) {
                    return new Message(kind, str);
                }
            }
        }
        return new Message(ERROR, null);
    }

    @Override
    public PrintWriter getErrLog() {
        return errLog;
    }

    @Override
    public PrintWriter getOutLog() {
        return outLog;
    }

    public String getSimpleInputFileName() {
        return simpleInputFileName;
    }

    @Override
    public void printException(Throwable throwable) {
        throwable.printStackTrace(errLog);
    }

    public enum EMessageFormatter {
        SHORT((severity, message) -> format("%s", message)),
        LONG((severity, message) -> format("%s: %s", severity.longForm(), message)),
        VERBOSE((severity, message) -> severity == INFO ? message : format("%-5s-%6s: %s",
                ToolLogger.getProgramName(), severity.shortForm(), message));
        final private BiFunction<EMessageKind, String, String> func;

        EMessageFormatter(BiFunction<EMessageKind, String, String> func) {
            this.func = func;
        }

        public String apply(EMessageKind kind, String format, Object... args) {
            return func.apply(kind, format(format, args));
        }

        public String apply(Message message) {
            return message.notFound() ? "" : func.apply(message.kind(), message.text());
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
