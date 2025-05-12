/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.common.structure.StackMap.EntryType.*;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;

/**
 * 4.7.4. The StackMapTable Attribute
 * <p>
 * StackMapTable_attribute {
 * u2              attribute_name_index;
 * u4              attribute_length;
 * u2              number_of_entries;
 * stack_map_entry entries[number_of_entries];
 * }
 */
public class StackMapData implements DataWriter {
    static final int UNDEFINED = -1;
    final JasmEnvironment environment;
    private long scannerPosition = 0;
    // Indicates that a method's Code attribute has a StackMapTable attribute (CFV >= 50.0) or has
    // StackMap_attribute {
    //    u2 attribute_name_index;
    //    u4 attribute_length;
    //    u2 number_of_entries;
    //    stack_map_entry entries[number_of_entries];
    //} where
    // stack_map_entry {
    //    u2 offset;
    //    u2 number_of_locals;
    //    verification_type_info locals[number_of_locals];
    //    u2 number_of_stack_items;
    //    verification_type_info stack[number_of_stack_items];
    //}
    final boolean hasStackMapTable;
    private int pc = UNDEFINED;
    private int offset = UNDEFINED;
    private StackMap.EntryType entryType = UNKNOWN_TYPE;
    DataVector<? extends Indexer> localsMap, stackMap, unsetFields;

    /**
     * In a class file whose version number is 50.0 or above, if a method's Code attribute does not have a StackMapTable attribute,
     * it has an implicit stack map attribute (§4.10.1). This implicit stack map attribute is equivalent to a StackMapTable
     * attribute with number_of_entries equal to zero.
     *
     * @param hasStackMapTable false if a method's Code attribute does not have a StackMapTable attribute
     */
    StackMapData(JasmEnvironment environment, boolean hasStackMapTable) {
        this.environment = environment;
        this.hasStackMapTable = hasStackMapTable;
    }

    StackMapData setOffset(StackMapData prevFrame) {
        offset = (prevFrame == null) ? pc : (pc - prevFrame.pc - 1);
        return this;
    }

    StackMapData setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    StackMapData setPC(int pc) {
        this.pc = pc;
        return this;
    }

    StackMapData setStackFrameTypeByName(String stackFrameTypeName) {
        Objects.requireNonNull(stackFrameTypeName,
                () -> this.environment.getLogger().getResourceString("err.obj.is.null", "String stackFrameType"));
        this.entryType = StackMap.getEntryTypeByName(stackFrameTypeName);
        if (this.entryType == UNKNOWN_TYPE) {
            environment.error(scannerPosition, "err.invalid.stack.frame.type", stackFrameTypeName);
        }
        return this;
    }

    /**
     * Sets Stack Frame type by number in table presentation: frame_type = 252
     *
     * @param stackFrameTypeValue frame type tag
     * @return StackMapData object
     */
    StackMapData setStackFrameType(int stackFrameTypeValue) {
        this.entryType = StackMap.EntryType.getByTag(stackFrameTypeValue);
        if (this.entryType == SAME_FRAME) {
            this.offset = stackFrameTypeValue;
        } else if (this.entryType == SAME_LOCALS_1_STACK_ITEM_FRAME) {
            this.offset = stackFrameTypeValue - SAME_LOCALS_1_STACK_ITEM_FRAME.fromTag();
        }
        return this;
    }

    StackMapData setScannerPosition(long scannerPosition) {
        this.scannerPosition = scannerPosition;
        return this;
    }

    /**
     * Checks whether a method's Code attribute has a StackMapTable attribute.
     *
     * @return true if a method's Code attribute has a StackMapTable attribute.
     */
    boolean isFrameTypeSet() {
        return hasStackMapTable ? this.entryType != UNKNOWN_TYPE : this.pc != UNDEFINED;
    }

    /**
     * The early_larval_frame wraps a base_frame.
     * As a wrapper, it doesn't supply the offset_delta.
     *
     * @return true if the StackMapData is a base_frame wrapper.
     */
    boolean isWrapper() {
        return hasStackMapTable && entryType == EARLY_LARVAL;
    }

    @Override
    public boolean isCountable() {
        return !isWrapper();
    }
    /**
     * Checks whether all fields corresponding to the current frame type are set.
     *
     * @return null if all fields are sufficiently valid to be written for the current stack map entry otherwise
     * expected token
     */
    JasmTokens.Token checkIntegrity() {
        switch (entryType) {
            case SAME_FRAME -> {                                   // 0-63
                return null;
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME -> {                // 64 - 127
                return isFull(stackMap) ? null : STACKMAP;
            }
            case EARLY_LARVAL -> {                                  // 246
                return isFull(unsetFields) ? null : UNSETFIELDS;
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED -> {       // 247
                if (offset == UNDEFINED) {
                    return OFFSETDELTA;
                }
                return isFull(stackMap) ? null : STACKMAP;
            }
            case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME,          // 248-250
                 SAME_FRAME_EXTENDED -> {                           // 251
                return (offset == UNDEFINED) ? OFFSETDELTA : null;
            }
            case APPEND_FRAME -> {                                  // 252-254
                if (offset == UNDEFINED) {
                    return OFFSETDELTA;
                }
                return isFull(localsMap) ? null : LOCALSMAP;
            }
            case FULL_FRAME -> {                                    // 255
                if (offset == UNDEFINED) {
                    return OFFSETDELTA;
                }
                if (!isFull(localsMap)) {
                    return LOCALSMAP;
                }
                if (!isFull(stackMap)) {
                    return STACKMAP;
                }
            }
        }
        return null;
    }

    private boolean isFull(DataVector<?>... maps) {
        for (DataVector map : maps) {
            if (map == null)
                return false;
        }
        return true;
    }

    @Override
    public int getLength() {
        int length = (hasStackMapTable) ? 1 : 0;
        if (!isFrameTypeSet() || !hasStackMapTable) {
            // in the case when either stack_frame_type or frame_type wasn't meet or
            // StackMap_attribute instead of StackMapTable_attribute
            entryType = FULL_FRAME;
        }
        switch (entryType) {
            case SAME_FRAME:
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                length += stackMap.getLength() - 2;
                break;
            case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
                length += stackMap.getLength();
                break;
            case EARLY_LARVAL:
                length += 2 + (unsetFields == null ? 0 : unsetFields.getLength()-2);
                break;
            case CHOP_1_FRAME:
            case CHOP_2_FRAME:
            case CHOP_3_FRAME:
            case SAME_FRAME_EXTENDED:
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
        if (!hasStackMapTable) {
            // Indicates that a method's Code attribute doesn't have a StackMapTable attribute (CFV >= 50.0)
            // but has
            // StackMap_attribute {
            //    u2 attribute_name_index;
            //    u4 attribute_length;
            //    u2 number_of_entries;
            //    stack_map_entry entries[number_of_entries];
            //} where
            // stack_map_entry {
            //    u2 offset;
            //    u2 number_of_locals;
            //    verification_type_info locals[number_of_locals];
            //    u2 number_of_stack_items;
            //    verification_type_info stack[number_of_stack_items];
            //}
            entryType = FULL_FRAME;
        }

        switch (entryType) {
            case EARLY_LARVAL -> {
                out.writeByte(entryType.fromTag());
                if (unsetFields == null) {
                    out.writeShort(0);
                } else {
                    unsetFields.write(out);
                }
            }
            case SAME_FRAME -> {
                if (!SAME_FRAME.inRange(offset)) {
                    environment.error(scannerPosition, "err.invalid.offset.frame.type", offset, entryType.printName());
                    break;
                }
                out.writeByte(offset);
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                if (stackMap == null) {
                    environment.error(scannerPosition, "err.no.stack.map", entryType.printName());
                    break;
                }
                if (stackMap.elements.size() != 1) {
                    environment.error(scannerPosition, "err.should.be.only.one.stack.map.element", entryType.printName());
                    break;
                }
                // The offset_delta value for the frame is given by the formula frame_type - 64.
                if (offset >= 64) {
                    environment.error(scannerPosition, "err.invalid.offset.frame.type", offset, entryType.printName());
                    break;
                }
                out.writeByte(entryType.fromTag() + offset);
                stackMap.writeElements(out);
            }
            case SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED -> {
                if (stackMap == null) {
                    environment.error(scannerPosition, "err.no.locals.map", entryType.printName());
                    break;
                }
                if (stackMap.elements.size() != 1) {
                    environment.error(scannerPosition, "err.should.be.only.one.stack.map.element", entryType.printName());
                    break;
                }
                out.writeByte(entryType.fromTag());
                out.writeShort(offset);
                stackMap.writeElements(out);
            }
            case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME, SAME_FRAME_EXTENDED -> {
                boolean error = false;
                if (stackMap != null) {
                    environment.error(scannerPosition, "err.unexpected.stack.maps", entryType.printName());
                    error = true;
                }
                if (localsMap != null) {
                    environment.error(scannerPosition, "err.unexpected.locals.maps", entryType.printName());
                    error = true;
                }
                if (error) {
                    break;
                }
                out.writeByte(entryType.fromTag());
                out.writeShort(offset);
            }
            case APPEND_FRAME -> {
                if (localsMap == null) {
                    environment.error(scannerPosition, "err.no.locals.map", entryType.printName());
                    break;
                }
                if (localsMap.elements.size() > 3) {
                    environment.error(scannerPosition, "err.more.locals.map.elements");
                    break;
                }
                out.writeByte(entryType.fromTag() + localsMap.elements.size() - 1);
                out.writeShort(offset);
                localsMap.writeElements(out);
            }
            case FULL_FRAME -> {
                if (hasStackMapTable) {
                    // method's Code attribute has a StackMapTable attribute (CFV >= 50.0)
                    out.writeByte(entryType.fromTag());
                    out.writeShort(offset);
                } else {
                    // method's Code attribute doesn't  has a StackMap attribute (CFV < 50.0)
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
            default -> environment.error(scannerPosition, "err.stackmap.entry.type.not.set", entryType.fromTag());
        }
    }

    public StackMap.EntryType getFrameType() {
        return entryType;
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
