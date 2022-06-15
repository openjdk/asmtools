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
 * 4.7.3. The Code Attribute
 * <p>
 *Code_attribute {
 *
 *     {   u2 start_pc;
 *         u2 end_pc;
 *         u2 handler_pc;
 *         u2 catch_type;
 *     }
 *
 * }
 */
class ExceptionData implements DataWriter {

    int pos;
    CodeAttr.RangePC rangePC;
    int handler_pc;
    Indexer catchType;

    public ExceptionData(int pos, CodeAttr.RangePC rangePC, int handler_pc, Indexer catchType) {
        this.pos = pos;
        this.rangePC = rangePC;
        this.handler_pc = handler_pc;
        this.catchType = catchType;
    }

    @Override
    public int getLength() {
        return 8; // add the length of number of elements
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        out.writeShort(rangePC.start_pc);
        out.writeShort(rangePC.end_pc);
        out.writeShort(handler_pc);
        if (catchType.isSet()) {
            out.writeShort(catchType.cpIndex);
        } else {
            out.writeShort(0);
        }
    }
}