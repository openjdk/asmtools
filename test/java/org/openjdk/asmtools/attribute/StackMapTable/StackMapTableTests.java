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
package org.openjdk.asmtools.attribute.StackMapTable;

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
import java.util.function.Function;
import java.util.stream.Stream;

import static org.openjdk.asmtools.lib.utility.StringUtils.funcSubStrCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StackMapTableTests {
    private Jasm jasm = new Jasm();
    private Jcoder jcoder = new Jcoder();
    private File resourceDir;


    private static Stream<Arguments> getJasmParameters() {
        return Stream.of(
                Arguments.of("StackMapData.g.jasm", EToolArguments.JDIS_G, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "unset_fields") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "stack_map") == 6)
                ),
                Arguments.of("StackMapData.g.t.jasm", EToolArguments.JDIS_G_T, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "number_of_entries") == 4,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "offset_delta") == 15)
                ),
                Arguments.of("StackMapData.jasm", EToolArguments.JDIS, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "unset_fields;") == 4)
                ),
                Arguments.of("StackMapData.t.jasm", EToolArguments.JDIS_T, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "frame_type") == 54)
                ),
                Arguments.of("StackMapData01.g.jasm", EToolArguments.JDIS_G, List.of(
                        (Function<String, Boolean>) (text) ->
                                funcSubStrCount.apply(text, "The ConstantPool[15] must be a CONSTANT_NameAndType_info") == 1)
                ),
                Arguments.of("StackMapData02.g.jasm", EToolArguments.JDIS_T, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval") == 13,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "frame_type") == 39)
                )

        );
    }

    private static Stream<Arguments> getJcodParameters() {
        return Stream.of(
                Arguments.of("StackMapData.g.jcod", EToolArguments.JDEC, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval_frame") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "246b,") == 14
                )),
                Arguments.of("StackMapData.jcod", EToolArguments.JDEC_G, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval_frame") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "246b,") == 14,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "[25]") == 1
                )),
                Arguments.of("StackMapData02.g.jcod", EToolArguments.JDEC_G, List.of(
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "early_larval_frame") == 13,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "246b,") == 13,
                        (Function<String, Boolean>) (text) -> funcSubStrCount.apply(text, "[11]") == 1
                ))
        );
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(Objects.requireNonNull(this.getClass().getResource("StackMapData.g.jasm")).getFile()).getParentFile();
    }

    @ParameterizedTest
    @MethodSource("getJasmParameters")
    public void jasmTest(String resourceName, EToolArguments args, List<Function<String, Boolean>> tests) {
        // jasm to class in memory
        LogAndBinResults binResult = jasm.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        Assertions.assertEquals(0, binResult.result);
        // class to jasm
        LogAndTextResults textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        String jasmText = textResult.getResultAsString(Function.identity());
        for (Function<String, Boolean> testFunction : tests) {
            Assertions.assertTrue(testFunction.apply(jasmText));
        }
        // jasm to class
        binResult = jasm.compile(new StringInput(jasmText));
        // class produced correctly
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        Assertions.assertEquals(0, binResult.result);
        // class to jasm
        textResult = new Jdis().setArgs(args).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        jasmText = textResult.getResultAsString(Function.identity());
        for (Function<String, Boolean> testFunction : tests) {
            Assertions.assertTrue(testFunction.apply(jasmText));
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
    public void jcoderTest(String resourceName, EToolArguments args, List<Function<String, Boolean>> tests) {
        // jcod to class in memory
        LogAndBinResults binResult = jcoder.compile(List.of(resourceDir + File.separator + resourceName));
        // class produced correctly
        Assertions.assertEquals(0, binResult.result);
        Assertions.assertTrue(binResult.log.toString().isEmpty());
        // class to jcod
        LogAndTextResults textResult = new Jdec().setArgs(args).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        for (Function<String, Boolean> testFunction : tests) {
            Assertions.assertTrue(testFunction.apply(textResult.getResultAsString(Function.identity())));
        }
        // class to jasm twice
        textResult = new Jdis().setArgs(EToolArguments.JDIS_G_T_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
        textResult = new Jdis().setArgs(EToolArguments.JDIS_GG_NC_LNT_LVT).decode(binResult.getAsByteInput());
        Assertions.assertEquals(0, textResult.result);
    }
}
