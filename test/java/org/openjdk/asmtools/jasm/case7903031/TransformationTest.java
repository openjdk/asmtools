/*
 * Copyright (c) 2023,2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm.case7903031;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.action.Jasm;
import org.openjdk.asmtools.lib.action.Jdis;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This is the test for the issue CODETOOLS-7903031 (https://bugs.openjdk.org/browse/CODETOOLS-7903031)
 * "jasm: Asmtools does not support CP table when it's processing modules"
 * Files module-info.class.g.jasm has Constant Pool.
 * The test is intended to check that the module-info file with Constant Pool can be compiled without issues.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransformationTest {

    private final String[] jasmFiles = new String[]{"module-info.class.g.jasm", "module-info.class.jasm"};

    @BeforeAll
    public void init() throws IOException {
        for (int i = 0; i < jasmFiles.length; i++) {
            String fileName = jasmFiles[i];
            File resourceDir = new File(this.getClass().getResource(fileName).getFile()).getParentFile();
            jasmFiles[i] = resourceDir + File.separator + fileName;
        }
    }

    @Test
    public void moduleInfoTest() {
        for (int i = 0; i < jasmFiles.length; i++) {
            try {
                //jasm to class
                LogAndBinResults binResult = new Jasm().compile(List.of(jasmFiles[i]));
                // class to jasm
                LogAndTextResults textResult = new Jdis().setArgs(EToolArguments.JDIS_G).decode(binResult.getAsByteInput());

                // Check that it is a module-info.jasm with removed spaces, tabs and new lines
                String jasmOutput = textResult.getResultAsString(s -> s.replaceAll("[ \t\n]*", ""));
                assertThat(jasmOutput, Matchers.allOf(
                        Matchers.startsWith("module#6/*java.base*/version65"),
                        Matchers.matchesRegex(".*const#100.*"),
                        Matchers.matchesRegex(".*const.*[Cc]lass.*"),
                        Matchers.matchesRegex(".*const.*Package.*"),
                        Matchers.matchesRegex(".*uses.*java.text.spi.DateFormatSymbolsProvider.*"),
                        Matchers.matchesRegex(".*provides.*java.nio.file.spi.FileSystemProvider.*")));
            } catch (Exception ex) {
                fail("Unexpected exception: " + ex);
            }
        }
    }
}
