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
package org.openjdk.asmtools.structure.ClassFile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.lib.action.Jasm;
import org.openjdk.asmtools.lib.log.LogAndBinResults;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.openjdk.asmtools.lib.utility.StringUtils.funcSubStrCount;

/**
 * This is the test for class/interface/field modifiers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClassFileTests {
    private Jasm jasm = new Jasm();
    private File resourceDir;

    private static Stream<Arguments> getTestParameters() {
        return Stream.of(
                // jasm
                Arguments.of("ClassFile00.jasm",
                        List.of("\"ACC_VALUE, ACC_PUBLIC\": If the ACC_INTERFACE flag is not set a value class must have at least one of its ACC_FINAL, ACC_IDENTITY, or ACC_ABSTRACT flags set",
                                "Class file version 69:0 does not conform to the new kind of objects; expected version")),
                Arguments.of("ClassFile01.jasm",
                        List.of("Class file version 69:0 does not conform to the new kind of objects; expected version")),

                Arguments.of("ClassFile02.jasm",
                        List.of("Class file version not specified in file or by -cv parameter. Defaulting to version \"%s\"".
                                formatted(CFVersion.ValueObjectsVersion().asString()))),
                Arguments.of("ClassFile03.jasm",
                        List.of(
                                "\"ACC_VALUE, ACC_PUBLIC\": If the ACC_INTERFACE flag is not set a value class must have at least one of its ACC_FINAL, ACC_IDENTITY, or ACC_ABSTRACT flags set",
                                "Class file version not specified in file or by -cv parameter. Defaulting to version \"%s\"".
                                        formatted(CFVersion.ValueObjectsVersion().asString()))),
                Arguments.of("ClassFile04.jasm",
                        List.of("Ambiguous use of similar modifiers")),
                Arguments.of("ClassFile05.jasm",
                        List.of("Ambiguous use of similar modifiers")),
                Arguments.of("ClassFile06.jasm",
                        List.of(
                                "\"ACC_PUBLIC, ACC_IDENTITY, ACC_INTERFACE, ACC_ENUM\": If the ACC_INTERFACE flag is set, the ACC_ABSTRACT flag must also be set",
                                "\"ACC_PUBLIC, ACC_IDENTITY, ACC_INTERFACE, ACC_ENUM\": If the ACC_INTERFACE flag is set, the ACC_FINAL, ACC_IDENTITY, ACC_ENUM, and ACC_MODULE flags must not be set",
                                "\"ACC_PRIVATE, ACC_STRICT\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set",
                                "<init> method cannot be an interface method"
                        )),
                Arguments.of("ClassFile07.jasm",
                        List.of(
                                "\"ACC_PUBLIC, ACC_ANNOTATION\": If the ACC_INTERFACE flag is not set ACC_ANNOTATION, and ACC_MODULE flags must not be set."
                        )),
                Arguments.of("ClassFile08.jasm",
                        List.of(
                                "\"ACC_INTERFACE, ACC_ENUM\": If the ACC_INTERFACE flag is set, the ACC_ABSTRACT flag must also be set",
                                "\"ACC_INTERFACE, ACC_ENUM\": If the ACC_INTERFACE flag is set, the ACC_FINAL, ACC_SUPER, ACC_ENUM, and ACC_MODULE flags must not be set."
                        )),
                Arguments.of("ClassFile09.jasm",
                        List.of(
                                "\"ACC_VALUE\": If the ACC_INTERFACE flag is not set a value class must have at least one of its ACC_FINAL, ACC_IDENTITY, or ACC_ABSTRACT flags set",
                                "\"ACC_PUBLIC\": Each field of a value class must have at least one of its ACC_STATIC or ACC_STRICT_INIT flags set"
                        )),
                Arguments.of("ClassFile10.jasm",
                        List.of(
                                "\"ACC_FINAL, ACC_ABSTRACT\": Class cannot be both abstract and final"
                        )),
                Arguments.of("ClassFile11.jasm",
                        List.of()),
                Arguments.of("ClassFile12.jasm",
                        List.of()),
                Arguments.of("ClassFile13.jasm",
                        List.of()),
                Arguments.of("ClassFile14.jasm",
                        List.of("Warning: Invalid modifier(s) for a field 0x0800")),
                Arguments.of("ClassFile15.jasm",
                        List.of()),
                Arguments.of("ClassField00.jasm",
                        List.of(
                                "\"ACC_PUBLIC, ACC_PROTECTED, ACC_STATIC\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set",
                                "\"ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_TRANSIENT\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set"
                        )),
                Arguments.of("ClassField01.jasm",
                        List.of(
                                "\"ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_VOLATILE\": Each field of a class must not have both its ACC_FINAL and ACC_VOLATILE flags set")),
                Arguments.of("ClassField02.jasm",
                        List.of(
                                "\"ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_SYNTHETIC, ACC_ENUM\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set",
                                "\"ACC_PUBLIC, ACC_STATIC, ACC_FINAL, ACC_TRANSIENT\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set",
                                "\"ACC_STATIC, ACC_FINAL, ACC_STRICT\": Interface field must be ACC_PUBLIC, ACC_STATIC, and ACC_FINAL only and may have ACC_STRICT_INIT or ACC_SYNTHETIC flag set")),
                Arguments.of("ClassField03.jasm",
                        List.of()),
                Arguments.of("ClassField04.jasm",
                        List.of("\"ACC_PUBLIC, ACC_FINAL\": Each field of a value class must have at least one of its ACC_STATIC or ACC_STRICT_INIT flags set",
                                "\"\": Each field of a value class must have at least one of its ACC_STATIC or ACC_STRICT_INIT flags set"))
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(this.getClass().getResource("ClassFile00.jasm").getFile()).getParentFile();
    }

    @ParameterizedTest
    @MethodSource("getTestParameters")
    public void methodSignatureTest(String resourceName, List<String> jasmSubStrings) {
        // jasm to class on the disk
        LogAndBinResults res = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        String log = res.log.toString();
        for (String jasmSubString : jasmSubStrings) {
            Assertions.assertTrue(log.contains(jasmSubString));
        }
        // exact number of warnings
        Assertions.assertEquals(jasmSubStrings.size(), funcSubStrCount.apply(log, "Warning:"));
        // jasm file was created
        Assertions.assertEquals(0, res.result);
    }
}
