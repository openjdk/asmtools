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
package org.openjdk.asmtools.jasm.case7903976;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.action.Jasm;
import org.openjdk.asmtools.lib.log.LogAndBinResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * This is the test for the issue CODETOOLS-7903976 (https://bugs.openjdk.org/browse/CODETOOLS-7903976)
 * "The jasm parser should permit malformed method signatures for JCK tests"
 * <p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
    private Jasm jasm = new Jasm();
    private File resourceDir;

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
                // Detailed jasm
                Arguments.of("Test01.jasm", "Warning: Malformed method signature at position 1. \"({JavaTypeSignature})Result\" is missing."),
                Arguments.of("Test02.jasm", "Warning: Malformed method signature at position 38. \"({JavaTypeSignature})Result\" is missing."),
                Arguments.of("Test03.jasm", "Warning: Malformed method signature at position 3. Unknown token \"X\" in \"({JavaTypeSignature})Result\""),
                Arguments.of("Test04.jasm", "Warning: Malformed method signature at position 2. Unknown token \"X\" in \"({JavaTypeSignature})Result\""),
                Arguments.of("Test05.jasm", "Warning: Malformed method signature at position 38. \"({JavaTypeSignature})Result\" is missing."),
                Arguments.of("Test06.jasm", "Warning: Malformed method signature at position 1. A \"(\" token is expected in \"({JavaTypeSignature})Result\""),
                Arguments.of("Test07.jasm", "Warning: Malformed method signature at position 3. Unknown token \"X\" in \"({JavaTypeSignature})Result\""),
                Arguments.of("Test08.jasm", "Warning: Malformed method signature at position 3. An array type signature is expected: \"[JavaTypeSignature\""),
                Arguments.of("Test09.jasm", "Warning: Malformed method signature at position 39. \"({JavaTypeSignature})Result\" is missing."),
                Arguments.of("Test10.jasm", "Warning: Malformed method signature at position 19. ClassTypeSignature is not properly terminated: L{PackageSpecifier/}SimpleClassTypeSignature;")
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(this.getClass().getResource("Test01.jasm").getFile()).getParentFile();
    }

    @ParameterizedTest
    @MethodSource("getTestParameters")
    public void methodSignatureTest(String resourceName, String jasmSubString) {
        // jasm to class on the disk
        LogAndBinResults res = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        String log = res.log.toString();
        Assertions.assertTrue(log.contains(jasmSubString));
        // jasm file was created
        Assertions.assertEquals(0, res.result);
    }
}
