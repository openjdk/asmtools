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
package org.openjdk.asmtools.transform.case7902820;

import org.openjdk.asmtools.transform.lib.ITestRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class TestRunnerNegative implements ITestRunner {

    private final List<String> classNames = List.of("SourceDebugExtensionNegative01", "SourceDebugExtensionNegative02");

    final String dataPackage = TestRunnerNegative.class.getPackageName() + ".data.";

    @Override
    public void run() {
        for (String name : classNames) {
            try {
                this.getClass().getClassLoader().loadClass(dataPackage + name).getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
                /* ignore to be able to analyze stderr */
            }
        }
    }
}
