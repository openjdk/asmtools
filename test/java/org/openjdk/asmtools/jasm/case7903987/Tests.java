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
package org.openjdk.asmtools.jasm.case7903987;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.action.Jasm;
import org.openjdk.asmtools.lib.action.Jdis;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

/**
 * This is the test for the enhancement CODETOOLS-7903987 https://bugs.openjdk.org/browse/CODETOOLS-7903987)
 * "The jasm parser should allow the use of primitive types in Constant Pool."
 *   const #1=true;
 *   const #2=192837465;
 *   const #3=1l;
 *   const #6=2.1f;
 *   const #8="utf 8 entry";
 * <p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
    private Jasm jasm = new Jasm();
    private Jdis jdis = new Jdis().setArgs(EToolArguments.JDIS_G);
    private File resourceDir;

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
                // Detailed jasm
                Arguments.of("Test00.jasm", "")
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(this.getClass().getResource("Test00.jasm").getFile()).getParentFile();
    }

    @ParameterizedTest
    @MethodSource("getTestParameters")
    public void test00(String resourceName, String jasmSubString) {
        // jasm to class on the disk
        LogAndBinResults binResults = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        // jasm file was created
        Assertions.assertEquals(0, binResults.result);
        LogAndTextResults  textResults = jdis.decode(binResults.getAsByteInput());
        Assertions.assertEquals(0, textResults.result);
        String buf = textResults.output.toString();
        assertThat(buf, matchesRegex("(?s).*const.*#1.*int.*1;.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#2.*int.*192837465;.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#3.*long.*1l;.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#5.*float.*2.1f;.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#6.*double.*2.2d;.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#8.*Utf8.*Long utf-8.*"));
        assertThat(buf, matchesRegex("(?s).*const.*#9.*int.*0;.*"));
    }
}
