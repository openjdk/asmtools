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
import static org.openjdk.asmtools.lib.transform.TransformLoader.TransformRules.JASM_TO_CLASS_LOAD;
import static org.openjdk.asmtools.lib.transform.TransformLoader.TransformRules.JCOD_TO_CLASS_LOAD;

/**
 * Tests UTF-8 support and broken UTF8 content of SourceDebugExtension attribute
 * CODETOOLS-7903454 Add Utf8 String and SourceDebugExtension attribute support
 */
public class case7903454Tests extends ResultChecker {

    private TransformLoader transformLoader;

    @BeforeEach
    public void setClassLoader() {
        transformLoader = new TransformLoader(case7903454Tests.class.getClassLoader()).
                setTransformFilter(className -> className.contains("org.openjdk.asmtools.transform.case7903454")).
                addToExcludeList("org.openjdk.asmtools.transform.case7903454.TestRunner"
                );
    }

    @Test
    @CaptureSystemOutput(value = OUTPUT, mute = true)
    void systemOutputCheck_JCOD_TO_CLASS_LOAD_Positive(CaptureSystemOutput.OutputCapture outputCapture) {
        outputCapture.expect(containsString("test 45 AÁBCČDĎEÉĚFGHCIÍJKLMαβγδεζηθικλμνξοπρσςτυφχψω"));
        transformLoader.setTransformRule(JCOD_TO_CLASS_LOAD);
        try {
            Class<?> cl = transformLoader.loadClass("org.openjdk.asmtools.transform.case7903454.TestRunner", true);
            setTestRunClass(cl);
            run();
        } catch (Exception ex) {
            String msg = outputCapture.release().useStringTransformer(this::getPurifiedString).getLogAsString(ERROR);
            System.err.println(msg);
            fail(ex);
        }
    }

    @Test
    @CaptureSystemOutput(value = OUTPUT, mute = true)
    void systemOutputCheck_JASM_TO_CLASS_LOAD_Positive(CaptureSystemOutput.OutputCapture outputCapture) {
        outputCapture.expect(containsString("test 45 AÁBCČDĎEÉĚFGHCIÍJKLMαβγδεζηθικλμνξοπρσςτυφχψω"));
        transformLoader.setTransformRule(JASM_TO_CLASS_LOAD);
        try {
            Class<?> cl = transformLoader.loadClass("org.openjdk.asmtools.transform.case7903454.TestRunner", true);
            setTestRunClass(cl);
            run();
        } catch (Exception ex) {
            String msg = outputCapture.release().useStringTransformer(this::getPurifiedString).getLogAsString(ERROR);
            System.err.println(msg);
            fail(ex);
        }
    }
}
