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
package org.openjdk.asmtools.lib.utility;

import org.openjdk.asmtools.lib.action.DebugHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public class StringUtils {



    public static BiFunction<String, String, Long> funcSubStrCount = (text, subStr) -> {
        Long count =IntStream.range(0, text.length() - subStr.length() + 1)
                .filter(i -> text.substring(i, i + subStr.length()).equals(subStr))
                .count();
        if(DebugHelper.isDebug()) {
            System.out.println("count(\"%s\") = %d".formatted(subStr, count));
        }
        return count;
    };

    public static Function<String, String> funcNormalizeText = s -> s.replaceAll("[\\r\\n]+", "")
            .replaceAll("\\t", " ").replaceAll(" {2,}", " ");

    public static List<String> substrBetween(final String str, final String startStr, final String endStr) {
        if (isEmpty(str) || isEmpty(startStr) || isEmpty(endStr)) {
            return new ArrayList<>();
        }
        final int strLen = str.length();
        final int startLen = endStr.length();
        final int endLen = startStr.length();
        final List<String> list = new ArrayList<>();
        int pos = 0;
        while (pos < strLen - startLen) {
            int start = str.indexOf(startStr, pos);
            if (start < 0)
                break;
            start += endLen;
            final int end = str.indexOf(endStr, start);
            if (end < 0)
                break;
            list.add(str.substring(start, end));
            pos = end + startLen;
        }
        return list;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String[] addTail(String[] elements, String... extras) {
        int length = extras.length;
        if (length == 0) {
            return elements;
        }
        String[] result = new String[elements.length + length];
        System.arraycopy(elements, 0, result, 0, elements.length);
        System.arraycopy(extras, 0, result, elements.length, length);
        return result;
    }
}
