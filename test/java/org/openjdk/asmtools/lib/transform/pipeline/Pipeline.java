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
package org.openjdk.asmtools.lib.transform.pipeline;

import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.StringLog;

import java.nio.file.Path;
import java.util.function.Function;

public class Pipeline<T, R> {
    static final int SUCCESS = 0;
    static final int INIT_STAGE = 65_535;
    static final String CLASS_EXT = ".class";
    static final String JASM_EXT = ".jasm";
    static final String JCOD_EXT = ".jcod";
    static final String JAVA_EXT = ".java";
    private static final boolean DEBUG = true;
    private static final boolean WARN = true;
    private final Function<T, R> currentStage;

    public Pipeline(Function<T, R> initialStage) {
        this.currentStage = initialStage;
    }

    static void logDebug(String context, String msg) {
        if (DEBUG)
            System.out.printf("DEBUG: <%s> [%s]%n", context, msg);
    }

    static void logWarn(String context, String msg) {
        if (WARN)
            System.out.printf("WARN : <%s> [%s]%n", context, msg);
    }

    static void logWarn(String msg) {
        if (WARN)
            System.out.printf("WARN : [%s]%n", msg);
    }

    public <K> Pipeline<T, K> addStage(Function<R, K> nextStage) {
        return new Pipeline<>(input -> nextStage.apply(currentStage.apply(input)));
    }

    public R execute(T input) {
        return currentStage.apply(input);
    }

    public record Status(String stage, Path file, StringLog log, int toolReturn, ByteOutput byteOutput) {
        public Status(Path file) {
            this(null, file, null, -1, null);
        }

        public Status(StringLog log) {
            this(null, null, log, -1, null);
        }
    }
}