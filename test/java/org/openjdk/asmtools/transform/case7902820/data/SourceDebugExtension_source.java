//
// Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//
// Java source to generate InvalidSourceDebugExtension.class.jcod & SourceDebugExtension.class.jcod
//
package org.openjdk.asmtools.transform.case7902820.data;

public class SourceDebugExtension_source {

    public int calculate() {
        int a1 = 1;
        int a2 = 1;
        int a3 = 1;
        try {
            for (int i = a1 / (a2 - a3); i < 5; i++) {
                a1 += 1;
            }
            a2 = 2;
        } catch (ArithmeticException e) {
            a3 = 2;
        }
        if (a1 != 1 || a2 != 1 || a3 != 2) {
            System.out.print("failed");
            return 2;
        }
        System.out.print("passed");
        return 0;
    }
}
