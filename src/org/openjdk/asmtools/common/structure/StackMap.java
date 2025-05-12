/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.structure;

import org.openjdk.asmtools.asmutils.FormatConsumer;
import org.openjdk.asmtools.asmutils.Range;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 4.7.4. The StackMapTable Attribute
 */
public class StackMap {

    /**
     * @param verificationTypeID u1 tag
     * @param errorConsumer      consumer to announce a problem
     * @return associated StackMap Verification Type
     */
    public static VerificationType getVerificationType(int verificationTypeID, Optional<FormatConsumer<String, Object>> errorConsumer) {
        if (VerificationType.isValidVerificationType(verificationTypeID)) {
            return VerificationType.get(verificationTypeID);
        } else {
            errorConsumer.ifPresent(c -> c.format("error.stackmap.unknown.type", verificationTypeID));
            return VerificationType.ITEM_UNKNOWN;
        }
    }

    public static VerificationType getVerificationType(String printName) {
        return VerificationType.getByPrintName(printName);
    }

    /**
     * Get Entry Type by tag belonging the range
     *
     * @param tag u1 tag in range
     * @return
     */
    public static EntryType stackMapEntryType(int tag) {
        EntryType entryType;
        entryType = EntryType.getByTag(tag);
        return entryType;
    }

    /**
     * Get frame type id by a name
     *
     * @param frameTypeName frame type name
     * @return Stack FrameType tag [0..255]
     */
    public static int getFrameTypeTagByName(String frameTypeName) {
        return EntryType.getByTagName(frameTypeName).fromTag();
    }

    /**
     * Get entry type id by a name
     *
     * @param entryTypeName entry type name
     * @return Stack FrameType tag [0..255]
     */
    public static EntryType getEntryTypeByName(String entryTypeName) {
        return EntryType.getByTagName(entryTypeName);
    }

    /**
     * Checks if the tag belongs to range of valid Frame Types
     *
     * @param tag
     * @return true if the tag does not belong to a range either of UNKNOWN_TYPE or RESERVED
     */
    public static boolean isValidEntryType(int tag) {
        return EntryType.isValid(tag);
    }

    /**
     * MapTypes table. These constants are used in stackmap pseudo-instructions only.
     */
    public enum VerificationType {
        /* Type codes for StackMap attribute */
        ITEM_UNKNOWN(-1, "??? Unknown verification type", "UNKNOWN"),     // placeholder for wrong types
        ITEM_Bogus(0, "bogus", "B"),                // an unknown or uninitialized value
        ITEM_Integer(1, "int", "I"),                // a 32-bit integer
        ITEM_Float(2, "float", "F"),                // not used
        ITEM_Double(3, "double", "D"),              // not used
        ITEM_Long(4, "long", "L"),                  // a 64-bit integer
        ITEM_Null(5, "null", "N"),                  // the type of null
        ITEM_InitObject(6, "this", "IO"),           // "this" in constructor
        ITEM_Object(7, "CP", "O"),                  // followed by 2-byte index of class name
        ITEM_NewObject(8, "at", "NO");              // followed by 2-byte ref to "new"

        private static HashMap<String, VerificationType> printNameToType;
        private static HashMap<String, VerificationType> parseKeyToType;
        private static HashMap<Integer, VerificationType> tagToType;
        private final String printName;
        private final String parseKey;
        private Integer tag;

        VerificationType(Integer tag, String printName, String parseKey) {
            this.tag = tag;
            this.printName = printName;
            this.parseKey = parseKey;
        }

        static boolean isValidVerificationType(int tag) {
            return tag >= ITEM_Bogus.tag && tag <= ITEM_NewObject.tag;
        }

        public static VerificationType getByPrintName(String printName) {
            if (printNameToType == null) {
                printNameToType = (HashMap<String, VerificationType>) Arrays.stream(VerificationType.values()).
                        collect(Collectors.toMap(VerificationType::printName, Function.identity()));
            }
            return printNameToType.get(printName);
        }

        public static VerificationType getByParseKey(String parseKey) {
            if (parseKeyToType == null) {
                parseKeyToType = (HashMap<String, VerificationType>) Arrays.stream(VerificationType.values()).
                        collect(Collectors.toMap(VerificationType::parseKey, Function.identity()));
            }
            VerificationType verificationType = parseKeyToType.get(parseKey);
            return verificationType == null ? VerificationType.ITEM_UNKNOWN : verificationType;
        }

        public static VerificationType get(int tag) {
            if (tagToType == null) {
                tagToType = (HashMap<Integer, VerificationType>) Arrays.stream(VerificationType.values()).
                        collect(Collectors.toMap(VerificationType::tag, Function.identity()));
            }
            return tagToType.get(tag);
        }

        public String parseKey() {
            return this.parseKey;
        }

        public String printName() {
            return this.printName;
        }

        public Integer tag() {
            return this.tag;
        }
    }

    /**
     * StackMap-FrameType table. These constants are used in stackmap pseudo-instructions
     * only.
     */
    public enum EntryType {
        UNKNOWN_TYPE(-1, -1, "unknown", false, false, false),                // placeholder for wrong frame types
        /* Type codes for StackMapFrame attribute */
        SAME_FRAME(0, 63, "same", false, false, false),
        SAME_LOCALS_1_STACK_ITEM_FRAME(64, 127, "stack1", false, true, false),
        RESERVED(128, 245, "reserved", false, false, false),
        EARLY_LARVAL(246, 246, "early_larval", false, false, true),
        SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED(247, 247, "stack1_ex", false, true, false),
        CHOP_1_FRAME(250, 250, "chop1", false, false, false),
        CHOP_2_FRAME(249, 249, "chop2", false, false, false),
        CHOP_3_FRAME(248, 248, "chop3", false, false, false),
        SAME_FRAME_EXTENDED(251, 251, "same_ex", false, false, false),
        APPEND_FRAME(252, 254, "append", true, false, false),
        FULL_FRAME(255, 255, "full", true, true, false);

        private static HashMap<String, EntryType> tagNameToFrameType;
        private final Range<Integer> tagRange;
        private final String tagName;
        private final boolean localMap;
        private final boolean stackMap;
        private final boolean fields;

        EntryType(int from, int to, String tagName, boolean localMap, boolean stackMap, boolean fields) {
            this.tagRange = new Range<>(from, to);
            this.tagName = tagName;
            this.localMap = localMap;
            this.stackMap = stackMap;
            this.fields = fields;
        }

        public boolean hasLocalMap() {
            return localMap;
        }

        public boolean hasStackMap() {
            return stackMap;
        }

        public boolean hasFields() {
            return fields;
        }

        public static EntryType getByTagName(String tagName) {
            if (tagNameToFrameType == null) {
                tagNameToFrameType = (HashMap<String, EntryType>) Arrays.stream(EntryType.values()).
                        collect(Collectors.toMap(EntryType::tagName, Function.identity()));
            }
            EntryType type = tagNameToFrameType.get(tagName);
            return type == null ? EntryType.UNKNOWN_TYPE : type;
        }

        public static EntryType getByTag(int tag) {
            for (EntryType type : EntryType.values()) {
                if (type.inRange(tag)) {
                    return type;
                }
            }
            return EntryType.UNKNOWN_TYPE;
        }

        public int fromTag() {
            return tagRange.from();
        }

        public String tagName() {
            return tagName;
        }

        public String printName() {
            String buf = this.name().toLowerCase().replace("_frame", "");
            return buf.startsWith("chop") ? tagName : buf;
        }

        public boolean inRange(int tag) {
            return tagRange.in(tag);
        }

        /**
         * Checks if the tag belongs to range of valid Frame Types
         *
         * @param tag
         * @return true if the tag does not belong to a range either of UNKNOWN_TYPE or RESERVED
         */
        public static boolean isValid(int tag) {
            for (EntryType entryType : Set.of(UNKNOWN_TYPE, RESERVED)) {
                if (entryType.inRange(tag))
                    return false;
            }
            return true;
        }
    }
}
