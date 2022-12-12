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
package org.openjdk.asmtools.jasm;


import org.openjdk.asmtools.common.structure.StackMap;

import java.io.IOException;
import java.util.Objects;

import static org.openjdk.asmtools.common.structure.StackMap.FrameType.SAME_FRAME;

/**
 * 4.7.4. The StackMapTable Attribute
 * <p>
 * StackMapTable_attribute {
 * u2              attribute_name_index;
 * u4              attribute_length;
 * u2              number_of_entries;
 * stack_map_frame entries[number_of_entries];
 * }
 */
public class StackMapData implements DataWriter {

    // Indicates that a method's Code attribute has a StackMapTable attribute
    boolean hasStackMapTable = false;
    DataVector<? extends Indexer> localsMap, stackMap;
    JasmEnvironment environment;
    private int pc;
    private int scannerPosition = 0;
    private int offset;
    private int frameTypeTag;
    private String stackFrameType = null;

    StackMapData(JasmEnvironment environment) {
        this.environment = environment;
    }

    StackMapData setOffset(StackMapData prevFrame) {
        offset = (prevFrame == null) ? pc : (pc - prevFrame.pc - 1);
        return this;
    }

    StackMapData setStackFrameType(String stackFrameType) {
        // TODO: check the case stackFrameType == null && "xxx" && that pc is valid
        Objects.requireNonNull(stackFrameType,
                () -> this.environment.getLogger().getResourceString("err.obj.is.null", "String stackFrameType"));
        this.stackFrameType = stackFrameType;
        this.frameTypeTag = StackMap.getFrameTypeTag(stackFrameType);
        if (!StackMap.isValidFrameType(this.frameTypeTag)) {
            environment.error(scannerPosition, "err.invalid.stack.frame.type", stackFrameType);
        }
        return this;
    }

    /**
     * In a class file whose version number is 50.0 or above, if a method's Code attribute does not have a StackMapTable attribute,
     * it has an implicit stack map attribute (§4.10.1). This implicit stack map attribute is equivalent to a StackMapTable
     * attribute with number_of_entries equal to zero.
     *
     * @param hasStackMapTable false if a method's Code attribute does not have a StackMapTable attribute
     */
    StackMapData setIsStackMapTable(boolean hasStackMapTable) {
        this.hasStackMapTable = hasStackMapTable;
        return this;
    }

    StackMapData setPC(int pc) {
        this.pc = pc;
        return this;
    }

    StackMapData setScannerPosition(int scannerPosition) {
        this.scannerPosition = scannerPosition;
        return this;
    }

    /**
     * Checks whether a method's Code attribute has a StackMapTable attribute.
     *
     * @return true if a method's Code attribute has a StackMapTable attribute.
     */
    boolean isSet() {
        return this.stackFrameType != null;
    }

    @Override
    public int getLength() {
        int length = 0;
        StackMap.FrameType frameType = StackMap.FrameType.FULL_FRAME;

        if (hasStackMapTable) {
            if (stackFrameType != null) {
                frameType = StackMap.stackMapFrameType(frameTypeTag);
            }
            length += 1;
        }

        switch (frameType) {
            case SAME_FRAME:
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                length += stackMap.getLength() - 2;
                break;
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                length += stackMap.getLength();
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
            case SAME_FRAME_EX:
                length += 2;
                break;
            case APPEND_FRAME:
                length += 2 + (localsMap == null ? 0 : (localsMap.getLength() - 2));
                break;
            case FULL_FRAME:
                length += 2;
                length += (localsMap == null ? 2 : localsMap.getLength());
                length += (stackMap == null ? 2 : stackMap.getLength());
                break;
            default:
        }
        return length;
    }

    @Override
    public void write(CheckedDataOutputStream out) throws IOException {
        StackMap.FrameType frameType = StackMap.FrameType.FULL_FRAME;

        if (hasStackMapTable) {
            if (stackFrameType != null) {
                frameType = StackMap.stackMapFrameType(frameTypeTag);
            }
        }

        switch (frameType) {
            case SAME_FRAME -> {
                if (!SAME_FRAME.inRange(offset)) {
                    environment.error(scannerPosition, "err.invalid.offset.frame.type", offset, frameType.printName());
                    break;
                }
                out.writeByte(offset);
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                if (stackMap == null) {
                    environment.error(scannerPosition, "err.no.stack.map", frameType.printName());
                    break;
                }
                if (stackMap.elements.size() != 1) {
                    environment.error(scannerPosition, "err.should.be.only.one.stack.map.element", frameType.printName());
                    break;
                }
                // The offset_delta value for the frame is given by the formula frame_type - 64.
                if (offset >= 64) {
                    environment.error(scannerPosition, "err.invalid.offset.frame.type", offset, frameType.printName());
                }
                out.writeByte(frameType.fromTag() + offset);
                stackMap.writeElements(out);
            }
            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME -> {
                if (stackMap == null) {
                    environment.error(scannerPosition, "err.no.locals.map", frameType.printName());
                    break;
                }
                if (stackMap.elements.size() != 1) {
                    environment.error(scannerPosition, "err.should.be.only.one.stack.map.element", frameType.printName());
                    break;
                }
                out.writeByte(frameType.fromTag());
                out.writeShort(offset);
                stackMap.writeElements(out);
            }
            case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME, SAME_FRAME_EX -> {
                boolean error = false;
                if (stackMap != null) {
                    environment.error(scannerPosition, "err.unexpected.stack.maps", frameType.printName());
                    error = true;
                }
                if (localsMap != null) {
                    environment.error(scannerPosition, "err.unexpected.locals.maps", frameType.printName());
                    error = true;
                }
                if (error) {
                    break;
                }
                out.writeByte(frameType.fromTag());
                out.writeShort(offset);
            }
            case APPEND_FRAME -> {
                if (localsMap == null) {
                    environment.error(scannerPosition, "err.no.locals.map", frameType.printName());
                    break;
                }
                if (localsMap.elements.size() > 3) {
                    environment.error(scannerPosition, "err.more.locals.map.elements");
                    break;
                }
                out.writeByte(frameType.fromTag() + localsMap.elements.size() - 1);
                out.writeShort(offset);
                localsMap.writeElements(out);
            }
            case FULL_FRAME -> {
                if (hasStackMapTable) {
                    out.writeByte(frameType.fromTag());
                    out.writeShort(offset);
                } else {
                    out.writeShort(pc);
                }
                if (localsMap == null) {
                    out.writeShort(0);
                } else {
                    localsMap.write(out);
                }
                if (stackMap == null) {
                    out.writeShort(0);
                } else {
                    stackMap.write(out);
                }
            }
            default -> environment.error(scannerPosition, "invalid.stack.frame.verificationType", frameType.tagName());
        }
    }

    /**
     * verification_type_info:
     * Top_variable_info, Integer_variable_info, Float_variable_info, Null_variable_info,
     * UninitializedThis_variable_info, Long_variable_info, Double_variable_info
     * Common format:
     * *_info {
     * u1 tag = ITEM_*;  // from 0 to 6
     * }
     */
    static public class StackMapItemTagged implements DataWriter {

        StackMap.VerificationType itemVerificationType;

        StackMapItemTagged(StackMap.VerificationType itemVerificationType) {
            this.itemVerificationType = itemVerificationType;
        }

        @Override
        public int getLength() {
            return 1;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(itemVerificationType.tag());
        }
    }

    /**
     * verification_type_info:
     * Object_variable_info, Uninitialized_variable_info
     * Common Format:
     * *_info {
     * u1 tag = ITEM_*;  // from 7 to 8
     * u2 cpool_index/offset;
     * }
     */
    static public class StackMapItemTaggedPointer implements DataWriter {

        StackMap.VerificationType itemVerificationType;
        // Object_variable_info.cpool_index, ITEM_Uninitialized.offset
        Indexer arg;

        StackMapItemTaggedPointer(StackMap.VerificationType itemVerificationType, Indexer arg) {
            this.itemVerificationType = itemVerificationType;
            this.arg = arg;
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(itemVerificationType.tag());
            out.writeShort(arg.cpIndex);
        }
    }
}
