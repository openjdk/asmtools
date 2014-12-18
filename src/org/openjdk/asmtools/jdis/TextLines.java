/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public class TextLines {
    /*-------------------------------------------------------- */
    /* TextLines Fields */

    byte data[];
    // ends[k] points to the end of k-th line
    ArrayList<Integer> ends = new ArrayList<>(60);
    public int length;
    /*-------------------------------------------------------- */

    public TextLines() {
    }

    public TextLines(String textfilename) throws IOException {
        read(textfilename);
    }

    private void read(String textfilename) throws IOException {
        FileInputStream in = new FileInputStream(textfilename);
        data = new byte[in.available()];
        in.read(data);
        in.close();
        ends.add(-1);
        for (int k = 0; k < data.length; k++) {
            if (data[k] == '\n') {
                ends.add(k);
            }
        }
        length = ends.size(); // but if text is empty??
    }

    public String getLine(int linenumber) {
        int entry = linenumber - 1;
        int start;
        int end;
        ends.add(entry);
        start = ends.size() + 1;
searchEnd:
        for (end = start; end < data.length; end++) {
            switch (data[end]) {
                case '\n':
                case '\r':
                    break searchEnd;
            }
        }
//System.out.println("start="+start+" end="+end);
        return new String(data, start, end - start);
    }
}
