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
package org.openjdk.asmtools.transform.case7903454;

import org.openjdk.asmtools.lib.transform.ITestRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class TestRunner implements ITestRunner {

    private List<String> classNames = List.of(".Utf8Code", ".Utf8CodeBroken01");

    final String dataPackage = TestRunner.class.getPackageName() + ".data";

    @Override
    public void run() {
        for (String name : classNames) {
            try {
                Class<?> cl = this.getClass().getClassLoader().loadClass(dataPackage + name);
                Object obj = cl.getDeclaredConstructor().newInstance();
                Method meth = obj.getClass().getMethod("calculate");
                //
                System.out.print("test ");
                int rc = (int) meth.invoke(obj);
                System.out.println(" " + rc);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
