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
package org.openjdk.asmtools.jdec.case7902055;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openjdk.asmtools.lib.action.*;
import org.openjdk.asmtools.lib.log.LogAndBinResults;
import org.openjdk.asmtools.lib.log.LogAndTextResults;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openjdk.asmtools.common.Environment.OK;

/**
 * This is the test for the issue: CODETOOLS-7902055 (https://bugs.openjdk.org/browse/CODETOOLS-7902055)
 * "Preserve module name in jdec output"
 * dec output doesn't preserve the name of the module at the start of the
 * declaration.
 *
 * Example:
 * -------------------------------
 * $ cat module-info.jcod
 * module foo.bar {
 * } // end module foo.bar
 *
 * $ java -jar asmtools.jar jcoder -d out module-info.jcod
 * $ java -jar asmtools.jar jdec out/module-info.class > module-info.jcod.after
 * $ diff module-info.jcod module-info.jcod.after
 * 1c1
 * < module foo.bar {
 * ---
 * > module {
 * -------------------------------
 *
 * Note that the module name in the end comment ("end module XXX") is
 * preserved.
 *
 * The name given at the start of the module doesn't seem to have any effect on the .class output,
 * but it would be nice to preserve it for readability reasons.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Tests {
    private final Jcoder jcoder = new Jcoder();

    private File resourceDir;

    private static Stream<Arguments> getJCodParameters() {
        return Stream.of(
                Arguments.of("module-info.g.jcod", "\\s*module\\s+moduleB\\s*\\{"),
                Arguments.of("module-info.jcod", "\\s*module\\s+moduleB\\s*\\{"));
    }

    @BeforeAll
    public void init() throws IOException {
        resourceDir = new File(Objects.requireNonNull(this.getClass().
                getResource("module-info.jcod")).getFile()).getParentFile();
    }

    // jcod -> .class -> jcod round trip should preserve the module name
    @ParameterizedTest
    @MethodSource("getJCodParameters")
    public void testJCoderJdecRoundTrip(String resourceName, String jcodRegex) {
        // jcod to class
        final LogAndBinResults binResult = jcoder.compile(List.of(resourceDir + File.separator + resourceName));
        Assertions.assertEquals(OK, binResult.result);
        // class to jcod
        for ( EToolArguments arg : List.of(EToolArguments.JDEC, EToolArguments.JDEC_G)) {
            LogAndTextResults textResult = new Jdec().setArgs(arg).decode(binResult.getAsByteInput());
            Assertions.assertEquals(OK, textResult.result);
            // expected substrings
            String jcodOutput = textResult.output.toString();
            jcodOutput = jcodOutput.substring(0, jcodOutput.indexOf("{")+1);
            assertThat(jcodOutput, Matchers.matchesRegex(jcodRegex));
        }
    }
}
