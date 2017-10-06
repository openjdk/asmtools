/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.asmutils.HexUtils;
import static org.openjdk.asmtools.jasm.Tables.*;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * represents one entry of StackMap attribute
 */
class StackMapData {
    /*-------------------------------------------------------- */
    /* StackMapData Fields */

    static int prevFramePC = 0;
    boolean isStackMapTable = false;
    StackMapFrameType stackFrameType = null;
    int start_pc;
    int[] lockMap;
    int[] stackMap;
    /*-------------------------------------------------------- */

    public StackMapData() {
    }

    public StackMapData(CodeData code, DataInputStream in) throws IOException {
        start_pc = in.readUnsignedShort();
        TraceUtils.trace("      stack_map_entry:pc=" + start_pc);
        TraceUtils.trace(" numloc=");
        lockMap = readMap(code, in);
        TraceUtils.trace(" numstack=");
        stackMap = readMap(code, in);
        TraceUtils.traceln("");
    }

    public StackMapData(CodeData code, DataInputStream in,
            boolean isStackMapTable) throws IOException {
        this.isStackMapTable = isStackMapTable;
        int ft_val = in.readUnsignedByte();
        StackMapFrameType frame_type = stackMapFrameType(ft_val);
        int offset = 0;
        switch (frame_type) {
            case SAME_FRAME:
                // type is same_frame;
                TraceUtils.trace("      same_frame=" + ft_val);
                TraceUtils.traceln("");
                offset = ft_val;
                break;
            case SAME_FRAME_EX:
                // type is same_frame_extended;
                TraceUtils.trace("      same_frame_extended=" + ft_val);
                TraceUtils.traceln("");
                offset = in.readUnsignedShort();
                TraceUtils.trace(" offset=" + offset);
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                // type is same_locals_1_stack_item_frame
                TraceUtils.trace("      same_locals_1_stack_item_frame=" + ft_val);
                offset = ft_val - 64;
                TraceUtils.trace(" offset=" + offset);
                // read additional single stack element
                TraceUtils.trace(" numstack=");
                stackMap = readMapElements(code, in, 1);
                TraceUtils.traceln("");
                break;
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                // type is same_locals_1_stack_item_frame_extended
                TraceUtils.trace("      same_locals_1_stack_item_frame_extended=" + ft_val);
                offset = in.readUnsignedShort();
                TraceUtils.trace(" offset=" + offset);
                // read additional single stack element
                TraceUtils.trace(" numstack=");
                stackMap = readMapElements(code, in, 1);
                TraceUtils.traceln("");
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
                // type is chop_frame
                TraceUtils.trace("      chop_frame=" + ft_val);
                TraceUtils.traceln("");
                offset = in.readUnsignedShort();
                TraceUtils.trace(" offset=" + offset);
                break;
            case APPEND_FRAME:
                // type is append_frame
                TraceUtils.trace("      append_frame=" + ft_val);
                offset = in.readUnsignedShort();
                TraceUtils.trace(" offset=" + offset);
                // read additional locals
                TraceUtils.trace(" numloc=");
                lockMap = readMapElements(code, in, ft_val - 251);
                TraceUtils.traceln("");
                break;
            case FULL_FRAME:
                // type is full_frame
                TraceUtils.trace("      full_frame=" + ft_val);
                offset = in.readUnsignedShort();
                TraceUtils.trace(" offset=" + offset);
                TraceUtils.trace(" numloc=");
                lockMap = readMap(code, in);
                TraceUtils.trace(" numstack=");
                stackMap = readMap(code, in);
                TraceUtils.traceln("");
                break;
            default:
                TraceUtils.trace("incorrect frame_type argument");

        }
        stackFrameType = frame_type;
        start_pc = prevFramePC == 0 ? offset : prevFramePC + offset + 1;
        prevFramePC = start_pc;
    }

    private int[] readMap(CodeData code, DataInputStream in) throws IOException {
        int num = in.readUnsignedShort();
        TraceUtils.trace("" + num);
        return readMapElements(code, in, num);
    }

    private int[] readMapElements(CodeData code, DataInputStream in, int num) throws IOException {
        int[] map = new int[num];
        for (int k = 0; k < num; k++) {
            int mt_val = in.readUnsignedByte();
            StackMapType maptype = stackMapType(mt_val, null);
            switch (maptype) {
                case ITEM_Object:
                    mt_val = mt_val | (in.readUnsignedShort() << 8);
                    break;
                case ITEM_NewObject: {
                    int pc = in.readUnsignedShort();
                    code.get_iAtt(pc).referred = true;
                    mt_val = mt_val | (pc << 8);
                    break;
                }
            }
            map[k] = mt_val;
            TraceUtils.trace(" " + HexUtils.toHex(mt_val));
        }
        return map;
    }

}
