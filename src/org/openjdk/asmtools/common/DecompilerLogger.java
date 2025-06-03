/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.openjdk.asmtools.common.EMessageKind.ERROR;
import static org.openjdk.asmtools.common.EMessageKind.WARNING;
import static org.openjdk.asmtools.common.Environment.OK;

public class

DecompilerLogger extends ToolLogger implements ILogger {

    // Message Container
    private final LinkedHashSet<String> messages = new LinkedHashSet<String>();

    private Consumer<String> addToContainer = (String msg) -> {
        if (msg != null) {
            messages.add(msg);
        }
    };

    /**
     * @param programName the tool name
     * @param cls         the environment class of the tool for which to obtain the resource bundle
     * @param outerLog    the logger stream
     */
    public DecompilerLogger(String programName, Class cls, DualStreamToolOutput outerLog) {
        super(programName, cls, outerLog);
    }


    @Override
    public void warning(String id, Object... args) {
        String msg = getResourceString(id, args);
        if (msg == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR,
                        "(I18NResourceBundle) The warning message '%s' not found", id);
            } else {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.WARNING, super.getProgramName(), format(id, args));
            }
        } else {
            msg = EMessageFormatter.VERBOSE.apply(EMessageKind.WARNING, super.getProgramName(), msg);
        }
        addToContainer.accept(msg);
    }

    @Override
    public void error(String id, Object... args) {
        String msg = getResourceString(id, args);
        if (msg == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR,
                        super.getProgramName(),
                        "(I18NResourceBundle) The error message '%s' not found", id);
            } else {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(), format(id, args));
            }
        } else {
            msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(), msg);
        }
        addToContainer.accept(msg);
    }

    @Override
    public void info(String id, Object... args) {
        String msg = getResourceString(id, args);
        if (msg == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR,
                        super.getProgramName(),
                        "(I18NResourceBundle) The error message '%s' not found", id);
            } else {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(), format(id, args));
            }
        } else {
            msg = EMessageFormatter.SHORT.apply(EMessageKind.ERROR, super.getProgramName(), msg);
        }
        addToContainer.accept(msg);
    }

    @Override
    public void printErrorLn(String format, Object... args) {
        String msg = (args == null || args.length == 0) ? format : format(format, args);
        addToContainer.accept(msg);
    }


    @Override
    public void error(Throwable exception) {
        String msg = ToolLogger.EMessageFormatter.VERBOSE.apply(ERROR, super.getProgramName(), exception.getMessage());
        addToContainer.accept(msg);
    }

    @Override
    public String getInfo(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            String msg;
            if (EMessageKind.isFromResourceBundle(id)) {
                msg = EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(),
                        "(I18NResourceBundle) The error message '%s' not found", id);
            } else {
                msg = (args == null || args.length == 0) ? id : format(id, args);
            }
            addToContainer.accept(msg);
        }
        return message;
    }

    @Override
    public long getCount(EMessageKind kind) {
        return messages.stream().filter(msg -> msg.contains(kind.shortForm())).count();
    }

    public int registerTotalIssues(int rc, ToolInput toolInput) {
        int ret = OK;
        int nErrors = (int) getCount(ERROR);
        int nWarnings = (int) getCount(WARNING);
        if (nWarnings > 0 || nErrors > 0) {
            // don't take into account warnings
            ret = max(rc, nErrors);
            String sWarnings = (nWarnings > 0) ?
                    "%d warning(s)".formatted(nWarnings).concat((nErrors > 0) ? ", " : "") : "";
            String sErrors = (nErrors > 0) ? "%d error(s) ".formatted(nErrors) : " ";
            info("err.count.issues", sWarnings, sErrors, toolInput);
        }
        return ret;
    }

    public synchronized void flush() {
        if (!messages.isEmpty()) {
            DualStreamToolOutput output = getOutputs();
            // output.printe('\n');
            for (String msg : messages) {
                output.printlne(msg);
            }
            synchronized (output) {
                output.flush();
                messages.clear();
            }
        }
    }

    @Override
    public void usage(List<String> usageIDs) {
        usage(usageIDs, id -> getInfo(id));
    }

    @Override
    public void usage(List<String> usageIDs, Function<String, String> func) {
        for (String id : usageIDs) {
            String s = func.apply(id);
            if (s != null) {
                Matcher m = usagePattern.matcher(s);
                if (m.find()) {
                    println(format("  %-21s %s", m.group(1).trim(), m.group(2).trim()));
                    if (s.contains("\n")) {
                        // multiline
                        String[] lines = s.split("\\n");
                        for (int i = 1; i < lines.length; i++) {
                            println(format("%s%s", " ".repeat(24), lines[i].trim()));
                        }
                    }
                } else {
                    String[] lines = s.split("\\n");
                    if (lines.length > 1 && lines[0].trim().startsWith("-")) {
                        println(format("  %s", lines[0].trim()));
                        for (int i = 1; i < lines.length; i++) {
                            println(format("%s%s", " ".repeat(24), lines[i].trim()));
                        }
                    } else {
                        println(s);
                    }
                }
            }
        }
    }
}
