/*
 * Copyright (c) 2009, 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.asmutils.StringUtils;
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.common.structure.StackMap;
import org.openjdk.asmtools.jcoder.JcodTokens;

import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static java.lang.String.format;
import static org.openjdk.asmtools.Main.sharedI18n;
import static org.openjdk.asmtools.asmutils.StringUtils.*;
import static org.openjdk.asmtools.common.structure.ClassFileContext.MODULE_DIRECTIVES;
import static org.openjdk.asmtools.jasm.ClassFileConst.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.AnnotationElementType.AE_UNKNOWN;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_MODULE;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.ETargetInfo;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.ETargetType;
import static org.openjdk.asmtools.jcoder.JcodTokens.Token.IDENT;

/**
 * Class data of the Java Decoder
 */
class ClassData {

    private static final int COMMENT_OFFSET = 32;
    private static final String INDENT_STRING = "  ";
    private static final int INDENT_LENGTH = INDENT_STRING.length();
    /*========================================================*/

    private final NestedByteArrayInputStream arrayInputStream;
    private final DataInputStream inputStream;
    protected JdecEnvironment environment;
    /* ====================================================== */
    private byte[] types;
    private Object[] cpool;
    private int CPlen;
    private int[] cpe_pos;
    private String entityType = "";
    private String entityName = "";
    /*========================================================*/
    private int indent = 0;

    ClassData(JdecEnvironment environment) throws IOException, URISyntaxException {
        this.environment = environment;
        //
        try (DataInputStream dis = environment.getInputFile().getDataInputStream(Optional.empty())) {
            byte[] buf = new byte[dis.available()];
            if (dis.read(buf) <= 0) {
                throw new FormatError(environment.getLogger(),
                        "err.file.empty", environment.getSimpleInputFileName());
            }
            arrayInputStream = new NestedByteArrayInputStream(buf);
            inputStream = new DataInputStream(arrayInputStream);
        }
    }

    private String toHex(long val, int width) {
        StringBuilder s = new StringBuilder();
        for (int i = width * 2 - 1; i >= 0; i--) {
            s.append(hexTable[((int) (val >> (4 * i))) & 0xF]);
        }
        return "0x" + s;
    }

    private String toHex(long val) {
        int width;
        for (width = 8; width > 0; width--) {
            if ((val >> (width - 1) * 8) != 0) {
                break;
            }
        }
        return toHex(val, width);
    }

    private void printByteHex(int b) {
        environment.print(hexTable[(b >> 4) & 0xF]);
        environment.print(hexTable[b & 0xF]);
    }

    /**
     *
     * @param in              input stream to get bytes for printing
     * @param len             number of bytes
     * @param printSeparately defines a format  of printed lines which will be either  0x04 0x3C 0x04 0x3D; or 0x043C043D043E1B1C;
     * @throws IOException exception might happen while reading DataInputStream
     **/
    private void printBytes(DataInputStream in, int len, boolean printSeparately) throws IOException {
        int i = 0;
        final int BYTES_IN_LINE = printSeparately ? 4 : 8;
        try {
            for (; i < len; i++) {
                if (i % BYTES_IN_LINE == 0) {
                    out_print(printSeparately ? "" : "0x");
                }
                if (printSeparately) {
                    environment.print("0x");
                }
                printByteHex(in.readByte());
                if (printSeparately) {
                    if (i % BYTES_IN_LINE == BYTES_IN_LINE - 1) {
                        environment.println(";");
                    } else if (i + 1 != len) {
                        environment.print(" ");
                    }
                } else {
                    if (i % BYTES_IN_LINE == BYTES_IN_LINE - 1) {
                        environment.println(";");
                    }
                }
            }
        } finally {
            if (len % 8 != 0) {
                if (i > 0)
                    environment.println(";");
                else
                    out_println(";");
            }
        }
    }

    private void printUtf8String(DataInputStream in, int len) throws IOException {
        final int CHARS_IN_LINE = 78;
        readUtf8String(in, len, CHARS_IN_LINE).forEach(s -> environment.println(getOutString("") + s));
    }

    private void printRestOfBytes() {
        for (int i = 0; ; i++) {
            try {
                byte b = inputStream.readByte();
                if (i % 8 == 0) {
                    out_print("0x");
                }
                printByteHex(b);
                if (i % 8 == 7) {
                    environment.println(";");
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    private void printUtf8InfoIndex(int index, String indexName) {
        String name = (String) cpool[index];
        out_print("#" + index + "; // ");
        if (environment.printDetailsFlag) {
            environment.println(format("%-16s", indexName) + " : " + name);
        } else {
            environment.println(indexName);
        }
    }

    private void out_begin(String s) {
        environment.println(getOutString(s));
        indent++;
    }

    private void out_print(String s) {
        environment.print(getOutString(s));
    }

    private void out_println(String s) {
        environment.println(getOutString(s));
    }

    private String getOutString(String s) {
        s = formatComments(s, indent);
        return repeat(INDENT_STRING, indent) + s;
    }

    private void out_end(String s) {
        s = formatComments(s, indent - 1);
        environment.println(repeat(INDENT_STRING, --indent) + s);
    }

    private String startArray(int length) {
        return "[" + (environment.printDetailsFlag ? Integer.toString(length) : "") + "]";
    }

    private void startArrayCmt(int length, String comment) {
        out_begin(startArray(length) + format(" {%s", comment == null ? "" : " // " + comment));
    }

    private void startArrayCmtB(int length, String comment) {
        out_begin(startArray(length) + format("b {%s", comment == null ? "" : " // " + comment));
    }

    private void readCP(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        CPlen = length;
        environment.traceln("jdec.trace.CP_len", length);
        types = new byte[length];
        cpool = new Object[length];
        cpe_pos = new int[length];
        for (int i = 1; i < length; i++) {
            byte btag;
            int v1;
            long lv;
            cpe_pos[i] = arrayInputStream.getPos();
            btag = in.readByte();
            environment.traceln("jdec.trace.CP_entry", i, btag);
            types[i] = btag;
            ConstType tg = tag(btag);
            switch (tg) {
                case CONSTANT_UTF8 -> cpool[i] = in.readUTF();
                case CONSTANT_INTEGER -> {
                    v1 = in.readInt();
                    cpool[i] = v1;
                }
                case CONSTANT_FLOAT -> {
                    v1 = Float.floatToIntBits(in.readFloat());
                    cpool[i] = v1;
                }
                case CONSTANT_LONG -> {
                    lv = in.readLong();
                    cpool[i] = lv;
                    i++;
                }
                case CONSTANT_DOUBLE -> {
                    lv = Double.doubleToLongBits(in.readDouble());
                    cpool[i] = lv;
                    i++;
                }
                case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_MODULE, CONSTANT_PACKAGE -> {
                    v1 = in.readUnsignedShort();
                    cpool[i] = v1;
                }
                case CONSTANT_INTERFACEMETHODREF, CONSTANT_FIELDREF, CONSTANT_METHODREF, CONSTANT_NAMEANDTYPE ->
                        cpool[i] = "#" + in.readUnsignedShort() + " #" + in.readUnsignedShort();
                case CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC ->
                        cpool[i] = in.readUnsignedShort() + "s #" + in.readUnsignedShort();
                case CONSTANT_METHODHANDLE -> cpool[i] = in.readUnsignedByte() + "b #" + in.readUnsignedShort();
                case CONSTANT_METHODTYPE -> cpool[i] = "#" + in.readUnsignedShort();
                default -> {
                    CPlen = i;
                    printCP();
                    out_println(toHex(btag, 1) + "; // invalid constant type: " + (int) btag + " for element " + i);
                    throw new ClassFormatError();
                }
            }
        }
    }

    private void printCP() {
        int length = CPlen;
        startArrayCmt(length, "Constant Pool");
        out_println("; // first element is empty");
        try {
            int size;
            for (int i = 1; i < length; i = i + size) {
                size = 1;
                byte btag = types[i];
                ConstType tg = tag(btag);
                int pos = cpe_pos[i];
                String tagstr;
                String valstr;
                int v1;
                long lv;
                if (tg != null) {
                    tagstr = tg.parseKey();
                } else {
                    throw new Error("Can't get a tg representing the type of Constant in the Constant Pool at: " + i);
                }
                switch (tg) {
                    case CONSTANT_UTF8 -> {
                        tagstr = "Utf8";
                        valstr = StringUtils.Utf8ToString((String) cpool[i], "\"");
                    }
                    case CONSTANT_FLOAT, CONSTANT_INTEGER -> {
                        v1 = (Integer) cpool[i];
                        valstr = toHex(v1, 4);
                    }
                    case CONSTANT_DOUBLE, CONSTANT_LONG -> {
                        lv = (Long) cpool[i];
                        valstr = toHex(lv, 8) + ";";
                        size = 2;
                    }
                    case CONSTANT_CLASS, CONSTANT_MODULE, CONSTANT_PACKAGE, CONSTANT_STRING -> {
                        v1 = (Integer) cpool[i];
                        valstr = "#" + v1;
                    }
                    case CONSTANT_INTERFACEMETHODREF, CONSTANT_FIELDREF,
                            CONSTANT_METHODREF, CONSTANT_NAMEANDTYPE,
                            CONSTANT_METHODHANDLE, CONSTANT_METHODTYPE,
                            CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC -> valstr = (String) cpool[i];
                    default -> throw new Error("invalid constant type: " + (int) btag);
                }
                out_print(tagstr + " " + valstr + "; // #" + i);
                if (environment.printDetailsFlag) {
                    out_println(" at " + toHex(pos));
                } else {
                    environment.println();
                }
            }
        } finally {
            out_end("}" + (environment.printDetailsFlag ? " // end of Constant Pool" : ""));
            environment.println();
        }
    }

    /**
     * CONSTANT_Module_info {
     *     u1 tag;              // == CONSTANT_MODULE(19)
     *     u2 name_index;
     * }
     *
     * @return Constant Pool module name by name_index
     */
    private String getModuleName() {
        int idx = 0;
        String name = "";
        for (int i = 1; i < types.length; i++) {
            if (types[i] == CONSTANT_MODULE.getTag()) {
                idx = i;
                break;
            }
        }
        if (idx != 0) {
            try {
                name = StringUtils.Utf8ToString((String) cpool[(int) cpool[idx]]);
            } catch (Throwable ignored) { /* ignored*/ }
        }
        return name;
    }

    private String getStringPos() {
        return " at " + toHex(arrayInputStream.getPos());
    }

    private String getCommentPosCond() {
        if (environment.printDetailsFlag) {
            return " // " + getStringPos();
        } else {
            return "";
        }
    }

    private void decodeCPXAttr(DataInputStream in, int len, String attrname) throws IOException {
        decodeCPXAttrM(in, len, attrname, 1);
    }

    private void decodeCPXAttrM(DataInputStream in, int len, String attrName, int expectedIndices) throws IOException {
        if (len != expectedIndices * 2) {
            out_println("// == invalid length of " + attrName + " attr: " + len + " (should be " + (expectedIndices * 2) + ") ==");
            printBytes(in, len, false);
        } else {
            StringBuilder outputString = new StringBuilder();
            for (int k = 1; k <= expectedIndices; k++) {
                outputString.append("#").append(in.readUnsignedShort()).append("; ");
                if (k % 16 == 0) {
                    out_println(outputString.toString().replaceAll("\\s+$", ""));
                    outputString = new StringBuilder();
                }
            }
            if (outputString.length() > 0) {
                out_println(outputString.toString().replaceAll("\\s+$", ""));
            }
        }
    }

    private String getStackMap(DataInputStream in, int elementsNum) throws IOException {
        int num;
        StringBuilder sb = new StringBuilder(20);
        if (elementsNum > 0) {
            num = elementsNum;
        } else {
            num = in.readUnsignedShort();
        }
        sb.append(startArray(num)).append(elementsNum > 0 ? "z" : "").append('{');
        try {
            for (int k = 0; k < num; k++) {
                int maptype = in.readUnsignedByte();
                StackMap.VerificationType verificationType = StackMap.getVerificationType(maptype,
                        Optional.of((s) -> environment.printErrorLn(s)));
                String maptypeImg;
                if (environment.printDetailsFlag) {
                    maptypeImg = maptype + "b";
                } else {
                    try {
                        maptypeImg = verificationType.parseKey();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        maptypeImg = "/* BAD TYPE: */ " + maptype + "b";
                    }
                }
                switch (verificationType) {
                    case ITEM_Object, ITEM_NewObject -> maptypeImg = maptypeImg + "," + in.readUnsignedShort();
                    case ITEM_UNKNOWN -> maptypeImg = maptype + "b";
                    default -> {
                    }
                }
                sb.append(maptypeImg);
                if (k < num - 1) {
                    sb.append("; ");
                }
            }
        } finally {
            sb.append('}');
        }
        return sb.toString();
    }

    /**
     * Processes 4.7.20 The RuntimeVisibleTypeAnnotations Attribute, 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
     * <code>type_annotation</code> structure.
     */
    private void decodeTargetTypeAndRefInfo(DataInputStream in) throws IOException {
        int tt = in.readUnsignedByte(); // [4.7.20] annotations[], type_annotation { u1 target_type; ...}
        ETargetType targetType = ETargetType.getTargetType(tt);
        if (targetType == null) {
            throw new Error("Type annotation: invalid target_type(u1) " + tt);
        }
        ETargetInfo targetInfo = targetType.targetInfo();
        out_println(toHex(tt, 1) + ";  //  target_type: " + targetType.parseKey());
        switch (targetInfo) {
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
                try {
                    for (int i = 0; i < lv_num; i++) {
                        out_println(in.readUnsignedShort() + " " + in.readUnsignedShort()
                                + " " + in.readUnsignedShort() + ";" + getCommentPosCond());
                    }
                } finally {
                    out_end("}");
                }
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
        startArrayCmtB(path_length, "type_paths");
        try {
            for (int i = 0; i < path_length; i++) {
                // print the type_path elements
                out_println("{ " + toHex(in.readUnsignedByte(), 1)  // { u1 type_path_kind;
                        + "; " + toHex(in.readUnsignedByte(), 1)    //   u1 type_argument_index; }
                        + "; } // type_path[" + i + "]");           // path[i]
            }
        } finally {
            out_end("}");
        }
    }

    private void decodeElementValue(DataInputStream in, ToolOutput out) throws IOException {
        out_begin("{  //  element_value");
        try {
            char tg = (char) in.readByte();
            AnnotationElementType tag = getAnnotationElementType(tg);
            if (tag != AE_UNKNOWN) {
                out_println("'" + tg + "';");
            }
            switch (tag) {
                case AE_BYTE, AE_CHAR, AE_DOUBLE, AE_FLOAT,
                        AE_INT, AE_LONG, AE_SHORT, AE_BOOLEAN,
                        AE_STRING -> decodeCPXAttr(in, 2, "const_value_index");
                case AE_ENUM -> {
                    out_begin("{  //  enum_const_value");
                    decodeCPXAttr(in, 2, "type_name_index");
                    decodeCPXAttr(in, 2, "const_name_index");
                    out_end("}  //  enum_const_value");
                }
                case AE_CLASS -> decodeCPXAttr(in, 2, "class_info_index");
                case AE_ANNOTATION -> decodeAnnotation(in, out);
                case AE_ARRAY -> {
                    int ev_num = in.readUnsignedShort();
                    startArrayCmt(ev_num, "array_value");
                    try {
                        for (int i = 0; i < ev_num; i++) {
                            decodeElementValue(in, out);
                            if (i < ev_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}  //  array_value");
                    }
                }
                case AE_UNKNOWN -> {
                    String msg = "invalid element_value" + (isPrintableChar(tg) ? " tag type : " + tg : "??");
                    out_println(toHex(tg, 1) + "; // " + msg);
                    throw new ClassFormatError(msg);
                }
            }
        } finally {
            out_end("}  //  element_value");
        }
    }

    public boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    private void decodeAnnotation(DataInputStream in, ToolOutput out) throws IOException {
        out_begin("{  //  annotation");
        try {
            decodeCPXAttr(in, 2, "field descriptor");
            int evp_num = in.readUnsignedShort();
            decodeElementValuePairs(evp_num, in, out);
        } finally {
            out_end("}  //  annotation");
        }
    }

    private void decodeElementValuePairs(int count, DataInputStream in, ToolOutput out) throws IOException {
        startArrayCmt(count, "element_value_pairs");
        try {
            for (int i = 0; i < count; i++) {
                out_begin("{  //  element value pair");
                try {
                    decodeCPXAttr(in, 2, "name of the annotation type element");
                    decodeElementValue(in, out);
                } finally {
                    out_end("}  //  element value pair");
                    if (i < count - 1) {
                        out_println(";");
                    }
                }
            }
        } finally {
            out_end("}  //  element_value_pairs");
        }
    }

    /**
     * component_info {     JEP 359 Record(Preview): class file 58.65535
     * u2               name_index;
     * u2               descriptor_index;
     * u2               attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     * <p>
     * or
     * field_info {
     * u2             access_flags;
     * u2             name_index;
     * u2             descriptor_index;
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     * or
     * method_info {
     * u2             access_flags;
     * u2             name_index;
     * u2             descriptor_index;
     * u2             attributes_count;
     * attribute_info attributes[attributes_count];
     * }
     */
    private void decodeInfo(DataInputStream in, ToolOutput out, String elementName, boolean hasAccessFlag) throws IOException {
        out_begin("{  // " + elementName + (environment.printDetailsFlag ? getStringPos() : ""));
        try {
            if (hasAccessFlag) {
                //  u2 access_flags;
                out_println(toHex(in.readShort(), 2) + "; // access");
            }
            // u2 name_index
            printUtf8InfoIndex(in.readUnsignedShort(), "name_index");
            // u2 descriptor_index
            printUtf8InfoIndex(in.readUnsignedShort(), "descriptor_index");
            // u2 attributes_count;
            // attribute_info attributes[attributes_count]
            decodeAttrs(in, out);
        } finally {
            out_end("}");
        }
    }

    private void decodeTypeAnnotation(DataInputStream in, ToolOutput out) throws IOException {
        out_begin("{  //  type_annotation");
        try {
            decodeTargetTypeAndRefInfo(in);
            decodeCPXAttr(in, 2, "field descriptor");
            int evp_num = in.readUnsignedShort();
            decodeElementValuePairs(evp_num, in, out);
        } finally {
            out_end("}  //  type_annotation");
        }
    }

    private void decodeBootstrapMethod(DataInputStream in) throws IOException {
        out_begin("{  //  bootstrap_method");
        try {
            out_println("#" + in.readUnsignedShort() + "; // bootstrap_method_ref");
            int bm_args_cnt = in.readUnsignedShort();
            startArrayCmt(bm_args_cnt, "bootstrap_arguments");
            try {
                for (int i = 0; i < bm_args_cnt; i++) {
                    out_println("#" + in.readUnsignedShort() + ";" + getCommentPosCond());
                }
            } finally {
                out_end("}  //  bootstrap_arguments");
            }
        } finally {
            out_end("}  //  bootstrap_method");
        }
    }

    private void decodeAttr(DataInputStream in, ToolOutput out) throws IOException {
        // Read one attribute
        String posComment = getStringPos();
        int name_cpx = 0, btag, len;

        String AttrName = "";
        try {
            name_cpx = in.readUnsignedShort();
            btag = types[name_cpx];
            ConstType tag = tag(btag);

            if (tag == ConstType.CONSTANT_UTF8) {
                AttrName = (String) cpool[name_cpx];
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            environment.print(getOutString(""));
            environment.println("// == %s ==", sharedI18n.getString("main.error.wrong.bytes"));
        }
        EAttribute tg = EAttribute.get(AttrName);
        String endingComment = AttrName.isEmpty() ? "#" + name_cpx : AttrName;
        len = in.readInt();
        arrayInputStream.enter(len);
        try {
            if (environment.printDetailsFlag) {
                out_begin("Attr(#" + name_cpx + ", " + len + ") { // " + endingComment + posComment);
            } else {
                out_begin("Attr(#" + name_cpx + ") { // " + endingComment);
            }

            switch (tg) {
                case ATT_Code -> {
                    out_println(in.readUnsignedShort() + "; // max_stack");
                    out_println(in.readUnsignedShort() + "; // max_locals");
                    int code_len = in.readInt();
                    out_begin("Bytes" + startArray(code_len) + "{");
                    try {
                        printBytes(in, code_len, false);
                    } finally {
                        out_end("}");
                    }
                    int trap_num = in.readUnsignedShort();
                    startArrayCmt(trap_num, "Traps");
                    try {
                        for (int i = 0; i < trap_num; i++) {
                            out_println(in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("} // end of Traps");
                    }
                    // Read the attributes
                    decodeAttrs(in, out);
                }
                case ATT_Exceptions -> {
                    int count = in.readUnsignedShort();
                    startArrayCmt(count, AttrName);
                    try {
                        for (int i = 0; i < count; i++) {
                            out_println("#" + in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_LineNumberTable -> {
                    int ll_num = in.readUnsignedShort();
                    startArrayCmt(ll_num, "line_number_table");
                    try {
                        for (int i = 0; i < ll_num; i++) {
                            out_println(in.readUnsignedShort() + "  " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_LocalVariableTable, ATT_LocalVariableTypeTable -> {
                    int lvt_num = in.readUnsignedShort();
                    startArrayCmt(lvt_num, AttrName);
                    try {
                        for (int i = 0; i < lvt_num; i++) {
                            out_println(in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_InnerClasses -> {
                    int ic_num = in.readUnsignedShort();
                    startArrayCmt(ic_num, "classes");
                    try {
                        for (int i = 0; i < ic_num; i++) {
                            out_println("#" + in.readUnsignedShort() + " #" +
                                    in.readUnsignedShort() + " #" +
                                    in.readUnsignedShort() + " " +
                                    in.readUnsignedShort() + ";" + getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_StackMap -> {
                    int e_num = in.readUnsignedShort();
                    startArrayCmt(e_num, "");
                    try {
                        for (int k = 0; k < e_num; k++) {
                            int start_pc = in.readUnsignedShort();
                            environment.println(format("%d, %s, %s;", start_pc,
                                    getStackMap(in, 0), getStackMap(in, 0)));
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_StackMapTable -> {
                    int et_num = in.readUnsignedShort();
                    startArrayCmt(et_num, "");
                    try {
                        for (int k = 0; k < et_num; k++) {
                            int frame_type = in.readUnsignedByte();
                            StackMap.FrameType ftype = StackMap.stackMapFrameType(frame_type);
                            switch (ftype) {
                                case SAME_FRAME -> {
                                    // verificationType is same_frame;
                                    out_println("" + frame_type + "b; // same_frame");
                                }
                                case SAME_LOCALS_1_STACK_ITEM_FRAME -> {
                                    // verificationType is same_locals_1_stack_item_frame
                                    // read additional single stack element
                                    out_println(format("%db, %s; // same_locals_1_stack_item_frame",
                                            frame_type, getStackMap(in, 1)));
                                }
                                case SAME_LOCALS_1_STACK_ITEM_EXTENDED_FRAME -> {
                                    // verificationType is same_locals_1_stack_item_frame_extended
                                    // read additional single stack element
                                    int noffset = in.readUnsignedShort();
                                    out_println(format("%db, %d, %s; // same_locals_1_stack_item_frame_extended",
                                            frame_type, noffset, getStackMap(in, 1)));
                                }
                                case CHOP_1_FRAME, CHOP_2_FRAME, CHOP_3_FRAME -> {
                                    // verificationType is chop_frame
                                    int coffset = in.readUnsignedShort();
                                    out_println(format("%db, %d; // chop_frame %d",
                                            frame_type, coffset, 251 - frame_type));
                                }
                                case SAME_FRAME_EX -> {
                                    // verificationType is same_frame_extended;
                                    int xoffset = in.readUnsignedShort();
                                    out_println(format("%db, %d; // same_frame_extended",
                                            frame_type, xoffset));
                                }
                                case APPEND_FRAME -> {
                                    // verificationType is append_frame
                                    // read additional locals
                                    int aoffset = in.readUnsignedShort();
                                    out_println(format("%db, %d, %s; // append_frame %d",
                                            frame_type, aoffset,
                                            getStackMap(in, frame_type - 251), frame_type - 251));
                                }
                                case FULL_FRAME -> {
                                    // verificationType is full_frame
                                    int foffset = in.readUnsignedShort();
                                    out_println(format("%db, %d, %s, %s; // full_frame", frame_type, foffset,
                                            getStackMap(in, 0), getStackMap(in, 0)));
                                }
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_EnclosingMethod -> decodeCPXAttrM(in, len, AttrName, 2);
                case ATT_AnnotationDefault -> decodeElementValue(in, out);
                case ATT_RuntimeInvisibleAnnotations, ATT_RuntimeVisibleAnnotations -> {
                    int an_num = in.readUnsignedShort();
                    startArrayCmt(an_num, "annotations");
                    try {
                        for (int i = 0; i < an_num; i++) {
                            decodeAnnotation(in, out);
                            if (i < an_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                // 4.7.20 The RuntimeVisibleTypeAnnotations Attribute
                // 4.7.21 The RuntimeInvisibleTypeAnnotations Attribute
                case ATT_RuntimeInvisibleTypeAnnotations, ATT_RuntimeVisibleTypeAnnotations -> {
                    int ant_num = in.readUnsignedShort();
                    startArrayCmt(ant_num, "annotations");
                    try {
                        for (int i = 0; i < ant_num; i++) {
                            decodeTypeAnnotation(in, out);
                            if (i < ant_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_RuntimeInvisibleParameterAnnotations, ATT_RuntimeVisibleParameterAnnotations -> {
                    int pm_num = in.readUnsignedByte();
                    startArrayCmtB(pm_num, "parameters");
                    try {
                        for (int k = 0; k < pm_num; k++) {
                            int anp_num = in.readUnsignedShort();
                            startArrayCmt(anp_num, "annotations");
                            try {
                                for (int i = 0; i < anp_num; i++) {
                                    decodeAnnotation(in, out);
                                    if (k < anp_num - 1) {
                                        out_println(";");
                                    }
                                }
                            } finally {
                                out_end("}");
                            }
                            if (k < pm_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_BootstrapMethods -> {
                    int bm_num = in.readUnsignedShort();
                    startArrayCmt(bm_num, "bootstrap_methods");
                    try {
                        for (int i = 0; i < bm_num; i++) {
                            decodeBootstrapMethod(in);
                            if (i < bm_num - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_Module -> decodeModule(in);
                case ATT_TargetPlatform -> decodeCPXAttrM(in, len, AttrName, 3);
                case ATT_ModulePackages -> {
                    int p_num = in.readUnsignedShort();
                    startArrayCmt(p_num, null);
                    try {
                        decodeCPXAttrM(in, len - 2, AttrName, p_num);
                    } finally {
                        out_end("}");
                    }
                }
                //  MethodParameters_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u1 parameters_count;
                //    {   u2 name_index;
                //        u2 access_flags;
                //    } parameters[parameters_count];
                //  }
                case ATT_MethodParameters -> {
                    int pcount = in.readUnsignedByte();
                    startArrayCmtB(pcount, AttrName);
                    try {
                        for (int i = 0; i < pcount; i++) {
                            out_println("#" + in.readUnsignedShort() + "  " +
                                    toHex(in.readUnsignedShort(), 2) + ";" +
                                    getCommentPosCond());
                        }
                    } finally {
                        out_end("}");
                    }
                }
                //  JEP 359 Record(Preview): class file 58.65535
                //  Record_attribute {
                //      u2 attribute_name_index;
                //      u4 attribute_length;
                //      u2 components_count;
                //      component_info components[components_count];
                //  }
                case ATT_Record -> {
                    int ncomps = in.readUnsignedShort();
                    startArrayCmt(ncomps, "components");
                    try {
                        for (int i = 0; i < ncomps; i++) {
                            decodeInfo(in, out, "component", false);
                            if (i < ncomps - 1) {
                                out_println(";");
                            }
                        }
                    } finally {
                        out_end("}");
                    }
                }
                case ATT_ConstantValue, ATT_Signature, ATT_SourceFile -> decodeCPXAttr(in, len, AttrName);

                //  JEP 181 (Nest-based Access Control): class file 55.0
                //  NestHost_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u2 host_class_index;
                //  }
                case ATT_NestHost -> decodeTypes(in, 1);

                //  JEP 181 (Nest-based Access Control): class file 55.0
                //  NestMembers_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u2 number_of_classes;
                //    u2 classes[number_of_classes];
                //  }
                //  JEP 360 (Sealed types): class file 59.65535
                //  PermittedSubclasses_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u2 number_of_classes;
                //    u2 classes[number_of_classes];
                //  }
                // Valhalla
                case ATT_NestMembers, ATT_PermittedSubclasses, ATT_Preload -> {
                    int nsubtypes = in.readUnsignedShort();
                    startArrayCmt(nsubtypes, "classes");
                    try {
                        decodeTypes(in, nsubtypes);
                    } finally {
                        out_end("}");
                    }
                }
                // SourceDebugExtension_attribute {
                //    u2 attribute_name_index;
                //    u4 attribute_length;
                //    u1 debug_extension[attribute_length];
                // }
                case ATT_SourceDebugExtension -> {
                    printUtf8String(in, len);
                    if (AttrName == null) {
                        endingComment = "Attr(#" + name_cpx + ")";
                    }
                }
                default -> {
                    printBytes(in, len, true);
                    if (AttrName == null) {
                        endingComment = "Attr(#" + name_cpx + ")";
                    }
                }
            }

        } catch (EOFException e) {
            environment.println(getOutString("") + "// == The unexpected end of attribute array while parsing. ==");
        } finally {
            int rest = arrayInputStream.available();
            if (rest > 0) {
                environment.println(getOutString("") +
                        "// == The attribute array started at" + posComment + " has " + rest + " bytes more than expected. ==");
                printBytes(in, rest, true);
            }
            out_end("} // end of " + endingComment);
            arrayInputStream.leave();
        }
    }

    private void decodeModuleStatement(String statementName, DataInputStream in) throws IOException {
        int index, nFlags;
        String sComment;
        // u2 {exports|opens}_count
        int count = in.readUnsignedShort();
        startArrayCmt(count, statementName);
        try {
            for (int i = 0; i < count; i++) {
                // u2 {exports|opens}_index; u2 {exports|opens}_flags
                index = in.readUnsignedShort();
                nFlags = in.readUnsignedShort();
                sComment = environment.printDetailsFlag ? format(" // [ %s ]", EModifier.asNames(nFlags, MODULE_DIRECTIVES)) : "";
                out_println(format("#%d %s%s", index, toHex(nFlags, 2), sComment));
                int exports_to_count = in.readUnsignedShort();
                startArrayCmt(exports_to_count, null);
                try {
                    for (int j = 0; j < exports_to_count; j++) {
                        out_println("#" + in.readUnsignedShort() + ";");
                    }
                } finally {
                    out_end("};");
                }
            }
        } finally {
            out_end("} // of " + statementName);
            environment.println();
        }
    }

    private void decodeModule(DataInputStream in) throws IOException {
        int nFlags;
        String sComment;
        //u2 module_name_index
        int index = in.readUnsignedShort();
        entityName = (String) cpool[(Integer) cpool[index]];
        out_print("#" + index + "; // ");
        if (environment.printDetailsFlag) {
            environment.println(format("%-16s", "name_index") + " : " + entityName);
        } else {
            environment.println("name_index");
        }

        // u2 module_flags
        int moduleFlags = in.readUnsignedShort();
        out_println(format("%s; //flags%s",
                toHex(moduleFlags, 2), environment.printDetailsFlag ? EModifier.asNames(moduleFlags, MODULE_DIRECTIVES) + " " : ""));
        environment.println();

        //u2 module_version
        int versionIndex = in.readUnsignedShort();
        out_println("#" + versionIndex + "; // version");

        // u2 requires_count
        int count = in.readUnsignedShort();
        startArrayCmt(count, "requires");
        try {
            for (int i = 0; i < count; i++) {
                // u2 requires_index; u2 requires_flags; u2 requires_version_index
                index = in.readUnsignedShort();
                nFlags = in.readUnsignedShort();
                versionIndex = in.readUnsignedShort();
                sComment = environment.printDetailsFlag ? format(" // %s", EModifier.asNames(nFlags, MODULE_DIRECTIVES)) : "";
                out_println(format("#%d %s #%d;%s", index, toHex(nFlags, 2), versionIndex, sComment));
            }
        } finally {
            out_end("} // end of requires");
            environment.println();
        }

        decodeModuleStatement("exports", in);

        decodeModuleStatement("opens", in);
        // u2 uses_count
        count = in.readUnsignedShort();
        startArrayCmt(count, "uses");
        try {
            for (int i = 0; i < count; i++) {
                // u2 uses_index
                out_println("#" + in.readUnsignedShort() + ";");
            }
        } finally {
            out_end("} // end of uses");
            environment.println();
        }
        count = in.readUnsignedShort(); // u2 provides_count
        startArrayCmt(count, "provides");
        try {
            for (int i = 0; i < count; i++) {
                // u2 provides_index
                out_println("#" + in.readUnsignedShort());
                int provides_with_count = in.readUnsignedShort();
                // u2 provides_with_count
                startArrayCmt(provides_with_count, null);
                try {
                    for (int j = 0; j < provides_with_count; j++) {
                        // u2 provides_with_index;
                        out_println("#" + in.readUnsignedShort() + ";");
                    }
                } finally {
                    out_end("};");
                }
            }
        } finally {
            out_end("} // end of provides");
            environment.println();
        }
    }

    private void decodeAttrs(DataInputStream in, ToolOutput out) throws IOException {
        // Read the attributes
        int attr_num = in.readUnsignedShort();
        startArrayCmt(attr_num, "Attributes");
        try {
            for (int i = 0; i < attr_num; i++) {
                decodeAttr(in, out);
                if (i + 1 < attr_num) {
                    out_println(";");
                }
            }
        } finally {
            out_end("} // end of Attributes");
        }
    }

    private void decodeMembers(DataInputStream in, ToolOutput out, String groupName, String elementName) throws IOException {
        int count = in.readUnsignedShort();
        environment.traceln(groupName + "=" + count);
        startArrayCmt(count, groupName);
        try {
            for (int i = 0; i < count; i++) {
                decodeInfo(in, out, elementName, true);
                if (i + 1 < count) {
                    out_println(";");
                }
            }
        } finally {
            out_end("} // end of " + groupName);
            environment.println();
        }
    }

    void decodeClass() throws IOException {
        // Read the header
        try {
            int magic = inputStream.readInt();
            int min_version = inputStream.readUnsignedShort();
            int version = inputStream.readUnsignedShort();

            // Read the constant pool
            readCP(inputStream);
            short access = inputStream.readShort(); // don't care about sign
            int this_cpx = inputStream.readUnsignedShort();

            try {
                entityName = (String) cpool[(Integer) cpool[this_cpx]];
                environment.getToolOutput().startClass(entityName, Optional.of(".jcod"), environment);
                if (entityName.equals("module-info")) {
                    entityType = "module";
                    entityName = getModuleName();
                } else {
                    entityType = "class";
                }
                if (!entityName.isEmpty() && (JcodTokens.keyword_token_ident(entityName) != IDENT || JcodTokens.constValue(entityName) != -1)) {
                    // JCod can't parse a entityName matching a keyword or a constant value,
                    // then use the filename instead:
                    out_begin(format("file \"%s.class\" {", entityName));
                } else {
                    out_begin(format("%s %s {", entityType, entityName));
                }
            } catch (Exception e) {
                entityName = environment.getInputFile().getFileName();
                environment.println("// " + e.getMessage() + " while accessing entityName");
                out_begin(format("%s %s { // source file name", entityType, entityName));
            }

            if (magic != JAVA_MAGIC) {
                out_println(toHex(magic, 4) + "; // wrong magic: 0x" + Integer.toString(JAVA_MAGIC, 16) + " expected");
            } else {
                out_println(toHex(magic, 4) + ";");

            }
            out_println(min_version + "; // minor version");
            out_println(version + "; // version");

            // Print the constant pool
            printCP();
            out_println(toHex(access, 2) + "; // access" +
                    (environment.printDetailsFlag ? " [ " + EModifier.asNames(access, ClassFileContext.CLASS) + " ]" : ""));
            out_println("#" + this_cpx + "; // this_cpx");
            int super_cpx = inputStream.readUnsignedShort();
            out_println("#" + super_cpx + "; // super_cpx");
            environment.traceln("jdec.trace.access_thisCpx_superCpx", access, this_cpx, super_cpx);
            environment.println();

            // Read the interfaces
            int numinterfaces = inputStream.readUnsignedShort();
            environment.traceln("jdec.trace.numinterfaces", numinterfaces);
            startArrayCmt(numinterfaces, "Interfaces");
            try {
                decodeTypes(inputStream, numinterfaces);
            } finally {
                out_end("} // end of Interfaces");
                environment.println();
            }
            // Read the fields
            decodeMembers(inputStream, environment.getToolOutput(), "Fields", "field");

            // Read the methods
            decodeMembers(inputStream, environment.getToolOutput(), "Methods", "method");

            // Read the attributes
            decodeAttrs(inputStream, environment.getToolOutput());
        } catch (EOFException ignored) {
        } catch (ClassFormatError err) {
            String msg = err.getMessage();
            environment.println("//------- ClassFormatError" +
                    (msg == null || msg.isEmpty() ? "" : ": " + msg));
            printRestOfBytes();
        } finally {
            if (environment.printDetailsFlag) {
                out_end(format("} // end of %s %s", entityType, entityName));
            } else {
                out_end("}");
            }
            environment.getToolOutput().finishClass(entityName);
        }
    } // end decodeClass()

    private void decodeTypes(DataInputStream in, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            int type_cpx = in.readUnsignedShort();
            environment.traceln("jdec.trace.type", i, type_cpx);
            String s = "#" + type_cpx + ";";
            if (environment.printDetailsFlag) {
                String name = (String) cpool[(int) cpool[type_cpx]];
                out_println(s + " // " + name + getStringPos());
            } else {
                environment.println(s);
            }
        }
    }

    private String formatComments(String s, int shift) {
        String[] pair = s.split("//");
        if (pair.length != 2) {
            return s;
        }
        pair[0] = pair[0].trim();
        pair[1] = pair[1].trim();
        return pair[0] + repeat(" ", COMMENT_OFFSET - pair[0].length() - shift * INDENT_LENGTH) + " // " + pair[1];
    }
}// end class ClassData
