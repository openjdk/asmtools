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
package org.openjdk.asmtools.attribute.BootstrapMethods.data;

import java.lang.invoke.*;

public class BootstrapExample01 {

    public static int main(String[] args) throws Throwable {
        // Dynamically resolve and invoke a method
        MethodHandle dynamicHandle = getDynamicInvoker();
        // Expected multiline output:
        // Bootstrap method called with:
        // Lookup: BootstrapExample01
        // Method Name: dynamicInvoke
        // Method Type: (String)void
        // Target method called with message: Hello, BootstrapExample01!
        dynamicHandle.invokeExact("Hello, BootstrapExample01!");
        return 1;
    }

    // Bootstrap method - this defines the linkage for invokedynamic
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType) throws Throwable {
        System.out.println("Bootstrap method called with:");
        System.out.println("Lookup: " + lookup);
        System.out.println("Method Name: " + methodName);
        System.out.println("Method Type: " + methodType);

        // Resolve the target method (example: static method in this class)
        MethodHandle targetHandle = lookup.findStatic(BootstrapExample01.class, "targetMethod", methodType);
        return new ConstantCallSite(targetHandle);
    }

    // Target method to be invoked dynamically
    public static void targetMethod(String message) {
        System.out.println("Target method called with message: " + message);
    }

    // Invokedynamic simulation for demonstration
    public static MethodHandle getDynamicInvoker() throws Throwable {
        MethodType bootstrapMethodType = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
        MethodHandle bootstrapHandle = MethodHandles.lookup().findStatic(BootstrapExample01.class, "bootstrap", bootstrapMethodType);

        // Define the signature of the invokedynamic target
        MethodType targetMethodType = MethodType.methodType(void.class, String.class);
        CallSite callSite = (CallSite) bootstrapHandle.invoke(MethodHandles.lookup(), "dynamicInvoke", targetMethodType);
        return callSite.dynamicInvoker();
    }
}
