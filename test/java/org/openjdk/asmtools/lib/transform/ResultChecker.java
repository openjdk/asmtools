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
package org.openjdk.asmtools.lib.transform;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResultChecker {

    public static String[] OUT_LINE_PREFIXES_TO_IGNORE = new String[]{
            ":TransformerLoader: "
    };

    // Filter to ignore Logged output lines
    static Function<String, Boolean> filterOut =
            s -> Arrays.stream(OUT_LINE_PREFIXES_TO_IGNORE).anyMatch(p -> s.startsWith(p));

    Class<?> trClass;

    public void run() {
        try {
            Object obj = trClass.getDeclaredConstructor().newInstance();
            //default void run()
            obj.getClass().getMethod("run").invoke(obj);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public ResultChecker setTestRunClass(Class<?> trClass) {
        this.trClass = trClass;
        return this;
    }

    /**
     * Filters out all trace/debug lines from str using Function<String, Boolean> filterOut
     *
     * @param str line that will be split into list by System.lineSeparator()
     * @return list of strings
     */
    protected List<String> getFilteredList(String str) {
        return Arrays.stream(str.split(System.lineSeparator())).filter(s -> !filterOut.apply(s)).toList();
    }

    /**
     * Filters out all trace/debug lines from str using Function<String, Boolean> filterOut
     *
     * @param str line that will be split into list by System.lineSeparator()
     * @return string without lines filtered out
     */
    protected String getFilteredString(String str) {
        return Arrays.stream(str.split(System.lineSeparator())).filter(s -> !filterOut.apply(s)).
                collect(Collectors.joining(System.lineSeparator()));
    }

    protected String getPurifiedString(String str) {
        for (String s : OUT_LINE_PREFIXES_TO_IGNORE) {
            str = str.replaceAll(s, "");
        }
        return str;
    }
}
