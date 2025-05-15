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
package org.openjdk.asmtools.attribute.BootstrapMethods;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.openjdk.asmtools.lib.action.EAsmTool;
import org.openjdk.asmtools.lib.action.EToolArguments;
import org.openjdk.asmtools.lib.action.Jdis;
import org.openjdk.asmtools.lib.attributes.Attribute;
import org.openjdk.asmtools.lib.ext.CaptureSystemOutput;
import org.openjdk.asmtools.lib.log.LogAndReturn;

import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.openjdk.asmtools.lib.ext.CaptureSystemOutput.Kind.OUTPUT;

public class BootstrapMethodsAttributeTests {

    private static Jdis jdis = new Jdis();

    @ParameterizedTest
    @ArgumentsSource(BootstrapMethodsAttributeProvider.class)
    @CaptureSystemOutput(value = OUTPUT, mute = true)
    void testOutput(Attribute data, CaptureSystemOutput.OutputCapture outputCapture) throws IOException {
        outputCapture.expect(containsString(data.getClassName()));
        int rc = data.run();
        Assertions.assertEquals(data.getExpectedRc(), rc);
        for (EToolArguments args : EToolArguments.ofTool(EAsmTool.JDIS)) {
            String fname = data.getSimpleClassName() + args.getPostfix();
            LogAndReturn res = jdis.setArgs(args).decode(data.getContent());
        }
    }

    @BeforeAll
    public static void setUp() throws IOException {
        jdis.setDestDir().setDebug(false);
    }

    @AfterAll
    public static void tearDown() throws IOException {
    }
}

class BootstrapMethodsAttributeProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        String packageName = BootstrapMethodsAttributeTests.class.getPackage().getName();
        return Stream.of(
                Arguments.of(new Attribute(Attribute.Kind.CLASS_MAIN, packageName.concat(".data.BootstrapExample01"), 1)),
                Arguments.of(new Attribute(Attribute.Kind.CLASS_MAIN, packageName.concat(".data.BootstrapExample02"), 4))
        );
    }
}
