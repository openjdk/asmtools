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
package org.openjdk.asmtools.attribute.SourceDebugExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.script.TestScript;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.AllOf.allOf;
import static org.openjdk.asmtools.lib.utility.StringUtils.funcSubStrCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SourceDebugExtensionTests extends TestScript {

    private static Stream<Arguments> getJasmParameters() {
        return Stream.of(
                Arguments.of("SourceDebugExt01.jasm",
                        EToolArguments.JDIS_G, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        containsString("\"SMAP\\nSourceDebugExt01.java\\nJava\\n*S Java\\n*F\\n+ 1 SourceDebugExt01.java\\n\";"),
                                        containsString("\"SourceDebugExt01.java\\n*L\\n1#1,5:1\\n*E\";"),
                                        matchesPattern(".*const #\\d\\d = Utf8 \"SourceDebugExtension\";.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(2, funcSubStrCount.apply(text, "SourceDebugExtension"))
                        )
                ),
                Arguments.of("SourceDebugExt02.jasm",
                        EToolArguments.JDIS_G_T, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        containsString("SourceDebugExtension { }"),
                                        matchesPattern(".*const #\\d\\d = Utf8 \"SourceDebugExtension\";.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(2, funcSubStrCount.apply(text, "SourceDebugExtension"))
                        )
                ),
                Arguments.of("SourceDebugExt03.jasm",
                        EToolArguments.JDIS, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        containsString("SourceDebugExtension { 0x01 0x02 0x03 0x04 0x05 0x06 0x07 0x08 0x09 0x0A 0x00 0x0B 0x0C; 0x0D 0x0E 0x0F; }")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(1, funcSubStrCount.apply(text, "SourceDebugExtension"))
                        )
                )
        );
    }

    @BeforeAll
    public void init() throws IOException {
        // initialize resource directory
        super.init("SourceDebugExt01.jasm");
        // enable warnings for Jasm tests
        super.enableToolsWarnings();
    }

    @ParameterizedTest
    @MethodSource("getJasmParameters")
    public void jasmTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        super.jasmTest(resourceName, args, tests);
    }
}
