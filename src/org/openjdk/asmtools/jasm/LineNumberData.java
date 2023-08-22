/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jasm;

import java.io.IOException;

/**
 * 4.7.12. The LineNumberTable Attribute
 * <p>
 * LineNumberTable_attribute {
 *
 *     {   u2 start_pc;
 *         u2 line_number;
 *     }
 *
 * }
 */
class LineNumberData implements DataWriter {

    int start_pc, line_number;

    public LineNumberData(int start_pc, int line_number) {
        this.start_pc = start_pc;
        this.line_number = line_number;
    }

    @Override
    public int getLength() {
        return 4;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(start_pc);
        out.writeShort(line_number);
    }
}
