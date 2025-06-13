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
package org.openjdk.asmtools.attribute.NestHost;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.common.inputs.StringInput;
import org.openjdk.asmtools.lib.action.*;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;
import org.openjdk.asmtools.lib.script.TestScript;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.AllOf.allOf;
import static org.openjdk.asmtools.lib.utility.StringUtils.funcNormalizeText;
import static org.openjdk.asmtools.lib.utility.StringUtils.funcSubStrCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NestHostTests extends TestScript {

    private Jasm jasm = new Jasm();
    private Jcoder jcoder = new Jcoder();
    private File resourceDir;

    private static Stream<Arguments> getJasmParameters() {
        return Stream.of(
                Arguments.of("Test01.jasm", EToolArguments.JDIS_G, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*const #\\d = class #\\d; // NestHost01.*"),
                                        matchesPattern(".*NestHost #\\d; // org/openjdk/asmtools/attribute/NestHost/NestHost01.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(11, funcSubStrCount.apply(text, "NestHost"))
                        )
                ),
                Arguments.of("Test01.jasm", EToolArguments.JDIS, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*NestHost org/openjdk/asmtools/attribute/NestHost/NestHost01;.*")
                                ))
                        )
                ),
                Arguments.of("Test01.g.jasm", EToolArguments.JDIS_G_T, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*const #\\d = Class #\\d; // NestHost01.*"),
                                        matchesPattern(".*NestHost #\\d; // org/openjdk/asmtools/attribute/NestHost/NestHost01.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(11, funcSubStrCount.apply(text, "NestHost"))
                        )
                ),
                Arguments.of("Test02.jasm", EToolArguments.JDIS_G, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*const #\\d = class #\\d; // NestHost02.*"),
                                        matchesPattern(".*NestHost #\\d; // org/openjdk/asmtools/attribute/NestHost/NestHost02.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(11, funcSubStrCount.apply(text, "NestHost"))
                        )
                ),
                Arguments.of("Test02.jasm", EToolArguments.JDIS, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*NestHost org/openjdk/asmtools/attribute/NestHost/NestHost02;.*")
                                ))
                        )
                )
        );
    }

    private static Stream<Arguments> getJcodParameters() {
        return Stream.of(
                Arguments.of("Test01.jcod", EToolArguments.JDEC_G, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*#\\d; // class: org/openjdk/asmtools/attribute/NestHost/NestHost01.*"),
                                        matchesPattern(".*Attr.#\\d, 2. \\{ // NestHost.*")
                                )),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(11, funcSubStrCount.apply(text, "NestHost"))
                        )
                )
        );
    }

    @BeforeAll
    public void init() throws IOException {
        // initialize resource directory
        super.init("Test01.jasm");
    }

    @ParameterizedTest
    @MethodSource("getJasmParameters")
    public void jasmTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        super.jasmTest(resourceName, args, tests);
    }

    @ParameterizedTest
    @MethodSource("getJcodParameters")
    public void jcoderTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        super.jcoderTest(resourceName, args, tests);
    }
}
