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

import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.lib.helper.BruteForceHelper;

import java.io.IOException;

class JdisJasmTest {
    @Test
    public void jdisJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        JdisJasm task = new JdisJasm(worker);
        task.run();
    }

    @Test
    public void jdisTJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdisJasm(worker, "-table").run();
    }

    @Test
    public void jdisGJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdisJasm(worker, "-g").run();
    }

    @Test
    public void jdisGTJasmAllDecompileCompileAndLoad() throws IOException {
        BruteForceHelper worker = new BruteForceHelper();
        new JdisJasm(worker, "-g", "-table").run();
    }
}
