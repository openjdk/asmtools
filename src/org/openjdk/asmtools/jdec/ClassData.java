/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdec;

import org.openjdk.asmtools.jasm.Modifiers;
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.*;

import static org.openjdk.asmtools.jasm.Tables.*;
import static org.openjdk.asmtools.jasm.TypeAnnotationUtils.*;

/**
 *
 *
 */
class ClassData {

    byte types[];
    Object cpool[];
    int CPlen;
    NestedByteArrayInputStream countedin;
    DataInputStream in;
    PrintWriter out;
    String inpname;
    int[] cpe_pos;
    boolean printDetails;
    String entityname;


    public static I18NResourceBundle i18n
            = I18NResourceBundle.getBundleForClass(Main.class);

    public ClassData(String inpname, int printFlags, PrintWriter out) throws IOException {
        entityname = (inpname.endsWith("module-info.class")) ? "module" : "class";
        FileInputStream filein = new FileInputStream(inpname);
        byte buf[] = new byte[filein.available()];
        filein.read(buf);
        countedin = new NestedByteArrayInputStream(buf);
        in = new DataInputStream(countedin);
        this.out = out;
        this.inpname = inpname;
        printDetails = ((printFlags & 1) == 1);
    }

    /*========================================================*/
    public static final char hexTable[] = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    String toHex(long val, int width) {
        StringBuilder s = new StringBuilder();
        for (int i = width * 2 - 1; i >= 0; i--) {
            s.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
        }
        return "0x" + s.toString();
    }

    String toHex(long val) {
        int width;
        for (width = 8; width > 0; width--) {
            if ((val >> (width - 1) * 8) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

    void printByteHex(PrintWriter out, int b) {
        out.print(hexTable[(b >> 4) & 0xF]);
        out.print(hexTable[b & 0xF]);
    }

    void printBytes(PrintWriter out, DataInputStream in, int len)
            throws IOException {
        try {
            for (int i = 0; i < len; i++) {
                if (i % 8 == 0) {
                    out_print("0x");
                }
                printByteHex(out, in.readByte());
                if (i % 8 == 7) {
                    out.println(";");
                }
            }
        } finally {
            if (len % 8 != 0) {
                out.println(";");
            }
        }
    }

    void printRestOfBytes() throws IOException {
        for (int i = 0;; i++) {
            try {
                if (i % 8 == 0) {
                    out_print("0x");
                }
                printByteHex(out, in.readByte());
                if (i % 8 == 7) {
                    out.print(";\n");
                }
            } catch (IOException e) {
                out.println();
                return;
            }
        }
    }

    /*========================================================*/
    int shift = 0;

    void out_begin(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
        shift++;
    }

    void out_print(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.print(s);
    }

    void out_println(String s) {
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    void out_end(String s) {
        shift--;
        for (int i = 0; i < shift; i++) {
            out.print("  ");
        }
        out.println(s);
    }

    String startArray(int length) {
        return "[" + (printDetails ? Integer.toString(length) : "") + "]";
    }

    void startArrayCmt(int length, String comment) {
        out_begin(startArray(length) + " { // " + comment);
    }

    void startArrayCmtB(int length, String comment) {
        out_begin(startArray(length) + "b { // " + comment);
    }

    /*========================================================*/
    void readCP(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        CPlen = length;
        traceln(i18n.getString("jdec.trace.CP_len", length));
        types = new byte[length];
        cpool = new Object[length];
        cpe_pos = new int[length];
        for (int i = 1; i < length; i++) {
            byte btag;
            int v1, v2, n = i;
            long lv;
            cpe_pos[i] = countedin.getPos();
            btag = in.readByte();
            traceln(i18n.getString("jdec.trace.CP_entry", i, btag));
            types[i] = btag;
            ConstType tg = tag(btag);
            switch (tg) {
                case CONSTANT_UTF8:
                    cpool[i] = in.readUTF();
                    break;
                case CONSTANT_INTEGER:
                    v1 = in.readInt();
                    cpool[i] = new Integer(v1);
                    break;
                case CONSTANT_FLOAT:
                    v1 = Float.floatToIntBits(in.readFloat());
                    cpool[i] = new Integer(v1);
                    break;
                case CONSTANT_LONG:
                    lv = in.readLong();
                    cpool[i] = new Long(lv);
                    i++;
                    break;
                case CONSTANT_DOUBLE:
                    lv = Double.doubleToLongBits(in.readDouble());
                    cpool[i] = new Long(lv);
                    i++;
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                    v1 = in.readUnsignedShort();
                    cpool[i] = new Integer(v1);
                    break;
                case CONSTANT_INTERFACEMETHOD:
                case CONSTANT_FIELD:
                case CONSTANT_METHOD:
                case CONSTANT_NAMEANDTYPE:
                    cpool[i] = "#" + in.readUnsignedShort() + " #" + in.readUnsignedShort();
                    break;
                case CONSTANT_INVOKEDYNAMIC:
                    cpool[i] = in.readUnsignedShort() + "s #" + in.readUnsignedShort();
                    break;
                case CONSTANT_METHODHANDLE:
                    cpool[i] = in.readUnsignedByte() + "b #" + in.readUnsignedShort();
                    break;
                case CONSTANT_METHODTYPE:
                    cpool[i] = "#" + in.readUnsignedShort();
                    break;
                default:
                    CPlen = i;
                    printCP(out);
                    out_println(toHex(btag, 1) + "; // invalid constant type: " + (int) btag + " for element " + i);
                    throw new ClassFormatError();
            }
        }
    }

    void printCP(PrintWriter out) throws IOException {
        int length = CPlen;
        startArrayCmt(length, "Constant Pool");
        out_println("; // first element is empty");
        int size;
        for (int i = 1; i < length; i = i + size) {
            size = 1;
            byte btag = types[i];
            ConstType tg = tag(btag);
            int pos = cpe_pos[i];
            String tagstr = "";
            String valstr;
            int v1, v2;
            long lv;
            if (tg != null) {
                tagstr = tg.parseKey();
            }
            switch (tg) {
                case CONSTANT_UTF8: {
                    tagstr = "Utf8";
                    StringBuilder sb = new StringBuilder();
                    String s = (String) cpool[i];
                    sb.append('\"');
                    for (int k = 0; k < s.length(); k++) {
                        char c = s.charAt(k);
                        switch (c) {
                            case '\t':
                                sb.append('\\').append('t');
                                break;
                            case '\n':
                                sb.append('\\').append('n');
                                break;
                            case '\r':
                                sb.append('\\').append('r');
                                break;
                            case '\"':
                                sb.append('\\').append('\"');
                                break;
                            default:
                                sb.append(c);
                        }
                    }
                    valstr = sb.append('\"').toString();
                }
                break;
                case CONSTANT_FLOAT:
                    v1 = ((Integer) cpool[i]).intValue();
                    valstr = toHex(v1, 4);
                    break;
                case CONSTANT_INTEGER:
                    v1 = ((Integer) cpool[i]).intValue();
                    valstr = toHex(v1, 4);
                    break;
                case CONSTANT_DOUBLE:
                    lv = ((Long) cpool[i]).longValue();
                    valstr = toHex(lv, 8) + ";";
                    size = 2;
                    break;
                case CONSTANT_LONG:
                    lv = ((Long) cpool[i]).longValue();
                    valstr = toHex(lv, 8) + ";";
                    size = 2;
                    break;
                case CONSTANT_CLASS:
                    v1 = ((Integer) cpool[i]).intValue();
                    valstr = "#" + v1;
                    break;
                case CONSTANT_STRING:
                    v1 = ((Integer) cpool[i]).intValue();
                    valstr = "#" + v1;
                    break;
                case CONSTANT_INTERFACEMETHOD:
                    valstr = (String) cpool[i];
                    break;
                case CONSTANT_FIELD:
                    valstr = (String) cpool[i];
                    break;
                case CONSTANT_METHOD:
                    valstr = (String) cpool[i];
                    break;
                case CONSTANT_NAMEANDTYPE:
                    valstr = (String) cpool[i];
                    break;
                case CONSTANT_METHODHANDLE:
                    valstr = (String) cpool[i];
                    break;
                case CONSTANT_METHODTYPE:
                    valstr = (String) cpool[i];
                    break;
//                case CONSTANT_INVOKEDYNAMIC_TRANS:
//                    tagstr = "InvokeDynamicTrans";
//                    valstr = (String) cpool[i];
//                    break;
                case CONSTANT_INVOKEDYNAMIC:
                    valstr = (String) cpool[i];
                    break;
                default:
                    throw new Error("invalid constant type: " + (int) btag);
            }
            out_print(tagstr + " " + valstr + "; // #" + i);
            if (printDetails) {
                out_println(" at " + toHex(pos));
            } else {
                out_println("");
            }
        }
        out_end("} // Constant Pool");
        out.println();
    }

    String getStringPos() {
        return " at " + toHex(countedin.getPos());
    }

    String getStringPosCond() {
        if (printDetails) {
            return getStringPos();
        } else {
            return "";
        }
    }

    String getCommentPosCond() {
        if (printDetails) {
            return " // " + getStringPos();
        } else {
            return "";
        }
    }

    void decodeCPXAttr(DataInputStream in, int len, String attrname, PrintWriter out) throws IOException {
        decodeCPXAttrM(in, len, attrname, out, 1);
    }

    void decodeCPXAttrM(DataInputStream in, int len, String attrname, PrintWriter out, int expectedIndices) throws IOException {
        if (len != expectedIndices * 2) {
            out_println("// invalid length of " + attrname + " attr: " + len + " (should be " + (expectedIndices * 2) + ") > ");
            printBytes(out, in, len);
        } else {
            String outputString = "";
            String space = "";
            for (int k = 0; k < expectedIndices; k++) {
                outputString += (space + "#" + in.readUnsignedShort());
                space = " ";
            }
            out_println(outputString + ";");
        }
    }

    void printStackMap(DataInputStream in, int elementsNum) throws IOException {
        int num;
        if (elementsNum > 0) {
            num = elementsNum;
        } else {
            num = in.readUnsignedShort();
        }
        out.print(startArray(num) + (elementsNum > 0 ? "z" : "") + "{");
        for (int k = 0; k < num; k++) {
            int maptype = in.readUnsignedByte();
            StackMapType mptyp = stackMapType(maptype);
            String maptypeImg;
            if (printDetails) {
                maptypeImg = Integer.toString(maptype) + "b";
            } else {
                try {
                    maptypeImg = mptyp.parsekey();
                    // maptypeImg = ITEM_Names[maptype];
                } catch (ArrayIndexOutOfBoundsException e) {
                    maptypeImg = "/* BAD TYPE: */ " + maptype + "b";
                }
            }
            switch (mptyp) {
                case ITEM_Object:
                case ITEM_NewObject:
                    maptypeImg = maptypeImg + "," + in.readUnsignedShort();
                    break;
                default:
            }
            out.print(maptypeImg);
            if (k < num - 1) {
                out.print("; ");
            }
        }
        out.print("}");
    }

    /**
     * Processes 4.7.20 The RuntimeVisibleTypeAnnotations Attribute, 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
     * <code>type_annotation</code> structure.
     */
    void decodeTargetTypeAndRefInfo(DataInputStream in, PrintWriter out, boolean isWildcard) throws IOException {
        int tt = in.readUnsignedByte(); // [4.7.20] annotations[], type_annotation { u1 target_type; ...}
        TargetType target_type = targetTypeEnum(tt);
        InfoType info_type = target_type.infoType();
        out_println(toHex(tt, 1) + ";  //  target_type: " + target_type.parseKey());

        switch (info_type) {
            case TYPEPARAM:          //[3.3.1] meth_type_param, class_type_param:
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  param_index");
                break;
            case SUPERTYPE:         //[3.3.2]  class_exts_impls
                out_println(toHex(in.readUnsignedShort(), 2) + ";  //  type_index");
                break;
            case TYPEPARAM_BOUND:   //[3.3.3]  class_type_param_bnds, meth_type_param_bnds
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  param_index");
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  bound_index");
                break;
            case EMPTY:             //[3.3.4]  meth_receiver, meth_ret_type, field
                // NOTE: reference_info is empty for this annotation's target
                break;
            case METHODPARAM:       //[3.3.5]  meth_formal_param:
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  parameter_index");
                break;
            case EXCEPTION:         //[3.3.61]  throws_type
                //KTL:  Updated index to UShort for JSR308 change
                out_println(in.readUnsignedShort() + ";  //  type_index");
                break;
            case LOCALVAR: //[3.3.7]  local_var, resource_var
            {
                int lv_num = in.readUnsignedShort();
                startArrayCmt(lv_num, "local_variables");
                for (int i = 0; i < lv_num; i++) {
                    out_println(in.readUnsignedShort() + " " + in.readUnsignedShort()
                            + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                }
                out_end("}");
            }
            break;
            case CATCH:             //[3.3.8]  exception_param
                out_println(in.readUnsignedShort() + ";  //  exception_table_index");
                break;
            case OFFSET:            //[3.3.9]  type_test (instanceof), obj_creat (new)
                // constr_ref_receiver, meth_ref_receiver
                out_println(in.readUnsignedShort() + ";  //  offset");
                break;
            case TYPEARG:           //[3.3.10]  cast, constr_ref_typearg, meth_invoc_typearg
                // constr_invoc_typearg, meth_ref_typearg
                out_println(in.readUnsignedShort() + ";  //  offset");
                out_println(toHex(in.readUnsignedByte(), 1) + ";  //  type_index");
                break;
            default:                // should never happen
                out_println(toHex(tt, 1) + "; // invalid target_info: " + tt);
                throw new ClassFormatError();
        }
        // [4.7.20.2]
        int path_length = in.readUnsignedByte();  // type_path { u1 path_length; ...}
        startArrayCmt(path_length, "type_paths");
        for (int i = 0; i < path_length; i++) {
            // print the type_path elements
            out_println("{ " + toHex(in.readUnsignedByte(), 1)  // { u1 type_path_kind;
                    + "; " + toHex(in.readUnsignedByte(), 1)    //   u1 type_argument_index; }
                    + "; } // type_path[" + i + "]");           // path[i]
        }
        out_end("}");

    }

    void decodeElementValue(DataInputStream in, int len, PrintWriter out) throws IOException {
        out_begin("{  //  element_value");
        char tg = (char) in.readByte();
        AnnotElemType tag = annotElemType(tg);
        out_println("'" + tg + "';");
        switch (tag) {
            case AE_BYTE:
            case AE_CHAR:
            case AE_DOUBLE:
            case AE_FLOAT:
            case AE_INT:
            case AE_LONG:
            case AE_SHORT:
            case AE_BOOLEAN:
            case AE_STRING:
                decodeCPXAttr(in, 2, "const_value_index", out);
                break;
            case AE_ENUM:
                out_begin("{  //  enum_const_value");
                decodeCPXAttr(in, 2, "type_name_index", out);
                decodeCPXAttr(in, 2, "const_name_index", out);
                out_end("}  //  enum_const_value");
                break;
            case AE_CLASS:
                decodeCPXAttr(in, 2, "class_info_index", out);
                break;
            case AE_ANNOTATION:
                decodeAnnotation(in, out);
                break;
            case AE_ARRAY:
                int ev_num = in.readUnsignedShort();
                startArrayCmt(ev_num, "array_value");
                for (int i = 0; i < ev_num; i++) {
                    decodeElementValue(in, 0, out);
                    if (i < ev_num - 1) {
                        out_println(";");
                    }
                }
                out_end("}  //  array_value");
                break;
            default:
                out_println(toHex(tg, 1) + "; // invalid element_value tag type: " + tg);
                throw new ClassFormatError();
        }
        out_end("}  //  element_value");
    }

    void decodeAnnotation(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  annotation");
        decodeCPXAttr(in, 2, "field descriptor", out);
        int evp_num = in.readUnsignedShort();
        startArrayCmt(evp_num, "element_value_pairs");
        for (int i = 0; i < evp_num; i++) {
            out_begin("{  //  element value pair");
            decodeCPXAttr(in, 2, "name of the annotation type element", out);
            decodeElementValue(in, 0, out);
            out_end("}  //  element value pair");
            if (i < evp_num - 1) {
                out_println(";");
            }
        }
        out_end("}  //  element_value_pairs");
        out_end("}  //  annotation");
    }

    void decodeTypeAnnotation(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  type_annotation");
        decodeTargetTypeAndRefInfo(in, out, false);
        decodeCPXAttr(in, 2, "field descriptor", out);
        int evp_num = in.readUnsignedShort();
        startArrayCmt(evp_num, "element_value_pairs");
        for (int i = 0; i < evp_num; i++) {
            out_begin("{  //  element value pair");
            decodeCPXAttr(in, 2, "name of the annotation type element", out);
            decodeElementValue(in, 0, out);
            out_end("}  //  element value pair");
            if (i < evp_num - 1) {
                out_println(";");
            }
        }
        out_end("}  //  element_value_pairs");
        out_end("}  //  type_annotation");
    }

    void decodeBootstrapMethod(DataInputStream in, PrintWriter out) throws IOException {
        out_begin("{  //  bootstrap_method");
        out_println("#" + in.readUnsignedShort() + "; // bootstrap_method_ref");
        int bm_args_cnt = in.readUnsignedShort();
        startArrayCmt(bm_args_cnt, "bootstrap_arguments");
        for (int i = 0; i < bm_args_cnt; i++) {
            out_println("#" + in.readUnsignedShort() + ";" + getCommentPosCond());
        }
        out_end("}  //  bootstrap_arguments");
        out_end("}  //  bootstrap_method");
    }

    void decodeAttr(DataInputStream in0, PrintWriter out) throws IOException {
        // Read one attribute
        String posComment = getStringPos();
        int name_cpx = in0.readUnsignedShort(), btag, len;

        String AttrName = "";
        try {
            btag = types[name_cpx];
            ConstType tag = tag(btag);

            if (tag == ConstType.CONSTANT_UTF8) {
                AttrName = (String) cpool[name_cpx];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        AttrTag tg = attrtag(AttrName);
        String endingComment = AttrName;
        len = in0.readInt();
        countedin.enter(len);
        try {
            if (printDetails) {
                out_begin("Attr(#" + name_cpx + ", " + len + ") { // " + AttrName + posComment);
            } else {
                out_begin("Attr(#" + name_cpx + ") { // " + AttrName);
            }

            switch (tg) {
                case ATT_Code:
                    out_println(in.readUnsignedShort() + "; // max_stack");
                    out_println(in.readUnsignedShort() + "; // max_locals");
                    int code_len = in.readInt();
                    out_begin("Bytes" + startArray(code_len) + "{");
                    printBytes(out, in, code_len);
                    out_end("};");
                    int trap_num = in.readUnsignedShort();
                    startArrayCmt(trap_num, "Traps");
                    for (int i = 0; i < trap_num; i++) {
                        out_println(in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("} // end Traps");

                    // Read the attributes
                    decodeAttrs(in, out);
                    break;
                case ATT_ConstantValue:
                    decodeCPXAttr(in, len, AttrName, out);
                    break;
                case ATT_Exceptions:
                    int exc_num = in.readUnsignedShort();
                    startArrayCmt(exc_num, AttrName);
                    for (int i = 0; i < exc_num; i++) {
                        out_println("#" + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("}");
                    break;
                case ATT_LineNumberTable:
                    int ll_num = in.readUnsignedShort();
                    startArrayCmt(ll_num, AttrName);
                    for (int i = 0; i < ll_num; i++) {
                        out_println(in.readUnsignedShort() + "  " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("}");
                    break;
                case ATT_LocalVariableTable:
                    int lv_num = in.readUnsignedShort();
                    startArrayCmt(lv_num, AttrName);
                    for (int i = 0; i < lv_num; i++) {
                        out_println(in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("}");
                    break;
                case ATT_LocalVariableTypeTable:
                    int lvt_num = in.readUnsignedShort();
                    startArrayCmt(lvt_num, AttrName);
                    for (int i = 0; i < lvt_num; i++) {
                        out_println(in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("}");
                    break;
                case ATT_InnerClasses:
                    int ic_num = in.readUnsignedShort();
                    startArrayCmt(ic_num, AttrName);
                    for (int i = 0; i < ic_num; i++) {
                        out_println("#" + in.readUnsignedShort() + " #" + in.readUnsignedShort() + " #" + in.readUnsignedShort() + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                    out_end("}");
                    break;
                case ATT_Signature:
                    decodeCPXAttr(in, len, AttrName, out);
                    break;
                case ATT_StackMap:
                    int e_num = in.readUnsignedShort();
                    startArrayCmt(e_num, "");
                    for (int k = 0; k < e_num; k++) {
                        int start_pc = in.readUnsignedShort();
                        out_print("" + start_pc + ", ");
                        printStackMap(in, 0);
                        out.print(", ");
                        printStackMap(in, 0);
                        out.println(";");
                    }
                    out_end("}");
                    break;
                case ATT_StackMapTable:
                    int et_num = in.readUnsignedShort();
                    startArrayCmt(et_num, "");
                    for (int k = 0; k < et_num; k++) {
                        int frame_type = in.readUnsignedByte();
                        StackMapFrameType ftype = stackMapFrameType(frame_type);
                        switch (ftype) {
                            case SAME_FRAME:
                                // type is same_frame;
                                out_print("" + frame_type + "b");
                                out.println("; // same_frame");
                                break;
                            case SAME_LOCALS_1_STACK_ITEM_FRAME:
                                // type is same_locals_1_stack_item_frame
                                int offset = frame_type - 64;
                                out_print("" + frame_type + "b, ");
                                // read additional single stack element
                                printStackMap(in, 1);
                                out.println("; // same_locals_1_stack_item_frame");
                                break;
                            case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME:
                                // type is same_locals_1_stack_item_frame_extended
                                int noffset = in.readUnsignedShort();
                                out_print("" + frame_type + "b, " + noffset + ", ");
                                // read additional single stack element
                                printStackMap(in, 1);
                                out.println("; // same_locals_1_stack_item_frame_extended");
                                break;
                            case CHOP_1_FRAME:
                            case CHOP_2_FRAME:
                            case CHOP_3_FRAME:
                                // type is chop_frame
                                int coffset = in.readUnsignedShort();
                                out_print("" + frame_type + "b, " + coffset);
                                out.println("; // chop_frame " + (251 - frame_type));
                                break;
                            case SAME_FRAME_EX:
                                // type is same_frame_extended;
                                int xoffset = in.readUnsignedShort();
                                out_print("" + frame_type + "b, " + xoffset);
                                out.println("; // same_frame_extended");
                                break;
                            case APPEND_FRAME:
                                // type is append_frame
                                int aoffset = in.readUnsignedShort();
                                out_print("" + frame_type + "b, " + aoffset + ", ");
                                // read additional locals
                                printStackMap(in, frame_type - 251);
                                out.println("; // append_frame " + (frame_type - 251));
                                break;
                            case FULL_FRAME:
                                // type is full_frame
                                int foffset = in.readUnsignedShort();
                                out_print("" + frame_type + "b, " + foffset + ", ");
                                printStackMap(in, 0);
                                out.print(", ");
                                printStackMap(in, 0);
                                out.println("; // full_frame");
                                break;
                        }
                    }
                    out_end("}");
                    break;
                case ATT_EnclosingMethod:
                    decodeCPXAttrM(in, len, AttrName, out, 2);
                    break;
                case ATT_SourceFile:
                    decodeCPXAttr(in, len, AttrName, out);
                    break;
                case ATT_AnnotationDefault:
                    decodeElementValue(in, len, out);
                    break;
                case ATT_RuntimeInvisibleAnnotations:
                case ATT_RuntimeVisibleAnnotations:
                    int an_num = in.readUnsignedShort();
                    startArrayCmt(an_num, "annotations");
                    for (int i = 0; i < an_num; i++) {
                        decodeAnnotation(in, out);
                        if (i < an_num - 1) {
                            out_println(";");
                        }
                    }
                    out_end("}");
                    break;
                // 4.7.20 The RuntimeVisibleTypeAnnotations Attribute
                // 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
                case ATT_RuntimeInvisibleTypeAnnotations:
                case ATT_RuntimeVisibleTypeAnnotations:
                    int ant_num = in.readUnsignedShort();
                    startArrayCmt(ant_num, "annotations");
                    for (int i = 0; i < ant_num; i++) {
                        decodeTypeAnnotation(in, out);
                        if (i < ant_num - 1) {
                            out_println(";");
                        }
                    }
                    out_end("}");
                    break;
                case ATT_RuntimeInvisibleParameterAnnotations:
                case ATT_RuntimeVisibleParameterAnnotations:
                    int pm_num = in.readUnsignedByte();
                    startArrayCmtB(pm_num, "parameters");
                    for (int k = 0; k < pm_num; k++) {
                        int anp_num = in.readUnsignedShort();
                        startArrayCmt(anp_num, "annotations");
                        for (int i = 0; i < anp_num; i++) {
                            decodeAnnotation(in, out);
                            if (k < anp_num - 1) {
                                out_println(";");
                            }
                        }
                        out_end("}");
                        if (k < pm_num - 1) {
                            out_println(";");
                        }
                    }
                    out_end("}");
                    break;
                case ATT_BootstrapMethods:
                    int bm_num = in.readUnsignedShort();
                    startArrayCmt(bm_num, "bootstrap_methods");
                    for (int i = 0; i < bm_num; i++) {
                        decodeBootstrapMethod(in, out);
                        if (i < bm_num - 1) {
                            out_println(";");
                        }
                    }
                    out_end("}");
                    break;
                case ATT_Module:
                    decodeModule(in, out);
                    break;
                default:
                    if (AttrName == null) {
                        printBytes(out, in, len);
                        endingComment = "Attr(#" + name_cpx + ")";
                    } else {
                        // some kind of error?
                        printBytes(out, in, len);
                    }
            }

        } catch (EOFException e) {
            out.println("// ======== unexpected end of attribute array");
        }
        int rest = countedin.available();
        if (rest > 0) {
            out.println("// ======== attribute array started " + posComment + " has " + rest + " bytes more:");
            printBytes(out, in, rest);
        }
        out_end("} // end " + endingComment);
        countedin.leave();
    }

    void decodeModule(DataInputStream in, PrintWriter out) throws IOException {
        int count = in.readUnsignedShort(); // u2 requires_count
        startArrayCmt(count, "requires");
        for (int i = 0; i < count; i++) {
            // u2 requires_index; u2 requires_flag
            out_println("#" + in.readUnsignedShort() + " " + toHex(in.readUnsignedShort(), 2) + ";");
        }
        out_end("} // requires\n");

//        count = in.readUnsignedShort();     // u2 permits_count
//        startArrayCmt(count, "permits");
//        for (int i = 0; i < count; i++) {
//            // u2 permits_index
//            out_println("#" + in.readUnsignedShort() + ";");
//        }
//        out_end("} // permits\n");

        count = in.readUnsignedShort();     // u2 exports_count
        startArrayCmt(count, "exports");
        for (int i = 0; i < count; i++) {
            // u2 exports_index
            out_println("#" + in.readUnsignedShort());
            int exports_to_count = in.readUnsignedShort();
            startArrayCmt(exports_to_count, "to");
            for (int j = 0; j < exports_to_count; j++) {
                out_println("#" + in.readUnsignedShort() + ";");
            }
            out_end("}; // end to");
        }
        out_end("} // exports\n");
        count = in.readUnsignedShort();     // u2 uses_count
        startArrayCmt(count, "uses");
        for (int i = 0; i < count; i++) {
            // u2 uses_index
            out_println("#" + in.readUnsignedShort() + ";");
        }
        out_end("} // uses\n");
        count = in.readUnsignedShort(); // u2 provides_count
        startArrayCmt(count, "provides");
        for (int i = 0; i < count; i++) {
            // u2 provides_index; u2 with_index
            out_println("#" + in.readUnsignedShort() + " #" + in.readUnsignedShort() + ";");
        }
        out_end("} // provides\n");
    }

    void decodeAttrs(DataInputStream in, PrintWriter out) throws IOException {
        // Read the attributes
        int attr_num = in.readUnsignedShort();
        startArrayCmt(attr_num, "Attributes");
        for (int i = 0; i < attr_num; i++) {
            decodeAttr(in, out);
            if (i + 1 < attr_num) {
                out_println(";");
            }
        }
        out_end("} // Attributes");
    }

    void decodeMembers(DataInputStream in, PrintWriter out, String comment) throws IOException {
        int nfields = in.readUnsignedShort();
        traceln(comment + "=" + nfields);
        startArrayCmt(nfields, "" + comment);
        try {
            for (int i = 0; i < nfields; i++) {
                out_begin("{ // Member" + getStringPosCond());
                int access = in.readShort();
                out_println(toHex(access, 2) + "; // access");
                int name_cpx = in.readUnsignedShort();
                out_println("#" + name_cpx + "; // name_cpx");
                int sig_cpx = in.readUnsignedShort();
                out_println("#" + sig_cpx + "; // sig_cpx");
                // Read the attributes
                decodeAttrs(in, out);
                out_end("} // Member");
                if (i + 1 < nfields) {
                    out_println(";");
                }
            }
        } finally {
            out_end("} // " + comment);
            out.println();
        }
    }

    public void decodeClass() throws IOException {
        String classname  = "N/A";
        // Read the header
        try {
            int magic = in.readInt();
            int min_version = in.readUnsignedShort();
            int version = in.readUnsignedShort();

            // Read the constant pool
            readCP(in);
            short access = in.readShort(); // don't care about sign
            int this_cpx = in.readUnsignedShort();

            try {
                classname = (String) cpool[((Integer) cpool[this_cpx]).intValue()];
                int ind = classname.lastIndexOf("module-info");
                if( ind > -1) {
                    entityname = "module";
                    classname = classname.substring(0, --ind < 0 ? 0 : ind ).replace('/', '.');
                }
                out_begin(String.format("%s %s {", entityname, classname));
            } catch (Exception e) {
                classname = inpname;
                out.println("// " + e.getMessage() + " while accessing classname");
                out_begin(String.format("%s %s { // source file name", entityname, classname));
            }

            out_print(toHex(magic, 4) + ";");
            if (magic != JAVA_MAGIC) {
                out.print(" // wrong magic: 0x" + Integer.toString(JAVA_MAGIC, 16) + " expected");
            }
            out.println();
            out_println(min_version + "; // minor version");
            out_println(version + "; // version");

            // Print the constant pool
            printCP(out);
            out_println(toHex(access, 2) + "; // access"  +
            ( printDetails ? " [" +  (" " + Modifiers.accessString(access, CF_Context.CTX_CLASS).toUpperCase()).replaceAll(" (\\S)"," ACC_$1") + "]" : "" ));
            out_println("#" + this_cpx + ";// this_cpx");
            int super_cpx = in.readUnsignedShort();
            out_println("#" + super_cpx + ";// super_cpx");
            traceln(i18n.getString("jdec.trace.access_thisCpx_superCpx", access, this_cpx, super_cpx));
            out.println();

            // Read the interface names
            int numinterfaces = in.readUnsignedShort();
            traceln(i18n.getString("jdec.trace.numinterfaces", numinterfaces));
            startArrayCmt(numinterfaces, "Interfaces");
            for (int i = 0; i < numinterfaces; i++) {
                int intrf_cpx = in.readUnsignedShort();
                traceln(i18n.getString("jdec.trace.intrf", i, intrf_cpx));
                out_println("#" + intrf_cpx + ";");
            }
            out_end("} // Interfaces\n");

            // Read the fields
            decodeMembers(in, out, "fields");

            // Read the methods
            decodeMembers(in, out, "methods");

            // Read the attributes
            decodeAttrs(in, out);
        } catch (EOFException e) {
        } catch (ClassFormatError err) {
            out.println("//------- ClassFormatError:" + err.getMessage());
            printRestOfBytes();
        } finally {
            out_end(String.format("} // end %s %s", entityname, classname));
        }
    } // end decodeClass()
    /* ====================================================== */
    public boolean DebugFlag = false;

    public void trace(String s) {
        if (!DebugFlag) {
            return;
        }
        System.out.print(s);
    }

    public void traceln(String s) {
        if (!DebugFlag) {
            return;
        }
        System.out.println(s);
    }
}// end class ClassData

