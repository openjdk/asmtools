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
package org.openjdk.asmtools.jasm;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * ClassFileConst
 * <p>
 * The classes in Tables are following a Singleton Pattern. These classes are Enums, and
 * they are contained in private hash maps (lookup tables and reverse lookup tables).
 * These hash maps all have public accessors, which clients use to look-up enums.
 * <p>
 * Tokens in this table carry no external state, and are typically treated as constants.
 * They do not need to be reset.
 */
public final class ClassFileConst {

    public static final int JAVA_MAGIC = 0xCAFEBABE;
    /**
     * Lookup-tables for various types.
     */
    private static final HashMap<String, SubTag> NameToSubTag = new HashMap<>(9);
    private static final HashMap<Integer, SubTag> SubTags = new HashMap<>(9);

    private static final HashMap<String, BasicType> NameToBasicType = new HashMap<>(10);
    private static final HashMap<Integer, BasicType> BasicTypes = new HashMap<>(10);

    private static final HashMap<Character, AnnotationElementType> AnnotationElementTypes = new HashMap<>(10);

    private static final HashMap<String, ConstType> NameToConstantType = new HashMap<>(ConstType.maxTag);
    private static final HashMap<Integer, ConstType> ConstantTypes = new HashMap<>(ConstType.maxTag);

    static {
        // register all the tokens
        for (ConstType ct : ConstType.values()) {
            registerConstantType(ct);
        }

        /* Type codes for SubTags */
        for (SubTag st : SubTag.values()) {
            registerSubtag(st);
        }

        /* Type codes for BasicTypes */
        for (BasicType bt : BasicType.values()) {
            registerBasicType(bt);
        }

        /* Type codes for BasicTypes */
        for (AnnotationElementType aet : AnnotationElementType.values()) {
            registerAnnotationElementType(aet);
        }
    }

    static public ConstType tag(int i) {
        return ConstantTypes.get(i);
    }

    static public ConstType tag(String parseKey) {
        return NameToConstantType.get(parseKey);
    }

    private static void registerConstantType(ConstType constType) {
        NameToConstantType.put(constType.parseKey, constType);
        if ( !ConstantTypes.containsKey(constType.tag) ) {
            // only first CONSTANT_INTEGER(3, "CONSTANT_INTEGER", "int", AnnotationElementType.AE_INT)
            ConstantTypes.put(constType.tag, constType);
        }
    }

    private static void registerSubtag(SubTag tg) {
        NameToSubTag.put(tg.printValue, tg);
        SubTags.put(tg.value, tg);
    }

    public static SubTag subTag(String subtag) {
        return NameToSubTag.get(subtag);
    }

    public static SubTag subTag(int subtag) {
        return SubTags.get(subtag);
    }

    private static void registerBasicType(BasicType basicType) {
        NameToBasicType.put(basicType.printValue, basicType);
        BasicTypes.put(basicType.value, basicType);
    }

    public static BasicType getBasicType(String idValue) {
        return NameToBasicType.get(idValue);
    }

    public static BasicType getBasicType(int subtag) {
        return BasicTypes.get(subtag);
    }

    public static int basicTypeValue(String idValue) {
        int retval = -1;
        BasicType tg = NameToBasicType.get(idValue);
        if (tg != null) {
            retval = tg.value;
        }
        return retval;
    }

    private static void registerAnnotationElementType(AnnotationElementType annotationElementType) {
        // NameToAnnotationElementType.put(annotationElementType.printValue, annotationElementType);
        AnnotationElementTypes.put(annotationElementType.value.charAt(0), annotationElementType);
    }

    public static AnnotationElementType getAnnotationElementType(char subTag) {
        AnnotationElementType elementType = AnnotationElementTypes.get(subTag);
        if (elementType == null) {
            elementType = AnnotationElementType.AE_UNKNOWN;
        }
        return elementType;
    }

    /**
     * A (typed) tag (constant) representing the type of Constant in the Constant Pool.
     */
    public enum ConstType {
        CONSTANT_UNKNOWN(-1, "CONSTANT_UNKNOWN", "", AnnotationElementType.AE_NOT_APPLICABLE),
        //
        CONSTANT_ZERO(0, "CONSTANT_ZERO", "", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_UTF8(1, "CONSTANT_UTF8", "Utf8", AnnotationElementType.AE_STRING),
        CONSTANT_ASCIZ(1, "CONSTANT_UTF8", "Asciz", AnnotationElementType.AE_STRING),   // supports previous version
        // Constant 2 reserved
        CONSTANT_INTEGER(3, "CONSTANT_INTEGER",  AnnotationElementType.AE_INT.printValue, AnnotationElementType.AE_INT),
        CONSTANT_INTEGER_BYTE(3, "CONSTANT_INTEGER", AnnotationElementType.AE_BYTE.printValue, AnnotationElementType.AE_BYTE),
        CONSTANT_INTEGER_CHAR(3, "CONSTANT_INTEGER", AnnotationElementType.AE_CHAR.printValue, AnnotationElementType.AE_CHAR),
        CONSTANT_INTEGER_SHORT(3, "CONSTANT_INTEGER", AnnotationElementType.AE_SHORT.printValue, AnnotationElementType.AE_SHORT),
        CONSTANT_INTEGER_BOOLEAN(3, "CONSTANT_INTEGER", AnnotationElementType.AE_BOOLEAN.printValue, AnnotationElementType.AE_BOOLEAN),

        CONSTANT_FLOAT(4, "CONSTANT_FLOAT", "float", AnnotationElementType.AE_FLOAT),
        CONSTANT_LONG(5, "CONSTANT_LONG", "long", AnnotationElementType.AE_LONG),
        CONSTANT_DOUBLE(6, "CONSTANT_DOUBLE", "double", AnnotationElementType.AE_DOUBLE),
        CONSTANT_CLASS(7, "CONSTANT_CLASS", "class", AnnotationElementType.AE_CLASS),
        CONSTANT_STRING(8, "CONSTANT_STRING", "String", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_FIELDREF(9, "CONSTANT_FIELDREF", "Field", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_METHODREF(10, "CONSTANT_METHODREF", "Method", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_INTERFACEMETHODREF(11, "CONSTANT_INTERFACEMETHODREF", "InterfaceMethod", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_NAMEANDTYPE(12, "CONSTANT_NAMEANDTYPE", "NameAndType", AnnotationElementType.AE_NOT_APPLICABLE),
        // Constant 13 reserved
        // Constant 14 reserved
        CONSTANT_METHODHANDLE(15, "CONSTANT_METHODHANDLE", "MethodHandle", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_METHODTYPE(16, "CONSTANT_METHODTYPE", "MethodType", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_DYNAMIC(17, "CONSTANT_DYNAMIC", "Dynamic", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_INVOKEDYNAMIC(18, "CONSTANT_INVOKEDYNAMIC", "InvokeDynamic", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_MODULE(19, "CONSTANT_MODULE", "Module", AnnotationElementType.AE_NOT_APPLICABLE),
        CONSTANT_PACKAGE(20, "CONSTANT_PACKAGE", "Package", AnnotationElementType.AE_NOT_APPLICABLE);

        static final public int maxTag = 20;

        private final int tag;
        private final String printVal;
        private final String parseKey;
        private final AnnotationElementType annotationElementType;

        ConstType(int val, String printVal, String parseKey, AnnotationElementType annotationElementType) {
            this.tag = val;
            this.printVal = printVal;
            this.parseKey = parseKey;
            this.annotationElementType = annotationElementType;
        }

        public boolean oneOf(ConstType... constTypes) {
            for (ConstType constType : constTypes) {
                if (this.tag == constType.tag) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The tag item uses a single ASCII character to indicate the type of the value of the element-value pair.
         * This determines which item of the value union is in use. Table 4.7.16.1-A shows the valid characters
         * for the tag item.
         *
         * @return a single ASCII character
         */
        public char getAnnotationElementTypeValue() {
            return annotationElementType.tag();
        }

        public byte getTag() {
            return (byte)tag;
        }

        public String parseKey() {
            return parseKey;
        }

        public String printVal() {
            return printVal;
        }

        public void print(PrintWriter out) {
            out.print(parseKey);
        }

        @Override
        public String toString() {
            return printVal + "." + tag;
        }
    }

    /**
     * Annotation Element Type enums
     * Table 4.7.16.1-A. Interpretation of tag values as types
     */
     public enum AnnotationElementType {
        AE_BYTE("B", "byte"),
        AE_CHAR("C", "char"),
        AE_SHORT("S", "short"),
        AE_INT("I", "int"),
        AE_LONG("J", "long"),
        AE_FLOAT("F", "float"),
        AE_DOUBLE("D", "double"),
        AE_BOOLEAN("Z", "boolean"),
        AE_STRING("s", "string"),
        AE_ENUM("e", "enum"),
        AE_CLASS("c", "class"),
        AE_ANNOTATION("@", "annotation"),
        AE_ARRAY("[", "array"),
        AE_UNKNOWN("U", "unknown"),
        AE_NOT_APPLICABLE("N", "not applicable");

        private final String value;
        private final String printValue;

        AnnotationElementType(String value, String printValue) {
            this.value = value;
            this.printValue = printValue;
        }

        public String value() {
            return value;
        }

        public String printValue() {
            return printValue;
        }

        /**
         * The tag item uses a single ASCII character to indicate the type of the value of the element-value pair.
         * This determines which item of the value union is in use. Table 4.7.16.1-A shows the valid characters
         * for the tag item.
         *
         * @return a single ASCII character
         */
        public char tag() {
            return value.charAt(0);
        }

        public static boolean isSet(char tagChar) {
            return AE_NOT_APPLICABLE.tag() != tagChar && AE_UNKNOWN.tag() != tagChar;
        }
    }

    /**
     * SubTag enums
     */
     public enum SubTag {
        REF_GETFIELD(1, "REF_getField"),
        REF_GETSTATIC(2, "REF_getStatic"),
        REF_PUTFIELD(3, "REF_putField"),
        REF_PUTSTATIC(4, "REF_putStatic"),
        REF_INVOKEVIRTUAL(5, "REF_invokeVirtual"),
        REF_INVOKESTATIC(6, "REF_invokeStatic"),
        REF_INVOKESPECIAL(7, "REF_invokeSpecial"),
        REF_NEWINVOKESPECIAL(8, "REF_newInvokeSpecial"),
        REF_INVOKEINTERFACE(9, "REF_invokeInterface");

        private final Integer value;
        private final String printValue;

        SubTag(Integer val, String print) {
            value = val;
            printValue = print;
        }

        public String printValue() {
            return printValue;
        }

        public Integer value() {
            return value;
        }
    }

    /**
     * BasicType enums
     */
     public enum BasicType {
        T_INT(0x0000000a, "int"),
        T_LONG(0x0000000b, "long"),
        T_FLOAT(0x00000006, "float"),
        T_DOUBLE(0x00000007, "double"),
        T_CLASS(0x00000002, "class"),
        T_BOOLEAN(0x00000004, "boolean"),
        T_CHAR(0x00000005, "char"),
        T_BYTE(0x00000008, "byte"),
        T_SHORT(0x00000009, "short");

        private final Integer value;
        private final String printValue;

        BasicType(Integer value, String printValue) {
            this.value = value;
            this.printValue = printValue;
        }

        public String printValue() {
            return printValue;
        }
    }
}
