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
package org.openjdk.asmtools.common;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

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
}
