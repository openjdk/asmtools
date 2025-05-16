/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm.case7902820;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.action.Jcoder;
import org.openjdk.asmtools.lib.action.Jdec;
import org.openjdk.asmtools.lib.action.Jdis;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JcodTests {

    private final Jcoder jcoder = new Jcoder();

    private File resourceDir;
    private String resName = "InvalidSourceDebugExtension.jcod";

    @BeforeAll
    public void init() throws IOException {
        File file = new File(this.getClass().getResource(resName).getFile());
        resourceDir = file.getParentFile();
    }

    /**
     * This is the test for the issue: CODETOOLS-7902820 (https://bugs.openjdk.org/browse/CODETOOLS-7902820)
     * "Asmtools incorrectly uses stdout for output error messages"
     * <p>
     * If InvalidSourceDebugExtension.jcod can't be processed by jcoder due to an error in the jcod then the command
     * java -jar asmtools.jar jcoder -w . InvalidSourceDebugExtension.jcod
     * hides the error because the error message is outputted to stdout instead of stderr.
     */
    @Test
    public void testJCoderWarning() {
        final LogAndBinResults jcodResult = jcoder.compile(List.of(resourceDir + File.separator + resName));
        List<String> out = jcodResult.getLogStringsByPrefix("Warning:");
        Assertions.assertEquals(OK, jcodResult.result);
        Assertions.assertEquals(1, out.size());
        String msg = out.get(0);
        // expected substring
        Assertions.assertTrue(
                msg.contains(" Expected attribute length \"2,147,483,647\" does not match the actual length \"0\"; expected length written"));
        //
        // class to jcod
        LogAndTextResults textResult = new Jdec().setArgs(EToolArguments.JDEC_G).decode(jcodResult.getAsByteInput());
        Assertions.assertEquals(FAILED, textResult.result);
        out = textResult.getLogStringsByPrefix("ERROR:");
        Assertions.assertEquals(1, out.size());
        msg = out.get(0);
        Assertions.assertTrue(msg.contains("Requested array size exceeds VM limit"));
        // class to jasm twice
        textResult = new Jdis().setArgs(EToolArguments.JDIS_G_T_LNT_LVT).decode(jcodResult.getAsByteInput());
        Assertions.assertEquals(FAILED, textResult.result);
        out = textResult.getLogStringsByPrefix("ERROR:");
        Assertions.assertEquals(1, out.size());
        msg = out.get(0);
        Assertions.assertTrue(msg.contains("Requested array size exceeds VM limit"));
        textResult = new Jdis().setArgs(EToolArguments.JDIS_GG_NC_LNT_LVT).decode(jcodResult.getAsByteInput());
        Assertions.assertEquals(FAILED, textResult.result);
    }
}
