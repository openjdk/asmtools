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
package org.openjdk.asmtools.jdis;

import org.junit.jupiter.api.Assertions;
import org.openjdk.asmtools.lib.helper.BruteForceHelper;
import org.openjdk.asmtools.lib.helper.ThreeStringWriters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.openjdk.asmtools.common.Environment.OK;
import static org.openjdk.asmtools.lib.utility.StringUtils.addTail;

class JdisJasm {

    private final String[] jdisArgs;
    private  String[] jasmArgs = new String[0];
    private final BruteForceHelper worker;

    public JdisJasm(BruteForceHelper worker, String... jdisArgs) {
        this.jdisArgs = jdisArgs;
        this.worker = worker;
    }

    public JdisJasm setJasmArgs(String... jasmArgs) {
        this.jasmArgs = jasmArgs;
        return this;
    }

    public void run() throws IOException {
        BruteForceHelper.AsmToolsExecutable jdis = new BruteForceHelper.AsmToolsExecutable() {
            @Override
            public int run(ThreeStringWriters outs, File clazz) throws IOException {
                Main disassem;
                if (jdisArgs.length == 0) {
                    disassem = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), clazz.getAbsolutePath());
                } else {
                    disassem = new Main(outs.getToolOutputWrapper(), outs.getLoggers(), addTail(jdisArgs, clazz.getAbsolutePath()));
                }
                return disassem.disasm();
            }

            @Override
            public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
                String gs = (jdisArgs.length == 0) ? "" : "with %s ".formatted(String.join(",", jdisArgs));
                Assertions.assertEquals(0, failures.size(), "from " + all.size() + "(" + worker.getClassesRoot() +
                        ") failed to disassemble " + gs + failures.size() + ": " +
                        BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
            }
        };
        BruteForceHelper.AsmToolsExecutable jasm = new JasmToolExecutable(worker, jasmArgs);
        worker.work(jdis, jasm);
    }

    private static class JasmToolExecutable implements BruteForceHelper.AsmToolsExecutable {
        private final BruteForceHelper worker;
        private final String[] arguments;

        public JasmToolExecutable(BruteForceHelper worker, String[] arguments) {
            this.worker = worker;
            this.arguments = arguments;
        }

        @Override
        public int run(ThreeStringWriters outs, File clazz) throws IOException {
            File savedAsm = BruteForceHelper.saveDecompiledCode(worker.getDecompiledClass(clazz), "JdisJasmWorks");
            org.openjdk.asmtools.jasm.Main asm = new org.openjdk.asmtools.jasm.Main(outs.getLoggers(),
                    addTail(arguments, savedAsm.getAbsolutePath(), "-d", worker.getCompileDir().getAbsolutePath()));
            BruteForceHelper.createMetadata(outs, clazz, savedAsm, worker.getCompileDir(), worker.getClassesRoot());
            int rc = asm.compile();
            if (rc != OK) {
                outs.getToolOutput().println("Failed to compile %s".formatted(savedAsm));
                outs.getToolOutput().flush();
            }
            return rc;
        }

        @Override
        public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
            //three classes now fails; they will fail again in an attempt to be loaded on NPE
            Assertions.assertEquals(0, failures.size(),
                    "from " + all.size() + " failed to assemble " + String.join(",", arguments) + " to (" + worker.getCompileDir() + ") " +
                            failures.size() + ": " + BruteForceHelper.keySetToString(failures, worker.getClassesRoot()));
        }
    }
}
