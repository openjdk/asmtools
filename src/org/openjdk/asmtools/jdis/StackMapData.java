/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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
    EAttributeType type;
    StackMap.FrameType stackFrameType = null;
    // stack frame type value
    int stackFrameTypeValue;
    int frame_pc;
    int offset;
    int[] lockMap;
    int[] stackMap;

    /**
     * @param type          either Implicit stack map attribute or the StackMapTable attribute
     * @param firstStackMap is it an entries[0] in the stack_map_frame structure? i.e. Does the StackMapData describe
     *                      the second stack map frame of the method?
     * @param prevFrame_pc  the bytecode offset of the previous entry (entries[current_index-1])
     * @param code          the code attribute where this attribute is located
     * @param in            the input stream
     * @throws IOException  the exception if something went wrong
     */
    public StackMapData(EAttributeType type, boolean firstStackMap, int prevFrame_pc, CodeData code, DataInputStream in) throws IOException {
        super(code);
        this.type = type;
        if (type == EAttributeType.STACKMAP) {
            frame_pc = in.readUnsignedShort();
            lockMap = readMap(in);
            stackMap = readMap(in);
            environment.traceln(" stack_map_entry:pc=%d numloc=%s  numstack=%s",
                    frame_pc, mapToHexString(lockMap), mapToHexString(stackMap));
        } else { // if (type == EDataType.STACKMAPTABLE)
            stackFrameTypeValue = in.readUnsignedByte();
            StackMap.FrameType frame_type = StackMap.stackMapFrameType(stackFrameTypeValue);
            switch (frame_type) {
                case SAME_FRAME -> {
                    // verificationType is same_frame;
                    offset = stackFrameTypeValue;
                    environment.traceln(" same_frame=%d", stackFrameTypeValue);
                }
                case SAME_FRAME_EX -> {
                    // verificationType is same_frame_extended;
                    offset = in.readUnsignedShort();
                    environment.traceln(" same_frame_extended=%d, offset=%d", stackFrameTypeValue, offset);
                }
                case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                    // verificationType is same_locals_1_stack_item_frame
                    offset = stackFrameTypeValue - 64;
                    stackMap = readMapElements(in, 1);
                    environment.traceln(" same_locals_1_stack_item_frame=%d, offset=%d, numstack=%s",
                            stackFrameTypeValue, offset, mapToHexString(stackMap));
                }
                case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME -> {
                    // verificationType is same_locals_1_stack_item_frame_extended
                    offset = in.readUnsignedShort();
                    stackMap = readMapElements(in, 1);
                    environment.traceln(" same_locals_1_stack_item_frame_extended=%d, offset=%d, numstack=%s",
                            stackFrameTypeValue, offset, mapToHexString(stackMap));
                }
                case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME -> {
                    // verificationType is chop_frame
                    offset = in.readUnsignedShort();
                    environment.traceln(" chop_frame=%d offset=%d", stackFrameTypeValue, offset);
                }
                case APPEND_FRAME -> {
                    // verificationType is append_frame
                    offset = in.readUnsignedShort();
                    lockMap = readMapElements(in, stackFrameTypeValue - 251);
                    environment.traceln(" append_frame=%d offset=%d numlock=%s",
                            stackFrameTypeValue, offset, mapToHexString(lockMap));
                }
                case FULL_FRAME -> {
                    // verificationType is full_frame
                    offset = in.readUnsignedShort();
                    lockMap = readMap(in);
                    stackMap = readMap(in);
                    environment.traceln(" full_frame=%d offset=%d numloc=%s  numstack=%s",
                            stackFrameTypeValue, offset, mapToHexString(lockMap), mapToHexString(stackMap));
                }
                default -> environment.traceln("incorrect frame_type argument");
            }
            stackFrameType = frame_type;
            if( prevFrame_pc == 0 && firstStackMap) {
                frame_pc = offset;
            } else {
                frame_pc = prevFrame_pc + offset + 1;
            }
        }
    }

    /**
     * @return the bytecode offset at which a stack map frame applies
     */
    public int getFramePC() {
        return frame_pc;
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
                        throw new FormatError(environment.getLogger(), s);
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

    /*
     *  In a class file whose version number is 50.0 or above, if a method's Code attribute does not have a StackMapTable attribute,
     *  it has an implicit stack map attribute (chapter 4.10.1). This implicit stack map attribute is equivalent to a StackMapTable
     *  attribute with number_of_entries equal to zero.
     */
    enum EAttributeType {
        // Implicit stack map attribute
        // This implicit stack map attribute is equivalent to a StackMapTable attribute with number_of_entries equal to zero.
        STACKMAP("ImplicitStackMap"),
        // The StackMapTable attribute
        STACKMAPTABLE("StackMapTable");
        private final String name;
        EAttributeType(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }
}
