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
package org.openjdk.asmtools.jasm.case7902696;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.action.Jasm;
import org.openjdk.asmtools.lib.action.Jdec;
import org.openjdk.asmtools.lib.action.Jdis;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.Environment.FAILED;
import static org.openjdk.asmtools.common.Environment.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JasmTests {

    private final Jasm jasm = new Jasm();

    private File resourceDir;

    private static Stream<Arguments> getJasmParameters() {
        return Stream.of(
                Arguments.of("CondyNestedResolution.g.jasm"),
                Arguments.of("CondyNestedResolution.g.t.jasm"));
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(Objects.requireNonNull(this.getClass().
                getResource("CondyNestedResolution.g.jasm")).getFile()).getParentFile();
    }

    /**
     * This is the test for the issue: CODETOOLS-7902696 (https://bugs.openjdk.org/browse/CODETOOLS-7902696)
     * "jdis doesn't catch circular references in bsm args"
     * <p>
     * public static Method bsm4arg:"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Object;
     * Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
     * stack 19 locals 19
     * {
     * getstatic Field java/lang/System.out:"Ljava/io/PrintStream;";
     * ldc String "In bsm4arg";
     * invokevirtual Method java/io/PrintStream.println:"(Ljava/lang/Object;)V";
     * getstatic Field java/lang/System.out:"Ljava/io/PrintStream;";
     * aload_3;
     * invokevirtual Method java/io/PrintStream.println:"(Ljava/lang/Object;)V";
     * getstatic Field java/lang/System.out:"Ljava/io/PrintStream;";
     * aload 4;
     * invokevirtual Method java/io/PrintStream.println:"(Ljava/lang/Object;)V";
     * getstatic Field java/lang/System.out:"Ljava/io/PrintStream;";
     * aload 5;
     * invokevirtual Method java/io/PrintStream.println:"(Ljava/lang/Object;)V";
     * getstatic Field java/lang/System.out:"Ljava/io/PrintStream;";
     * aload 6;
     * invokevirtual Method java/io/PrintStream.println:"(Ljava/lang/Object;)V";
     * aload_3;
     * areturn;
     * }
     * public static Method test_condy:"()V"
     * stack 12 locals 12
     * {
     * jdis: fatal error in file: CondyNestedResolution.class
     */
    @ParameterizedTest
    @MethodSource("getJasmParameters")
    public void testJCoderWarning(String resourceName) {
        // jasm to class
        final LogAndBinResults binResult = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        Assertions.assertEquals(OK, binResult.result);
       // class to jasm
        LogAndTextResults textResult = new Jdis().setArgs(EToolArguments.JDIS).decode(binResult.getAsByteInput());
        Assertions.assertEquals(FAILED, textResult.result);
        List<String> out = textResult.getLogStringsByPrefix("ERROR:");
        Assertions.assertEquals(9, out.size());
        for (String line : out) {
            Assertions.assertTrue(line.contains("circular reference to Dynamic #"));
        }
    }
}
