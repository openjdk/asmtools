/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.util.regex.Pattern;

import static java.lang.String.format;

public interface ILogger {

    // A logged message isn't attached to a position of a parsed file
    int NOWHERE = Integer.MAX_VALUE;
    // Replacement for the tab found in an input
    CharSequence TAB_REPLACEMENT = " ".repeat(4);

    Pattern usagePattern = Pattern.compile("(-\\S+\\p{Blank}*\\S*)\\p{Blank}+([PGSDCSOIU]+.*)");

    default String getResourceString(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void warning(long where, String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(long where, String id, Object... args) {
        throw new NotImplementedException();
    }

    default void info(String id, Object... args) {
        String message = getInfo(id, args);
        if (message != null) {
            println(message);
        }
    }

    default long getCount(EMessageKind kind) {
        throw new NotImplementedException();
    }

    default String getInfo(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void warning(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(Throwable exception) {
        error(NOWHERE, exception.getMessage());
    }

    default void traceln(String format, Object... args) {
        getOutputs().printlne((args == null || args.length == 0) ? format : format(format, args));
        ;
    }

    default void trace(String format, Object... args) {
        getOutputs().printe((args == null || args.length == 0) ? format : format(format, args));
    }

    default void printErrorLn(String format, Object... args) {
        getOutputs().printlne((args == null || args.length == 0) ? format : format(format, args));
    }

    default void println(String format, Object... args) {
        getOutputs().printlns((args == null || args.length == 0) ? format : format(format, args));
    }

    default void println() {
        getOutputs().printlns("");
    }

    default void print(String format, Object... args) {
        getOutputs().prints((args == null || args.length == 0) ? format : format(format, args));
    }

    default void print(char ch) {
        getOutputs().prints(ch);
    }

    DualStreamToolOutput getOutputs();

    void setOutputs(DualStreamToolOutput nwoutput);

    default ToolOutput getToolOutput() {
        throw new NotImplementedException("implement wisely!");
    }

    default void setToolOutput(ToolOutput toolOutput) {
        throw new NotImplementedException("implement wisely!");
    }

    void printException(Throwable throwable);
}
