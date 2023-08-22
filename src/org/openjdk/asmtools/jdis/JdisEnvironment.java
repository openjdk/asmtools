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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.common.DecompilerLogger;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

public class JdisEnvironment extends Environment<DecompilerLogger> {

    /**
     * @param builder the jdis environment builder
     */
    private JdisEnvironment(Builder<JdisEnvironment, DecompilerLogger> builder) {
        super(builder);
    }

    @Override
    public void printErrorLn(String format, Object... args) {
        getLogger().printErrorLn(format, args);
    }

    @Override
    public void warning(String format, Object... args) {
        getLogger().warning(format, args);
    }

    static class JdisBuilder extends Environment.Builder<JdisEnvironment, DecompilerLogger> {

        public JdisBuilder(ToolOutput toolOutput, DualStreamToolOutput outerLog) {
            super(toolOutput, new DecompilerLogger("jdis", JdisEnvironment.class, outerLog));
        }

        @Override
        public JdisEnvironment build() {
            return new JdisEnvironment(this);
        }
    }
}
