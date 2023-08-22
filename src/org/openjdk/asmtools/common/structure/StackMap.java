/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.asmutils.Range;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * 4.7.4. The StackMapTable Attribute
 */
public class StackMap {

    /**
     * @param verificationTypeID u1 tag
     * @param errorConsumer   consumer to announce a problem
     * @return associated StackMap Verification Type
     */
    public static VerificationType getVerificationType(int verificationTypeID, Optional<Consumer<String>> errorConsumer) {
        if (VerificationType.isValidVerificationType(verificationTypeID)) {
            return VerificationType.get(verificationTypeID);
        } else {
            errorConsumer.ifPresent(c->c.accept(format("Unknown StackMap verification type %d", verificationTypeID)));
            return VerificationType.ITEM_UNKNOWN;
        }
    }

    public static VerificationType getVerificationType(String printName) {
        return VerificationType.getByPrintName(printName);
    }

    /**
     * Get Frame Type by tag belonging the range
     *
     * @param tag u1 tag in range
     * @return
     */
    public static FrameType stackMapFrameType(int tag) {
        FrameType frameType;
        frameType = FrameType.getByTag(tag);
        return frameType;
    }

    /**
     * Get frame type id by a name
     *
     * @param frameTypeName frame type name
     * @return Stack FrameType tag [0..255]
     */
    public static int getFrameTypeTag(String frameTypeName) {
        return FrameType.getByTagName(frameTypeName).fromTag();
    }

    /**
     * Checks if the tag belongs to range of valid Frame Types
     *
     * @param tag
     * @return true if the tag does not belong to a range either of UNKNOWN_TYPE or RESERVED
     */
    public static boolean isValidFrameType(int tag) {
        return FrameType.isValid(tag);
    }

    /**
     * MapTypes table. These constants are used in stackmap pseudo-instructions only.
     */
    public enum VerificationType {
        /* Type codes for StackMap attribute */
        ITEM_UNKNOWN(-1, "UNKNOWN", "UNKNOWN"),     // placeholder for wrong types
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
    public enum FrameType {
        UNKNOWN_TYPE(-1, -1, "unknown"),                // placeholder for wrong frame types
        /* Type codes for StackMapFrame attribute */
        SAME_FRAME(0, 63, "same"),
        SAME_LOCALS_1_STACK_ITEM_FRAME(64, 127, "stack1"),
        RESERVED(128, 246, "reserved"),
        SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME(247, 247, "stack1_ex"),
        CHOP_1_FRAME(250, 250, "chop1"),
        CHOP_2_FRAME(249, 249, "chop2"),
        CHOP_3_FRAME(248, 248, "chop3"),
        SAME_FRAME_EX(251, 251, "same_ex"),
        APPEND_FRAME(252, 254, "append"),
        FULL_FRAME(255, 255, "full");

        private static HashMap<String, FrameType> tagNameToFrameType;
        private final Range<Integer> tagRange;
        private final String tagName;

        FrameType(int from, int to, String tagName) {
            this.tagRange = new Range<>(from, to);
            this.tagName = tagName;
        }

        public static FrameType getByTagName(String tagName) {
            if (tagNameToFrameType == null) {
                tagNameToFrameType = (HashMap<String, FrameType>) Arrays.stream(FrameType.values()).
                        collect(Collectors.toMap(FrameType::tagName, Function.identity()));
            }
            FrameType type = tagNameToFrameType.get(tagName);
            return type == null ? FrameType.UNKNOWN_TYPE : type;
        }

        public static FrameType getByTag(int tag) {
            for (FrameType type : FrameType.values()) {
                if (type.inRange(tag)) {
                    return type;
                }
            }
            return FrameType.UNKNOWN_TYPE;
        }

        public int fromTag() {
            return tagRange.from();
        }

        public String tagName() {
            return tagName;
        }

        public String printName() {
            return this.name().toLowerCase();
        }

        public Range<Integer> tagRange() {
            return tagRange;
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
            for (FrameType frameType : Set.of(UNKNOWN_TYPE, RESERVED)) {
                if (frameType.inRange(tag))
                    return false;
            }
            return true;
        }
    }
}
