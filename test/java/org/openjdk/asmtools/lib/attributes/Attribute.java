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
package org.openjdk.asmtools.lib.attributes;

import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.lib.action.Jasm;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class Attribute {

    static MethodHandles.Lookup lookup = MethodHandles.lookup();

    Jasm jasm = null;

    private final Attribute.Kind kind;
    private final int expectedRc;
    private String[] classParams = new String[0];

    private String attributeName;
    private String className;
    private String resourceName;
    private String outputFileName;

    public Attribute(Attribute.Kind kind, String className, int expectedRc) {
        this.kind = kind;
        this.className = className;
        this.resourceName = "/".concat(className.replace('.', '/')).concat(".class");
        this.expectedRc = expectedRc;
        this.attributeName = this.getClass().getSimpleName();
        int ind = attributeName.lastIndexOf("Attribute");
        if (ind != -1) {
            attributeName = attributeName.substring(0, ind);
        }
    }

    public String getSimpleClassName() {
        int ind = className.lastIndexOf(".");
        return (ind != -1) ? className.substring(ind + 1) : className;
    }

    public String getClassName() {
        return className;
    }

    public int getExpectedRc() {
        return expectedRc;
    }

    public ToolInput getContent() {
        byte[] bytes = null;
        try {
            bytes = getClass().getResourceAsStream(resourceName).readAllBytes();
        } catch (IOException e) {
            fail("Can't read the class  %s\ndue to %s".formatted(className,
                    e.toString()));
        }
        return new ByteInput(bytes);
    }

    public int run() {
        int returnCode = -1;
        try {
            Class<?> clazz = Class.forName(className);
            MethodHandle methodHandle;
            if (kind == Attribute.Kind.CLASS_MAIN || kind == Attribute.Kind.CLASS_RUN) {
                methodHandle = Attribute.lookup.findStatic(clazz, kind.methodName, kind.methodType);
                returnCode = (Integer) methodHandle.invoke(this.classParams);
            } else {
                methodHandle = Attribute.lookup.findVirtual(clazz, kind.methodName, kind.methodType);
                // Create an instance of the class
                Object instance = clazz.getDeclaredConstructor().newInstance();
                // Invoke the method (non-static, so pass the instance)
                returnCode = (Integer) methodHandle.invoke(instance, this.classParams);
            }
        } catch (Throwable e) {
            fail("Can't run %s::%s\ndue to %s".formatted(className,
                    kind.toString(),
                    e.toString()));
        }
        return returnCode;
    }

    public enum Kind {
        CLASS_MAIN("main", MethodType.methodType(int.class, String[].class)),
        CLASS_RUN("run", MethodType.methodType(int.class, String[].class)),
        INSTANCE_RUN("run", MethodType.methodType(int.class, String[].class));
        final String methodName;
        final MethodType methodType;

        Kind(String methodName, MethodType methodType) {
            this.methodName = methodName;
            this.methodType = methodType;
        }

        @Override
        public String toString() {
            return "%s %s(%s)".formatted(methodType.returnType().getCanonicalName(),
                    methodName,
                    Arrays.stream(methodType.parameterArray()).
                            map(clazz -> clazz.getCanonicalName()).collect(Collectors.joining(", ")));
        }
    }
}
