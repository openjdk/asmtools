/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.StackMap;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.openjdk.asmtools.asmutils.StringUtils.mapToHexString;

/**
 * represents one entry of StackMap attribute
 */
class StackMapData extends MemberData<CodeData> {

    static int prevFramePC = 0;

    EDataType type;
    StackMap.FrameType stackFrameType = null;
    int start_pc;
    int[] lockMap;
    int[] stackMap;

    public StackMapData(EDataType type, CodeData code, DataInputStream in) throws IOException {
        super(code);
        this.type = type;
        if (type == EDataType.STACKMAP) {
            start_pc = in.readUnsignedShort();
            lockMap = readMap(in);
            stackMap = readMap(in);
            environment.traceln(" stack_map_entry:pc=%d numloc=%s  numstack=%s",
                    start_pc, mapToHexString(lockMap), mapToHexString(stackMap));
        } else { // if (type == EDataType.STACKMAPTABLE)
            int ft_val = in.readUnsignedByte();
            StackMap.FrameType frame_type = StackMap.stackMapFrameType(ft_val);
            int offset = 0;
            switch (frame_type) {
                case SAME_FRAME -> {
                    // verificationType is same_frame;
                    offset = ft_val;
                    environment.traceln(" same_frame=%d", ft_val);
                }
                case SAME_FRAME_EX -> {
                    // verificationType is same_frame_extended;
                    offset = in.readUnsignedShort();
                    environment.traceln(" same_frame_extended=%d, offset=%d", ft_val, offset);
                }
                case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                    // verificationType is same_locals_1_stack_item_frame
                    offset = ft_val - 64;
                    stackMap = readMapElements(in, 1);
                    environment.traceln(" same_locals_1_stack_item_frame=%d, offset=%d, numstack=%s",
                            ft_val, offset, mapToHexString(stackMap));
                }
                case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME -> {
                    // verificationType is same_locals_1_stack_item_frame_extended
                    offset = in.readUnsignedShort();
                    stackMap = readMapElements(in, 1);
                    environment.traceln(" same_locals_1_stack_item_frame_extended=%d, offset=%d, numstack=%s",
                            ft_val, offset, mapToHexString(stackMap));
                }
                case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME -> {
                    // verificationType is chop_frame
                    offset = in.readUnsignedShort();
                    environment.traceln(" chop_frame=%d offset=%d", ft_val, offset);
                }
                case APPEND_FRAME -> {
                    // verificationType is append_frame
                    offset = in.readUnsignedShort();
                    lockMap = readMapElements(in, ft_val - 251);
                    environment.traceln(" append_frame=%d offset=%d numlock=%s",
                            ft_val, offset, mapToHexString(lockMap));
                }
                case FULL_FRAME -> {
                    // verificationType is full_frame
                    offset = in.readUnsignedShort();
                    lockMap = readMap(in);
                    stackMap = readMap(in);
                    environment.traceln(" full_frame=%d offset=%d numloc=%s  numstack=%s",
                            ft_val, offset, mapToHexString(lockMap), mapToHexString(stackMap));
                }
                default -> environment.traceln("incorrect frame_type argument");
            }
            stackFrameType = frame_type;
            start_pc = prevFramePC == 0 ? offset : prevFramePC + offset + 1;
            prevFramePC = start_pc;
        }
    }

    private int[] readMap(DataInputStream in) throws IOException {
        int num = in.readUnsignedShort();
        return readMapElements(in, num);
    }

    private int[] readMapElements(DataInputStream in, int num) throws IOException {
        int[] map = new int[num];
        for (int k = 0; k < num; k++) {
            int mt_val;
            mt_val = in.readUnsignedByte();
            StackMap.VerificationType stackMapVerificationType =
                    StackMap.getVerificationType(mt_val, Optional.of((s) -> {
                        throw new FormatError(s);
                    }));
            switch (stackMapVerificationType) {
                case ITEM_Object -> mt_val = mt_val | (in.readUnsignedShort() << 8);
                case ITEM_NewObject -> {
                    int pc = in.readUnsignedShort();
                    data.getInstructionAttribute(pc).referred = true;
                    mt_val = mt_val | (pc << 8);
                }
            }
            map[k] = mt_val;
        }
        return map;
    }

    enum EDataType {STACKMAP, STACKMAPTABLE}
}
