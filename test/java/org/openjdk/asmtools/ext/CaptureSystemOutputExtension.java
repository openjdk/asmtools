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
package org.openjdk.asmtools.ext;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.openjdk.asmtools.ext.CaptureSystemOutput.OutputCapture;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

class CaptureSystemOutputExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private CaptureSystemOutput.Kind kind;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        Optional<Method> method = context.getTestMethod();
        if (method.isPresent()) {
            CaptureSystemOutput ann = method.get().getAnnotation(CaptureSystemOutput.class);
            if (ann != null) {
                kind = ann.value();
                getOutputCapture(context).capture(ann.value(), ann.mute());
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        OutputCapture outputCapture = getOutputCapture(context);
        try {
            CaptureSystemOutput.Kind k = this.kind == null ? CaptureSystemOutput.Kind.BOTH : this.kind;
            // expect is used
            if (!outputCapture.strMatchers.isEmpty()) {
                String string = outputCapture.getLogAsString(kind);
                assertThat(string, allOf(outputCapture.strMatchers));
            }
            // array is used
            if (!outputCapture.listMatchers.isEmpty()) {
                List<String> list = outputCapture.getLogAsList(kind);
                assertThat(list, allOf(outputCapture.listMatchers));
            }
        } finally {
            outputCapture.release();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        boolean isTestMethodLevel = extensionContext.getTestMethod().isPresent();
        boolean isOutputCapture = parameterContext.getParameter().getType() == OutputCapture.class;
        return isTestMethodLevel && isOutputCapture;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return getOutputCapture(extensionContext);
    }

    private OutputCapture getOutputCapture(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(OutputCapture.class);
    }

    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}
