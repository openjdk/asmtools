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

import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.util.List;
import java.util.regex.Matcher;

import static java.lang.String.format;

public class DecompilerLogger extends ToolLogger implements ILogger {

    /**
     * @param programName the tool name
     * @param cls         the environment class of the tool for which to obtain the resource bundle
     * @param outerLog    the logger stream
     */
    public DecompilerLogger(String programName, Class cls, DualStreamToolOutput outerLog) {
        super(programName, cls, outerLog);
    }

    @Override
    public void usage(List<String> usageIDs) {
        for (String id : usageIDs) {
            String s = getInfo(id);
            if (s != null) {
                Matcher m = usagePattern.matcher(s);
                if (m.find()) {
                    println(format("  %-21s %s", m.group(1).trim(), m.group(2).trim()));
                } else {
                    println(s);
                }
            }
        }
    }

    @Override
    public void printErrorLn(String format, Object... args) {
        super.printErrorLn((args == null || args.length == 0) ? format : format(format, args));
    }

    @Override
    public void error(Throwable exception) {
        super.printErrorLn(ToolLogger.EMessageFormatter.VERBOSE.apply(super.getProgramName(),
                new ToolLogger.Message(EMessageKind.ERROR, super.getProgramName(), exception.getMessage())));
    }

    @Override
    public void warning(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                printErrorLn(EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR,
                        "(I18NResourceBundle) The warning message '%s' not found", id));
            } else {
                println(EMessageFormatter.LONG.apply(EMessageKind.WARNING, super.getProgramName(), format(id, args)));
            }
        } else {
            println(EMessageFormatter.LONG.apply(EMessageKind.WARNING, super.getProgramName(), message));
        }
    }

    @Override
    public void error(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                printErrorLn(EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR,
                        super.getProgramName(),
                        "(I18NResourceBundle) The error message '%s' not found", id));
            } else {
                printErrorLn(EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(), format(id, args)));
            }
        } else {
            printErrorLn(EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(), message));
        }
    }

    @Override
    public String getInfo(String id, Object... args) {
        String message = getResourceString(id, args);
        if (message == null) {
            if (EMessageKind.isFromResourceBundle(id)) {
                printErrorLn(EMessageFormatter.VERBOSE.apply(EMessageKind.ERROR, super.getProgramName(),
                        "(I18NResourceBundle) The error message '%s' not found", id));
            } else {
                println(id, args);
            }
        }
        return message;
    }
}
