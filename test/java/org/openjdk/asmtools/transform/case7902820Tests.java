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
package org.openjdk.asmtools.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.asmtools.ext.CaptureSystemOutput;
import org.openjdk.asmtools.lib.transform.ResultChecker;
import org.openjdk.asmtools.lib.transform.TransformLoader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openjdk.asmtools.ext.CaptureSystemOutput.Kind.ERROR;
import static org.openjdk.asmtools.ext.CaptureSystemOutput.Kind.OUTPUT;
import static org.openjdk.asmtools.lib.transform.TransformLoader.TransformRules.JCOD_TO_CLASS_LOAD;

/**
 * Tests stdout for error messages
 * CODETOOLS-7902820 Asmtools incorrectly uses stdout for output error messages
 */
public class case7902820Tests extends ResultChecker {

    private TransformLoader transformLoader;

    @BeforeEach
    public void setClassLoader() {
        transformLoader = new TransformLoader(case7902820Tests.class.getClassLoader()).
                setTransformFilter(className -> className.contains("org.openjdk.asmtools.transform.case7902820")).
                addToExcludeList("org.openjdk.asmtools.transform.case7902820.TestRunnerPositive",
                        "org.openjdk.asmtools.transform.case7902820.TestRunnerNegative"
                );
    }

    @Test
    @CaptureSystemOutput(value = OUTPUT, mute = true)
    void systemOutputCheck_JCOD_TO_CLASS_LOAD_Positive(CaptureSystemOutput.OutputCapture outputCapture) {
        outputCapture.expect(containsString("test SourceDebugExtensionPositive01 passed 0"));
        outputCapture.expect(containsString("test SourceDebugExtensionPositive02 passed 0"));
        transformLoader.setTransformRule(JCOD_TO_CLASS_LOAD);
        try {
            Class<?> cl =
                    transformLoader.loadClass("org.openjdk.asmtools.transform.case7902820.TestRunnerPositive", true);
            setTestRunClass(cl);
            run();
        } catch (Exception ex) {
            String msg = outputCapture.release().useStringTransformer(this::getPurifiedString).getLogAsString(ERROR);
            System.err.println(msg);
            fail(ex);
        }
    }

    @Test
    @CaptureSystemOutput(value = ERROR, mute = true)
    void systemOutputCheck_JCOD_TO_CLASS_LOAD_Negative(CaptureSystemOutput.OutputCapture errorCapture) {
        transformLoader.setTransformRule(JCOD_TO_CLASS_LOAD);
        errorCapture.expect(
                containsString("Warning: expected attribute length \"2,147,483,647\" do not match real" +
                        " length \"6\"; expected length written"));
        errorCapture.expect(
                containsString("Warning: expected attribute length \"2\" do not match real length \"6\";" +
                        " expected length written"));
        errorCapture.expect(
                containsString("java.lang.ClassFormatError: Extra bytes at the end of class file" +
                        " org/openjdk/asmtools/transform/case7902820/data/SourceDebugExtensionNegative01"));
        errorCapture.expect(
                containsString("java.lang.ClassFormatError: Extra bytes at the end of class file" +
                        " org/openjdk/asmtools/transform/case7902820/data/SourceDebugExtensionNegative02"));
        try {
            Class<?> cl = transformLoader.
                    loadClass("org.openjdk.asmtools.transform.case7902820.TestRunnerNegative", true);
            setTestRunClass(cl);
            run();
        } catch (Exception ignored) { /* ignore to be able to analyze stderr */ }
    }
}
