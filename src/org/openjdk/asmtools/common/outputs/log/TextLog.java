/*
 * Copyright (c) 2023, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.outputs.log;

import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.outputs.TextOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class TextLog extends NamedDualStreamToolOutput {

    public static class NamedLog extends TextOutput.NamedSource {

        public NamedLog(String fqn, String body) {
            super(fqn, body);
        }
    }

    private final ArrayList<NamedLog> outputs = new ArrayList<>();
    private StringBuilder currentClass;
    private StringBuilder classlessLogs = new StringBuilder();

    public ArrayList<NamedLog> getOutputs() {
        return outputs;
    }

    public String getClasslessLogs() {
        return classlessLogs.toString();
    }

    @Override
    public String toString() {
        return classlessLogs.toString() + System.lineSeparator() + outputs.stream().map(a -> a.toString()).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public void startClass(String fullyQualifiedName, Optional<String> suffix, Environment logger) throws IOException {
        super.startClass(fullyQualifiedName, suffix, logger);
        currentClass = new StringBuilder();
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        if (!getCurrentClassName().equals(fqn)) {
            throw new RuntimeException("Ended different class - " + fqn + " - then started - " + super.fqn);
        }
        outputs.add(new NamedLog(fqn, currentClass.toString()));
        super.fqn = null;
        currentClass = null;
    }

    @Override
    public void printlns(String line) {
        if (currentClass == null) {
            classlessLogs.append(line).append(System.lineSeparator());
        } else {
            currentClass.append(line).append(System.lineSeparator());
        }
    }

    @Override
    public void prints(String line) {
        if (currentClass == null) {
            classlessLogs.append(line);
        } else {
            currentClass.append(line);
        }
    }

    @Override
    public void prints(char line) {
        if (currentClass == null) {
            classlessLogs.append(line);
        } else {
            currentClass.append(line);
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void printlne(String line) {
        if (currentClass == null) {
            classlessLogs.append(line).append(System.lineSeparator());
        } else {
            currentClass.append(line).append(System.lineSeparator());
        }
    }

    @Override
    public void printe(String line) {
        if (currentClass == null) {
            classlessLogs.append(line);
        } else {
            currentClass.append(line);
        }
    }

    @Override
    public void printe(char line) {
        if (currentClass == null) {
            classlessLogs.append(line);
        } else {
            currentClass.append(line);
        }
    }

    @Override
    public void stacktrace(Throwable ex) {
        if (currentClass == null) {
            classlessLogs.append(ToolOutput.exToString(ex));
        } else {
            currentClass.append(ToolOutput.exToString(ex));
        }
    }

    @Override
    public ToolOutput getSToolObject() {
        return this;
    }

    @Override
    public ToolOutput getEToolObject() {
        return this;
    }
}
