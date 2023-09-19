/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm.case7903405;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.lib.LogAndReturn;
import org.openjdk.asmtools.lib.action.CompileAction;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.openjdk.asmtools.lib.action.EAsmTools.Tool.TOOL_PASSED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NegativeJasmTests {

    CompileAction compiler;
    File resourceDir;
    String resName = "ifge_overflow.jasm";

    @BeforeAll
    public void init() throws IOException {
        File file = new File(this.getClass().getResource(resName).getFile());
        resourceDir = file.getParentFile();
        compiler = new CompileAction();
    }

    /**
     * This is the test for the issue: CODETOOLS-7903405 (https://bugs.openjdk.org/browse/CODETOOLS-7903405)
     * "compiler does not warn about instruction arguments that exceed allowed limits"
     * <p>
     * The attached jasm source has a set of nop instructions between the jfqe instruction and the "SKIP" label that is used by it.
     * The length of the set is 0x8FFF which exceeds allowed by JVMS - signed 16-bit value 0x8000.
     * The jasm silently produces a class file that is declined by JVM:
     * <p>
     * >java Test
     * Error: Unable to initialize main class Test
     * Caused by: java.lang.VerifyError: (class: Test, method: test_1 signature: ()V) Illegal target of jump or branch
     * <p>
     * Since jasm allows to generate a "defect" binaries, it would be nice if the jasm assembler warns that already,
     * and not just the class file verifier.
     * <p>
     * Expected warning should be like:
     * <p>
     * jasm   -  WARN: test_1()V - The argument 0x8000 of the 'ifge' instruction is written.
     * It is larger than the allowed signed 16-bit value 0x7FFF
     * 1 warning(s)
     */
    @Test
    public void testIfgeOverflow_7903405() {
        final LogAndReturn logAndReturn = compiler.jasm(
                List.of(resourceDir + File.separator + resName));
        final List<String> warns = logAndReturn.getLogStringsByPrefix("WARN:");
        Assertions.assertEquals(logAndReturn.result, TOOL_PASSED);
        Assertions.assertEquals(warns.size(), 1);
        String warn = warns.get(0);
        // expected substrings
        Assertions.assertTrue(warn.contains("test_1()V"),
                "Expected method name \'test_1()V\' not found");
        Assertions.assertTrue(warn.contains("signed 16-bit value 0x7FFF"),
                "Expected argument length \'signed 16-bit value 0x7FFF\' not found");
        Assertions.assertTrue(warn.contains("0x8000"),
                "Expected length of written argument \'0x8000\' not found");
    }
}
