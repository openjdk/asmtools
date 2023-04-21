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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openjdk.asmtools.lib.action.EAsmTools.JDEC;
import static org.openjdk.asmtools.lib.action.EAsmTools.JDIS;
import static org.openjdk.asmtools.ext.CaptureSystemOutput.Kind.*;
import static org.openjdk.asmtools.lib.transform.TransformLoader.TransformRules.*;

/**
 * Tests JDK-8302260 VarHandle.describeConstable() fails to return a nominal descriptor for static public fields
 */
class case8302260Tests extends ResultChecker {

    private final List<String> goldList = List.of(
            "0: stringField 1: ITestInterfaceA 2: ITestInterfaceA 3: ITestInterfaceA 4: ITestInterfaceA",
            "0: longField 1: 10 2: 10 3: 10 4: 10",
            "0: stringField2 1: CTestSuperClass2 2: CTestSuperClass2 3: CTestSuperClass2 4: CTestSuperClass2",
            "0: longField2 1: 102 2: 102 3: 102 4: 102",
            "0: stringField3 1: ITestInterfaceA3 2: ITestInterfaceA3 3: ITestInterfaceA3 4: ITestInterfaceA3",
            "0: longField3 1: 13 2: 13 3: 13 4: 13"
    );

    private TransformLoader transformLoader;

    @BeforeEach
    public void setClassLoader() {
        transformLoader = new TransformLoader(case8302260Tests.class.getClassLoader()).
                setTransformFilter(className -> className.contains("org.openjdk.asmtools.transform.case8302260.")).
                clearOptions().setDeleteInterimFile(false).setDEBUG(false);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_JCOD_TO_CLASS_LOAD(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(JCOD_TO_CLASS_LOAD);
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_JASM_TO_CLASS_LOAD(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(JASM_TO_CLASS_LOAD);
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_CLASS_LOAD(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(CLASS_LOAD);
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_CLASS_TO_JCOD_TO_CLASS_LOAD(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(CLASS_TO_JCOD_TO_CLASS_LOAD);
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_CLASS_TO_JCOD_TO_CLASS_LOAD_DETAILED(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(CLASS_TO_JCOD_TO_CLASS_LOAD).setToolsOptions(JDEC, "-g");
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_CLASS_TO_JASM_TO_CLASS_LOAD(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(CLASS_TO_JASM_TO_CLASS_LOAD);
        commonTestCase(outputCapture, transformLoader);
    }

    @Test
    @CaptureSystemOutput(value = BOTH, mute = true)
    void systemOutputCheck_CLASS_TO_JASM_TO_CLASS_LOAD_DETAILED(CaptureSystemOutput.OutputCapture outputCapture) {
        transformLoader.setTransformRule(CLASS_TO_JASM_TO_CLASS_LOAD).setToolsOptions(JDIS, "-g");
        commonTestCase(outputCapture, transformLoader);
    }

    private void commonTestCase(final CaptureSystemOutput.OutputCapture capture, final TransformLoader loader) {
        try {
            Class<?> cl = loader.loadClass("org.openjdk.asmtools.transform.case8302260.TestRunner", true);
            setTestRunClass(cl);
            run();
        } catch (Exception ex) {
            String msg = capture.release().useStringTransformer(this::getPurifiedString).getLogAsString(ERROR);
            System.err.println(msg);
            fail(ex);
        }
        assertArrayEquals(goldList.toArray(), capture.useListTransformer(this::getFilteredList).getLogAsArray(OUTPUT));
        assertThat(capture.getLogAsString(ERROR), emptyString());
    }
}
