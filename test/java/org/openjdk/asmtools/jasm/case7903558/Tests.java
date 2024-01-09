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
package org.openjdk.asmtools.jasm.case7903558;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.LogAndTextResults;
import org.openjdk.asmtools.lib.action.CompileAction;
import org.openjdk.asmtools.lib.action.GenerateAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.openjdk.asmtools.lib.action.EAsmTools.Tool.TOOL_PASSED;

/**
 * This is the test for the issue CODETOOLS-7903558 (https://bugs.openjdk.org/browse/CODETOOLS-7903558)
 * "jasm: add support this_class, super_class value(s)"
 * <p>
 * asm tool should support the values:
 * this_class[:]  (#ID | IDENT); // CLASSNAME
 * super_class[:] (#ID | IDENT); // SUPERCLASSNAME
 * <p>
 * Then the command `java -jar asmtools.jar jasm -d . FILE.jasm` where the jasm file is as follows:
 * class FILENAME.data {
 * this_class CLASSNAME;
 * super_class SUPERCLASSNAME;
 * }
 * will produce a binary file `FILENAME.data` which, after decompiling (`java -jar asmtools.jar jdis FILENAME.data`), will be:
 * <p>
 * super class CLASSNAME extends SUPERCLASSNAME version 45:0 {}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
    private CompileAction compiler;
    private GenerateAction generator;
    private File resourceDir;
    private File resultDir;

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
                // Detailed jasm
                Arguments.of("JasmFile01.g.jasm", "FileName01.class", "ClassName01 extends java/lang/String"),
                Arguments.of("JasmFile02.g.jasm", "FileName02.data", "ClassName02 extends java/lang/String"),
                Arguments.of("JasmFile03.g.jasm", "FileName03.class", "ClassName03 extends package/SuperClassName03"),
                Arguments.of("JasmFile04.g.jasm", "FileName04.obj", "ClassName04 extends package/SuperClassName04"),
                // short version
                Arguments.of("JasmFile01.jasm", "FileName01.class", "ClassName01 extends java/lang/String"),
                Arguments.of("JasmFile02.jasm", "FileName02.data", "ClassName02 extends java/lang/String"),
                Arguments.of("JasmFile03.jasm", "FileName03.class", "ClassName03 extends package/SuperClassName03"),
                Arguments.of("JasmFile04.jasm", "FileName04.obj", "ClassName04 version")
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resultDir = Files.createTempDirectory("JdisJasmWorks").toFile();
        resultDir.deleteOnExit();
        resourceDir = new File(this.getClass().getResource("JasmFile01.g.jasm").getFile()).getParentFile();
        compiler = new CompileAction();
        compiler.setToolArgs("-d", resultDir.getAbsolutePath());
        generator = new GenerateAction();
    }

    @ParameterizedTest
    @MethodSource("getTestParameters")
    public void moduleInfoTest(String resourceName, String outputFileName, String jasmSubString) {
        // jasm to class on the disk
        compiler.getJasmResult(List.of(resourceDir + File.separator + resourceName));
        Path resultPath = Path.of(resultDir + File.separator + outputFileName);
        Assertions.assertTrue(Files.exists(resultPath), format("Result file not found: %s%n", resultPath));
        // class to jasm
        LogAndTextResults textResult = generator.getJdisResult(List.of(resultPath.toString()));
        Assertions.assertEquals(textResult.result, TOOL_PASSED);
        // expected substrings
        String str = textResult.output.toString().substring(0, 80);
        Assertions.assertTrue(str.contains(jasmSubString), format("'%s' not found in '%s'%n", jasmSubString, str));
    }
}
