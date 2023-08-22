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
package org.openjdk.asmtools.transform.case8302260;

import org.openjdk.asmtools.lib.transform.ITestRunner;
import org.openjdk.asmtools.transform.case8302260.data.CTestClass;

import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_long;

public class TestRunner implements ITestRunner {

    final String superPackage = TestRunner.class.getPackageName() + ".data";

    @Override
    public void run() {
        VarHandle vh;
        // Chapter 5.4.3.2. Field Resolution Otherwise, field lookup is applied recursively
        // to the direct superinterfaces of the specified class or interface C.
        vh = testFieldRef(CTestClass.class, "stringField", String.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "stringField", CD_String);
        vh = testFieldRef(CTestClass.class, "longField", long.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "longField", CD_long);
        // Otherwise, if C has a superclass S, field lookup is applied recursively to S.
        vh = testFieldRef(CTestClass.class, "stringField2", String.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "stringField2", CD_String);
        vh = testFieldRef(CTestClass.class, "longField2", long.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "longField2", CD_long);
        //
        vh = testFieldRef(CTestClass.class, "stringField3", String.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "stringField3", CD_String);
        vh = testFieldRef(CTestClass.class, "longField3", long.class);
        testFieldValue(vh, ClassDesc.of(superPackage, "CTestClass"), "longField3", CD_long);
    }

    void testFieldValue(VarHandle vh, ClassDesc desc, String name, ClassDesc fdesc) {
        if (vh != null) {
            try {
                System.out.print(" 1: " + vh.get());
            } catch (Throwable e) {
                System.out.println(" 1: failed");
                e.printStackTrace(System.err);
            }
        }
        try {
            VarHandle vh2 = MethodHandles.lookup().findStaticVarHandle((Class<?>) desc.resolveConstantDesc(MethodHandles.lookup()),
                    name, (Class<?>) fdesc.resolveConstantDesc(MethodHandles.lookup()));
            System.out.print(" 2: " + vh2.get());
        } catch (Throwable e) {
            System.out.println(" 2: failed");
            e.printStackTrace(System.err);
        }
        if (vh != null) {
            try {
                VarHandle.VarHandleDesc vhd = vh.describeConstable().orElseThrow();
                VarHandle obj = vhd.resolveConstantDesc(MethodHandles.lookup());
                System.out.print(" 3: " + obj.get());
            } catch (Throwable e) {
                System.out.println(" 3: failed");
                e.printStackTrace(System.err);
            }
        }
        try {
            VarHandle.VarHandleDesc vhd = VarHandle.VarHandleDesc.ofStaticField(desc, name, fdesc);
            VarHandle obj = vhd.resolveConstantDesc(MethodHandles.lookup());
            System.out.println(" 4: " + obj.get());
        } catch (Throwable t) {
            System.out.println(" 4: failed");
            t.printStackTrace(System.out);
        }
    }

    VarHandle testFieldRef(Class<?> rec, String fname, Class<?> type) {
        VarHandle vh = null;
        try {
            vh = MethodHandles.lookup().findStaticVarHandle(rec, fname, type);
            System.out.print("0: " + fname);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.out.println("0: failed");
            e.printStackTrace(System.err);
        }
        return vh;
    }
}
