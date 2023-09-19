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
package org.openjdk.asmtools.jasm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;
import org.openjdk.asmtools.ext.CaptureSystemOutput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.openjdk.asmtools.ext.CaptureSystemOutput.Kind.ERROR;

public class MultipleFilesInSingleJasmTest {

    @Test
    @CaptureSystemOutput(value = ERROR, mute = true)
    public void clfacc00610m10pTest(CaptureSystemOutput.OutputCapture outputCapture) throws IOException {
        byte[] jasmFile = getJasmFile("clfacc00610m10p.jasm");
        ToolInput file = new ByteInput(jasmFile);
        ByteOutput output = new ByteOutput();
        DualStreamToolOutput log = new StderrLog();
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v");
        int i = jasm.compile();
        outputCapture.expect(containsString("Invalid modifier(s) for a class 0x0002"));
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(2, output.getOutputs().size());
    }

    private byte[] getJasmFile(String s) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(s);
        byte[] bytes;
        try (is) {
            bytes = is.readAllBytes();
        }
        Assertions.assertNotNull(bytes);
        String jasm = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertNotNull(jasm);
        return bytes;
    }

    @Test
    public void spinum00101m10pTest() throws IOException {
        byte[] jasmFile = getJasmFile("spinum00101m10p.jasm");
        ToolInput file = new ByteInput(jasmFile);
        ByteOutput output = new ByteOutput();
        DualStreamToolOutput log = new StderrLog(); //todo hide to ToolOutput.StringLog once done
        org.openjdk.asmtools.jasm.Main jasm = new org.openjdk.asmtools.jasm.Main(output, log, file, "-v", "-nowarn");
        int i = jasm.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(258, output.getOutputs().size());
    }
}
