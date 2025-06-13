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
package org.openjdk.asmtools.attribute.LoadableDescriptors;

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
public class LoadableDescriptorsTests {

    private Jasm jasm = new Jasm();
    private Jcoder jcoder = new Jcoder();
    private File resourceDir;

    private static Stream<Arguments> getJasmParameters() {
        return Stream.of(
                Arguments.of("Test01.jasm", EToolArguments.JDIS_G_T, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                                matchesPattern(".*const #\\d = Utf8 \"LLoadableDescriptors01;\";.*"),
                                                matchesPattern(".*const #\\d = Utf8 \"LLoadableDescriptors02;\";.*"),
                                                matchesPattern(".*LoadableDescriptors #\\d, #\\d; // \"LLoadableDescriptors01;\", \"LLoadableDescriptors02;\".*")
                                        )
                                ),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(10, funcSubStrCount.apply(text, "LoadableDescriptors"))
                        )
                ),
                Arguments.of("LoadableDescriptorsAttributeTest$X.jasm", EToolArguments.JDIS, List.of(
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(1, funcSubStrCount.apply(text, "LoadableDescriptors ")),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(3, funcSubStrCount.apply(text, "strict "))
                        )
                ),
                Arguments.of("LoadableDescriptorsAttributeTest$X.g.jasm", EToolArguments.JDIS_G_T, List.of(
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(1, funcSubStrCount.apply(text, "LoadableDescriptors ")),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(3, funcSubStrCount.apply(text, "strict "))
                        )
                ),
                Arguments.of("LoadableDescriptorsAttributeTest$X.g.t.jasm", EToolArguments.JDIS_G, List.of(
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(1, funcSubStrCount.apply(text, "LoadableDescriptors ")),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(3, funcSubStrCount.apply(text, "strict "))
                        )
                )
        );
    }

    private static Stream<Arguments> getJcodParameters() {
        return Stream.of(
                Arguments.of("LoadableDescriptorsAttributeTest$X.jcod", EToolArguments.JDEC_G, List.of(
                        (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*Attr\\(#\\d\\d, \\d\\) \\{ // LoadableDescriptors at.*"),
                                        matchesPattern(".*descriptor: LLoadableDescriptorsAttributeTest\\$V3.*"),
                                        matchesPattern(".*descriptor: LLoadableDescriptorsAttributeTest\\$V7.*"),
                                        matchesPattern(".*descriptor: LLoadableDescriptorsAttributeTest\\$V2.*")
                                )
                        ))
                ),
                Arguments.of("LoadableDescriptorsAttributeTest$X.g.jcod", EToolArguments.JDEC, List.of(
                                (Consumer<String>) (text) -> assertThat(text, allOf(
                                        matchesPattern(".*Attr\\(#\\d\\d\\) \\{ // LoadableDescriptors.*"))),
                                (Consumer<String>) (text) ->
                                        Assertions.assertEquals(26, funcSubStrCount.apply(text, "LoadableDescriptors"))
                        )
                )
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(Objects.requireNonNull(this.getClass().
                getResource("Test01.jasm")).getFile()).getParentFile();
    }

    @ParameterizedTest
    @MethodSource("getJasmParameters")
    public void jasmTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        // jasm to class in memory
        // jasm.setDebug(true);
        LogAndBinResults binResult = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        Assertions.assertEquals(0, binResult.result);

        // class to jasm
        LogAndTextResults textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());

        Assertions.assertEquals(0, textResult.result);
        String jasmText = textResult.getResultAsString(Function.identity());
        String normJasmText = funcNormalizeText.apply(jasmText);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(normJasmText);
        }
        // jasm to class
        binResult = jasm.compile(new StringInput(jasmText));
        // class produced correctly
        Assertions.assertEquals(0, binResult.result);
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        // class to jasm
        textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        jasmText = textResult.getResultAsString(Function.identity());
        normJasmText = funcNormalizeText.apply(jasmText);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(normJasmText);
        }
        // class to jcod
        textResult = new Jdec().setArgs(EToolArguments.JDEC_G).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        // jcod to class
        binResult = jcoder.compile(new StringInput(textResult.getResultAsString(Function.identity())));
        Assertions.assertEquals(0, binResult.result);
    }

    @ParameterizedTest
    @MethodSource("getJcodParameters")
    public void jcoderTest(String resourceName, EToolArguments args, List<Consumer<String>> tests) {
        // jcod to class in memory
        LogAndBinResults binResult = jcoder.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        Assertions.assertEquals(0, binResult.result);
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        // class to jcod
        LogAndTextResults textResult = new Jdec().setArgs(args).decode(binResult.getAsByteInput());
        String jcoderText = textResult.getResultAsString(funcNormalizeText);
        Assertions.assertEquals(0, textResult.result);
        for (Consumer<String> testConsumer : tests) {
            testConsumer.accept(jcoderText);
        }
        // class to jasm twice
        textResult = new Jdis().setArgs(EToolArguments.JDIS_G_T_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        textResult = new Jdis().setArgs(EToolArguments.JDIS_GG_NC_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
    }
}
