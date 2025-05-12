/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * LocalTable_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 local_table_length;
 * {    u2 start_pc;
 * u2 length;
 * u2 name_index;
 * u2 descriptor/signature_index;
 * u2 index;
 * } local_table[local__table_length];
 * }
 */
public abstract class LocalData<M extends MemberData<?>> extends Indenter {
    protected short start_pc, length, name_cpx, sig_cpx, slot;
    protected ConstantPool pool;
    protected String header;
    protected String format;

    public LocalData(MemberData<M> owner, DataInputStream in, MethodData methodData, String fieldFacet) throws IOException {
        start_pc = in.readShort();
        length = in.readShort();
        name_cpx = in.readShort();
        sig_cpx = in.readShort();
        slot = in.readShort();
        super.toolOutput = methodData.toolOutput;
        pool = methodData.getConstantPool();
        int shift = methodData.calculateInlinedTitleShift("Start");
        header = (MessageFormat.format("%{0}s  Length  Slot  Name    %s", shift)).
                formatted("Start", fieldFacet);
        format = MessageFormat.format("%{0}d  %6d  %4d  %4s  %s", shift);
    }

    @Override
    public int getCommentOffset() {
        return super.getCommentOffset() + PROGRAM_COUNTER_PLACEHOLDER_LENGTH;
    }

    @Override
    protected void tablePrint(int index, int size) throws IOException {
        incIndent();
        if (index == 0) {
            printIndentLn(getTitle());
            printIndentLn(header);
        }
        String nameCpx = PadRight("#%s".formatted(name_cpx), 6);
        String name = pool.StringValue(name_cpx);
        String type = pool.StringValue(sig_cpx);
        if (printCPIndex) {
            if (skipComments) {
                printIndentLn(format.formatted(start_pc, length, slot, nameCpx, "#%s".formatted(sig_cpx)));
            } else {
                String str = format.formatted(start_pc, length, slot, nameCpx, "%-4s".formatted("#" + sig_cpx));
                printIndent(PadRight(str, getCommentOffset()));
                println(" // %s:%s".formatted(name, type));
            }
        } else {
            printIndentLn(format.formatted(start_pc, length, slot, PadRight(name, 6), type));
        }
    }
}
