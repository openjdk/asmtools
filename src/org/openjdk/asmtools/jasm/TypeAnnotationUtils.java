/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 */
public class TypeAnnotationUtils {


    /*-------------------------------------------------------- */
    /* TypeAnnotationData Inner Classes */
    static public enum PathKind {
        DEEPER_ARRAY            (0, "DEEPER_ARRAY", "ARRAY"),
        DEEPER_NESTEDTYPE       (1, "DEEPER_NESTEDTYPE", "INNER_TYPE"),
        BOUND_WILDCARDTYPE      (2, "BOUND_WILDCARDTYPE", "WILDCARD"),
        ITHARG_PARAMETERTYPE    (3, "ITHARG_PARAMETERTYPE", "TYPE_ARGUMENT");

        private final int key;
        private final String type;
        private final String parseKey;
        public static final int maxLen = 3;

        PathKind(int key, String type, String parseval) {
            this.key = key;
            this.type = type;
            this.parseKey = parseval;
        }

        public int key() {
            return key;
        }

        public String parsekey() {
            return parseKey;
        }

    }

    static private HashMap<String, PathKind> PathTypeHash = new HashMap<>(PathKind.maxLen);

    private static void initPathTypes(PathKind pk) {
        PathTypeHash.put(pk.parseKey, pk);
    }

    static {
        for (PathKind pk : PathKind.values()) {
            initPathTypes(pk);
        }
    }

    public static PathKind pathKind(String parseKey) {
        return PathTypeHash.get(parseKey);
    }

    // will throw ArrayIndexOutOfBounds if i < 0 or i > 3
    static public PathKind getPathKind(int i) {
        return PathKind.values()[i];
    }

    /*-------------------------------------------------------- */
    /* TypeAnnotationData Inner Classes */
    static public class TypePathEntry {

        private final PathKind kind;
        private final char index;

        public TypePathEntry(int kind, char index) {
            this.kind = getPathKind(kind);
            this.index = index;
        }

        public PathKind kind() {
            return kind;
        }

        public char index() {
            return index;
        }

        public String toString() {
            return kind.parsekey() + "(" + index + ")";
        }

    }

  /*-------------------------------------------------------- */
  /* TypeAnnotationData Inner Classes */
    static public enum InfoType {
        TYPEPARAM           ("TYPEPARAM", "TYPEPARAM"),
        SUPERTYPE           ("SUPERTYPE", "SUPERTYPE"),
        TYPEPARAM_BOUND     ("TYPEPARAM_BOUND", "TYPEPARAM_BOUND"),
        EMPTY               ("EMPTY", "EMPTY"),
        METHODPARAM         ("METHODPARAM", "METHODPARAM"),
        EXCEPTION           ("EXCEPTION", "EXCEPTION"),
        LOCALVAR            ("LOCALVAR", "LOCALVAR"),
        CATCH               ("CATCH", "CATCH"),
        OFFSET              ("OFFSET", "OFFSET"),
        TYPEARG             ("TYPEARG", "TYPEARG");

        private final String parseKey;
        private final String printval;

        InfoType(String parse, String print) {
            parseKey = parse;
            printval = print;
        }

        public String parseKey() {
            return parseKey;
        }

    }

    /**
     * TargetType
     *
     * A (typed) tag (constant) representing the type of Annotation Target.
     */
    static public enum TargetType {
        class_type_param            (0x00, "CLASS_TYPE_PARAMETER",  InfoType.TYPEPARAM, "class type parameter"),
        meth_type_param             (0x01, "METHOD_TYPE_PARAMETER",  InfoType.TYPEPARAM, "method type parameter"),
        class_exts_impls            (0x10, "CLASS_EXTENDS",  InfoType.SUPERTYPE, "class extends/implements"),
        class_type_param_bnds       (0x11, "CLASS_TYPE_PARAMETER_BOUND",  InfoType.TYPEPARAM_BOUND, "class type parameter bounds"),
        meth_type_param_bnds        (0x12, "METHOD_TYPE_PARAMETER_BOUND",  InfoType.TYPEPARAM_BOUND, "method type parameter bounds"),
        field                       (0x13, "FIELD",  InfoType.EMPTY, "field"),
        meth_ret_type               (0x14, "METHOD_RETURN",  InfoType.EMPTY, "method return type"),
        meth_reciever               (0x15, "METHOD_RECEIVER",  InfoType.EMPTY, "method reciever"),
        meth_formal_param           (0x16, "METHOD_FORMAL_PARAMETER",  InfoType.METHODPARAM, "method formal parameter type"),
        throws_type                 (0x17, "THROWS",  InfoType.EXCEPTION, "exception type in throws"),
        local_var                   (0x40, "LOCAL_VARIABLE",  InfoType.LOCALVAR, "local variable"),
        resource_var                (0x41, "RESOURCE_VARIABLE",  InfoType.LOCALVAR, "resource variable"),                     // TODO
        exception_param             (0x42, "EXCEPTION_PARAM",  InfoType.CATCH, "exception parameter"),                   // TODO
        type_test                   (0x43, "INSTANCEOF",  InfoType.OFFSET, "type test (instanceof)"),
        obj_creat                   (0x44, "NEW",  InfoType.OFFSET, "object creation (new)"),
        constr_ref_receiver         (0x45, "CONSTRUCTOR_REFERENCE_RECEIVER", InfoType.OFFSET, "constructor reference receiver"),
        meth_ref_receiver           (0x46, "METHOD_REFERENCE_RECEIVER", InfoType.OFFSET, "method reference receiver"),
        cast                        (0x47, "CAST",  InfoType.TYPEARG, "cast"),
        constr_invoc_typearg        (0x48, "CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT",  InfoType.TYPEARG, "type argument in constructor call"),
        meth_invoc_typearg          (0x49, "METHOD_INVOCATION_TYPE_ARGUMENT", InfoType.TYPEARG, "type argument in method call"),
        constr_ref_typearg          (0x4A, "CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT", InfoType.TYPEARG, "type argument in constructor reference"),
        meth_ref_typearg            (0x4B, "METHOD_REFERENCE_TYPE_ARGUMENT",  InfoType.TYPEARG, "type argument in method reference");

        public static final int maxTag = 0x9A;
        public static final int maxLen = 36;

        public final int value;
        private final String parseKey;
        private final InfoType infoKey;
        private final String printval;

        TargetType(int val, String parse, InfoType info, String print) {
            value = val;
            parseKey = parse;
            infoKey = info;
            printval = print;
        }

        public String parseKey() {
            return parseKey;
        }

        public String infoKey() {
            return infoKey.parseKey();
        }

        public InfoType infoType() {
            return infoKey;
        }

        public void print(PrintWriter out) {
            out.print(parseKey);
        }

        @Override
        public String toString() {
            return parseKey + " (" + infoKey() + ") <" + printval + "> [" + Integer.toHexString(value) + "]";
        }
    };

    static private HashMap<String, TargetType> TargetTypeHash = new HashMap<>(TargetType.maxLen);
    static private HashMap<Integer, TargetType> TargetTypeList = new HashMap<>(TargetType.maxLen);

    private static void initTargetTypes(TypeAnnotationUtils.TargetType tt) {
        TargetTypeList.put(tt.value, tt);
        TargetTypeHash.put(tt.parseKey, tt);
    }

    static {
        for (TargetType type : TargetType.values()) {
            initTargetTypes(type);
        }
    }

    public static TargetType targetType(String parseKey) {
        return TargetTypeHash.get(parseKey);
    }

    public static TargetType targetTypeEnum(Integer typeCode) {
        return TargetTypeList.get(typeCode);
    }

    /**
     * TargetInfo
     *
     * BaseClass for any Type Annotation Target-Info.
     */
    public static class TargetInfo {

        protected TargetType targettype = null;

        public TargetInfo(TargetType tt) {
            targettype = tt;
        }

        public TargetType getTargetType() {
            return targettype;
        }

        public void print(PrintWriter out, String tab) {
            // print the TargetType and TargetInfo
            out.print(tab + " {");
            targettype.print(out);
            _print(out, tab);
            out.print(tab + "}");
        }

        public void _print(PrintWriter out, String tab) {
            // sub-classes override
        }

        public void write(CheckedDataOutputStream out) throws IOException {
            // placeholder
        }

        public int getLength() {
            return 0;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        protected void _toString(StringBuilder sb, int tabLevel) {
            // sub-classes override
        }

        public String toString(int tabLevel) {
            StringBuilder sb = new StringBuilder();
            String tabStr = tabString(tabLevel);

            // first print the name/target-type
            sb.append(tabStr);
            sb.append(targettype.infoKey() + " \n");
            sb.append(tabStr);
            sb.append(targettype + " \n");

            // get the sub-classes parts
            _toString(sb, tabLevel);

            return sb.toString();
        }

        protected static String tabString(int tabLevel) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tabLevel; i++) {
                sb.append('\t');
            }

            return sb.toString();
        }

    }

    /**
     * TypeParams_Target (3.3.1 Type parameters)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class typeparam_target extends TargetInfo {

        int pindex;

        public typeparam_target(TargetType tt, int indx) {
            super(tt);
            pindex = indx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(pindex);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(pindex);
        }

        @Override
        public int getLength() {
            return 1 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("param_index: ");
            sb.append(pindex);
            sb.append('\n');
        }
    }

    /**
     * supertype_target (3.3.2 Class extends and implements)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class supertype_target extends TargetInfo {

        int type_index;

        public supertype_target(TargetType tt, int tindex) {
            super(tt);
            type_index = tindex;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(type_index);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(type_index);
        }

        @Override
        public int getLength() {
            return 2 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("type_index: ");
            sb.append(type_index);
            sb.append('\n');
        }
    }

    /**
     * typeparam_bound_target (3.3.3 Type parameter bounds)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class typeparam_bound_target extends TargetInfo {

        int pindex;
        int bindex;

        public typeparam_bound_target(TargetType tt, int pindx, int bindx) {
            super(tt);
            pindex = pindx;
            bindex = bindx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(pindex);
            out.writeByte(bindex);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(pindex);
            out.print(" ");
            out.print(bindex);
        }

        @Override
        public int getLength() {
            return 2 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("param_index: ");
            sb.append(pindex);
            sb.append('\n');

            sb.append(tabStr);
            sb.append("bound_index: ");
            sb.append(bindex);
            sb.append('\n');
        }
    }

    /**
     * empty_target (3.3.4 )
     *
     * Types without arguments.
     *
     * Basic types without arguments, like field, method return, or method receiver.
     */
    public static class empty_target extends TargetInfo {

        public empty_target(TargetType tt) {
            super(tt);
        }
    }

    /**
     * methodformalparam_target (3.3.5 Method parameters)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class methodformalparam_target extends TargetInfo {

        int index;

        public methodformalparam_target(TargetType tt, int indx) {
            super(tt);
            index = indx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(index);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(index);
        }

        @Override
        public int getLength() {
            return 1 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("index: ");
            sb.append(index);
            sb.append('\n');
        }
    }

    /**
     * throws_target (3.3.6 throws clauses)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class throws_target extends TargetInfo {

        int type_index;

        public throws_target(TargetType tt, int tindex) {
            super(tt);
            type_index = tindex;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(type_index);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(type_index);
        }

        @Override
        public int getLength() {
            return 2 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("type_index: ");
            sb.append(type_index);
            sb.append('\n');
        }
    }

    /**
     * localvar_target (3.3.7 Local variables)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class localvar_target extends TargetInfo {

        public static class LocalVar_Entry {

            public int startPC;
            public int length;
            public int cpx;

            public LocalVar_Entry(int st, int len, int index) {
                startPC = st;
                length = len;
                cpx = index;
            }

            void write(CheckedDataOutputStream out) throws IOException {
                out.writeShort(startPC);
                out.writeShort(length);
                out.writeShort(cpx);
            }

            public void _print(PrintWriter out, String tab) {
                out.print(tab + "{");
                out.print(startPC);
                out.print(" ");
                out.print(length);
                out.print(" ");
                out.print(cpx);
                out.println("}");
            }

            public String toString() {
                return new String("startPC: " + startPC
                        + "  length: " + length
                        + "  cpx: " + cpx);
            }
        }

        ArrayList<LocalVar_Entry> table = null;

        public localvar_target(TargetType tt, int size) {
            super(tt);
            table = new ArrayList<>(size);
        }

        public void addEntry(int startPC, int length, int cpx) {
            LocalVar_Entry entry = new LocalVar_Entry(startPC, length, cpx);
            table.add(entry);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(table.size());
            for (LocalVar_Entry entry : table) {
                entry.write(out);
            }
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            String innerTab = tab + "          ";
            out.println();
            for (LocalVar_Entry entry : table) {
                entry._print(out, innerTab);
            }
            out.print(innerTab);
        }

        @Override
        public int getLength() {
            return 2 + // U2 for table size
                    (6 * table.size()) + // (3 * U2) for each table entry
                    super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr1 = tabString(tabLevel + 1);
            int i = 1;
            for (LocalVar_Entry entry : table) {
                sb.append(tabStr1);
                sb.append("[" + i + "]: ");
                sb.append(entry.toString());
                sb.append('\n');
                i += 1;
            }
        }
    }

    /**
     * catch_target (3.3.8 Exception parameters (catch clauses))
     *
     * Index to the exception type (the type that shows up in a catch clause).
     *
     * These need location information to identify the annotated element.
     */
    public static class catch_target extends TargetInfo {

        int exception_table_index;

        public catch_target(TargetType tt, int indx) {
            super(tt);
            exception_table_index = indx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(exception_table_index);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(exception_table_index);
        }

        @Override
        public int getLength() {
            return 2 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("exception_table_index: ");
            sb.append(exception_table_index);
            sb.append('\n');
        }
    }

    /**
     * offset_target (3.3.9 Typecasts, type tests, and object creation)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class offset_target extends TargetInfo {

        int offset;

        public offset_target(TargetType tt, int ofst) {
            super(tt);
            offset = ofst;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(offset);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(offset);
        }

        @Override
        public int getLength() {
            return 2 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("offset: ");
            sb.append(offset);
            sb.append('\n');
        }
    }

    /**
     * typearg_target (3.3.10 Constructor and method call type arguments)
     *
     * BaseClass for any Type Annotation Target-Info that is in a parameterized type,
     * array, or nested type.
     *
     * These need location information to identify the annotated element.
     */
    public static class typearg_target extends TargetInfo {

        int offset;
        int typeIndex;

        public typearg_target(TargetType tt, int ofst, int tindx) {
            super(tt);
            offset = ofst;
            typeIndex = tindx;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort(offset);
            out.writeByte(typeIndex);
            super.write(out);
        }

        @Override
        public void _print(PrintWriter out, String tab) {
            out.print(" ");
            out.print(offset);
            out.print(" ");
            out.print(typeIndex);
        }

        @Override
        public int getLength() {
            return 3 + super.getLength();
        }

        @Override
        protected void _toString(StringBuilder sb, int tabLevel) {
            String tabStr = tabString(tabLevel);
            sb.append(tabStr);
            sb.append("offset: ");
            sb.append(offset);
            sb.append('\n');

            sb.append(tabStr);
            sb.append("type_index: ");
            sb.append(typeIndex);
            sb.append('\n');
        }
    }

    /*-------------------------------------------------------- */
    /* TypeAnnotationVisitor Methods */
    public static class TypeAnnotationTargetVisitor {

        public final void visit(TargetType tt) {
            switch (tt) {
                case class_type_param:
                case meth_type_param:
                    visit_type_param_target(tt);
                    break;
                case class_exts_impls:
                    visit_supertype_target(tt);
                    break;
                case class_type_param_bnds:
                case meth_type_param_bnds:
                    visit_typeparam_bound_target(tt);
                    break;
                case field:
                case meth_ret_type:
                case meth_reciever:
                    visit_empty_target(tt);
                    break;
                case meth_formal_param:
                    visit_methodformalparam_target(tt);
                    break;
                case throws_type:
                    visit_throws_target(tt);
                    break;
                case local_var:
                case resource_var:
                    visit_localvar_target(tt);
                    break;
                case exception_param:
                    visit_catch_target(tt);
                    break;
                case type_test:
                case obj_creat:
                case constr_ref_receiver:
                case meth_ref_receiver:
                    visit_offset_target(tt);
                    break;

                case cast:
                case constr_invoc_typearg:
                case meth_invoc_typearg:
                case constr_ref_typearg:
                case meth_ref_typearg:

                    visit_typearg_target(tt);
                    break;
            }
        }

        public void visit_type_param_target(TargetType tt) {
        }

        public void visit_supertype_target(TargetType tt) {
        }

        public void visit_typeparam_bound_target(TargetType tt) {
        }

        public void visit_empty_target(TargetType tt) {
        }

        public void visit_methodformalparam_target(TargetType tt) {
        }

        public void visit_throws_target(TargetType tt) {
        }

        public void visit_localvar_target(TargetType tt) {
        }

        public void visit_catch_target(TargetType tt) {
        }

        public void visit_offset_target(TargetType tt) {
        }

        public void visit_typearg_target(TargetType tt) {
        }
    }

    /*-------------------------------------------------------- */

    /*-------------------------------------------------------- */
    /* TypeAnnotationData Methods */
    public TypeAnnotationUtils() {
    }

    public static TargetType getTargetType(int tt_index) {
        if (tt_index < 0 || tt_index > TargetType.maxTag) {
            return null;
        }
        return TargetTypeList.get(tt_index);
    }

    static public TargetType getTargetType(String tt) {
        TargetType retval = TargetTypeHash.get(tt);

        if (retval.name().equals("UNUSED")) {
            retval = null;
        }

        return retval;
    }

}
