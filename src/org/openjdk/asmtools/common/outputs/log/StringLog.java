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

import org.openjdk.asmtools.common.outputs.ToolOutput;

public class StringLog extends NamedDualStreamToolOutput {

    private final StringBuilder log = new StringBuilder();

    @Override
    public String toString() {
        return log.toString();
    }

    @Override
    public void printlns(String line) {
        log.append(line).append(System.lineSeparator());
    }

    @Override
    public void prints(String line) {
        log.append(line);
    }

    @Override
    public void prints(char line) {
        log.append(line);
    }

    @Override
    public void flush() {

    }

    @Override
    public void printlne(String line) {
        log.append(line).append(System.lineSeparator());
    }

    @Override
    public void printe(String line) {
        log.append(line);
    }

    @Override
    public void printe(char line) {
        log.append(line);
    }

    @Override
    public void stacktrace(Throwable ex) {
        log.append(ToolOutput.exToString(ex));
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
