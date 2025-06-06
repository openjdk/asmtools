/*
 * Copyright (c) 1996, 2024, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.asmutils.HexUtils;
import org.openjdk.asmtools.asmutils.Range;
import org.openjdk.asmtools.asmutils.StringUtils;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.CONSTANT_POOL;

/**
 * Class representing the Constant Pool
 */
public class ConstantPool extends Indenter {

    public static final Function<Integer, String> funcInvalidCPIndex =
            index -> "??? Invalid constant_pool reference #%d".formatted(index);

    private static final Hashtable<Byte, TAG> tagHash = new Hashtable<>();
    private static final Hashtable<Byte, SUBTAG> subTagHash = new Hashtable<>();

    // Class initializer Code
    static {
        // Make sure all the tags get initialized before being used.
        tagHash.put(TAG.CONSTANT_UTF8.value(), TAG.CONSTANT_UTF8);
        // Obsolete: tagHash.put(TAG.CONSTANT_UNICODE.value(), TAG.CONSTANT_UNICODE);
        tagHash.put(TAG.CONSTANT_INTEGER.value(), TAG.CONSTANT_INTEGER);
        tagHash.put(TAG.CONSTANT_FLOAT.value(), TAG.CONSTANT_FLOAT);
        tagHash.put(TAG.CONSTANT_LONG.value(), TAG.CONSTANT_LONG);
        tagHash.put(TAG.CONSTANT_DOUBLE.value(), TAG.CONSTANT_DOUBLE);
        tagHash.put(TAG.CONSTANT_CLASS.value(), TAG.CONSTANT_CLASS);
        tagHash.put(TAG.CONSTANT_STRING.value(), TAG.CONSTANT_STRING);
        tagHash.put(TAG.CONSTANT_FIELD.value(), TAG.CONSTANT_FIELD);
        tagHash.put(TAG.CONSTANT_METHOD.value(), TAG.CONSTANT_METHOD);
        tagHash.put(TAG.CONSTANT_INTERFACEMETHOD.value(), TAG.CONSTANT_INTERFACEMETHOD);
        tagHash.put(TAG.CONSTANT_NAMEANDTYPE.value(), TAG.CONSTANT_NAMEANDTYPE);
        tagHash.put(TAG.CONSTANT_METHODHANDLE.value(), TAG.CONSTANT_METHODHANDLE);
        tagHash.put(TAG.CONSTANT_METHODTYPE.value(), TAG.CONSTANT_METHODTYPE);
        tagHash.put(TAG.CONSTANT_DYNAMIC.value(), TAG.CONSTANT_DYNAMIC);
        tagHash.put(TAG.CONSTANT_INVOKEDYNAMIC.value(), TAG.CONSTANT_INVOKEDYNAMIC);
        tagHash.put(TAG.CONSTANT_MODULE.value(), TAG.CONSTANT_MODULE);
        tagHash.put(TAG.CONSTANT_PACKAGE.value(), TAG.CONSTANT_PACKAGE);

        subTagHash.put(SUBTAG.REF_GETFIELD.value(), SUBTAG.REF_GETFIELD);
        subTagHash.put(SUBTAG.REF_GETSTATIC.value(), SUBTAG.REF_GETSTATIC);
        subTagHash.put(SUBTAG.REF_PUTFIELD.value(), SUBTAG.REF_PUTFIELD);
        subTagHash.put(SUBTAG.REF_PUTSTATIC.value(), SUBTAG.REF_PUTSTATIC);
        subTagHash.put(SUBTAG.REF_INVOKEVIRTUAL.value(), SUBTAG.REF_INVOKEVIRTUAL);
        subTagHash.put(SUBTAG.REF_INVOKESTATIC.value(), SUBTAG.REF_INVOKESTATIC);
        subTagHash.put(SUBTAG.REF_INVOKESPECIAL.value(), SUBTAG.REF_INVOKESPECIAL);
        subTagHash.put(SUBTAG.REF_NEWINVOKESPECIAL.value(), SUBTAG.REF_NEWINVOKESPECIAL);
        subTagHash.put(SUBTAG.REF_INVOKEINTERFACE.value(), SUBTAG.REF_INVOKEINTERFACE);
    }

    /**
     * Reference to the class data
     */
    private final ClassData classData;
    private Environment environment;

    /**
     * The actual pool of Constants
     */
    private ArrayList<Constant> pool;
    private Range<Integer> range;

    private boolean printTAG = false;

    /* ConstantPool Constructors */
    public ConstantPool(ClassData cd) {
        this(cd, 10);
    }

    public ConstantPool(ClassData classData, int size) {
        super(classData.toolOutput);
        this.classData = classData;
        this.environment = classData.environment;
        tableToken = CONSTANT_POOL;
        pool = new ArrayList<>(size);
    }

    public void setPrintTAG(boolean value) {
        this.printTAG = value;
    }

    public String getPrintedTAG(TAG tag) {
        return (this.printTAG) ? tag.tagName + " " : "";
    }

    public int size() {
        return pool.size();
    }

    public boolean inRange(int value) {
        if (range == null) {
            range = new Range<>(1, pool.size() - 1);
        }
        return range.in(value);
    }

    /**
     * decodes a ConstantPool and it's constants from a data stream.
     */
    ConstantPool read(DataInputStream in) throws IOException {
        // constant_pool_count
        //The value of the constant_pool_count item is equal to the number of entries in the constant_pool table plus one.
        int constant_pool_count = in.readUnsignedShort();
        int tagSize;
        pool = new ArrayList<>(constant_pool_count);
        pool.add(0, null);
        environment.traceln("constant_pool_count = " + constant_pool_count);
        for (int i = 1; i < constant_pool_count; i += tagSize) {
            byte tagByte = in.readByte();
            TAG tag = tagHash.get(tagByte);
            if (tag == null) {
                throw new ClassFormatError(
                        format("Error while reading constant pool for %s: unexpected tag at #%d: %d",
                                environment.getToolInput(), i, tagByte));
            }
            tagSize = tag.size();
            environment.traceln("\tCP entry #" + i + " tag[" + tagByte + "]\t=\t" + tag);
            switch (tag) {
                case CONSTANT_UTF8 -> pool.add(i, new CP_Str(tag, in.readUTF()));
                case CONSTANT_INTEGER -> pool.add(i, new CP_Int(tag, in.readInt()));
                case CONSTANT_LONG -> {
                    pool.add(i, new CP_Long(tag, in.readLong()));
                    // handle null entry to account for Longs taking up 2 CP slots
                    pool.add(null);
                }
                case CONSTANT_FLOAT -> pool.add(i, new CP_Float(tag, in.readFloat()));
                case CONSTANT_DOUBLE -> {
                    pool.add(i, new CP_Double(tag, in.readDouble()));
                    // handle null entry to account for Doubles taking up 2 CP slots
                    pool.add(null);
                }
                case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHODTYPE, CONSTANT_PACKAGE, CONSTANT_MODULE ->
                        pool.add(i, new CPX(tag, in.readUnsignedShort()));
                case CONSTANT_FIELD, CONSTANT_METHOD, CONSTANT_INTERFACEMETHOD, CONSTANT_NAMEANDTYPE, CONSTANT_DYNAMIC,
                     CONSTANT_INVOKEDYNAMIC ->
                        pool.add(i, new CPX2(tag, in.readUnsignedShort(), in.readUnsignedShort()));
                case CONSTANT_METHODHANDLE -> pool.add(i, new CPX2(tag, in.readUnsignedByte(), in.readUnsignedShort()));
                default -> throw new ClassFormatError("invalid constant type: " + (int) tagByte);
            }
        }
        return this;
    }

    /**
     * Public getter - Safely gets a Constant from the CP at a given index.
     */
    public Constant getConst(int cpx) {
        if (inRange(cpx)) {
            return pool.get(cpx);
        } else {
            return null;
        }
    }

    /**
     * Safely gets the string representation of a ConstantUTF8 from the CP at a given index.
     * <p>
     * Returns either the UTF8 string, or a default ConstantUTF8 built by CP index
     * with the function funcGetDefaultString like: index-> "#" + index
     */
    public String getString(int cpx, Function<Integer, String> funcGetDefaultString) {
        String str = funcGetDefaultString.apply(cpx);
        if (inRange(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_UTF8) {
                CP_Str cns1 = (CP_Str) cns;
                str = cns1.value;
            }
        }
        return str;
    }

    /**
     * Safely gets the string representation of a ConstantModule from the CP at a  given index.
     * <p>
     * Returns either the Java Module name, or a default class name built by CP index
     * with the function funcGetDefaultModuleName like: index-> "#" + index
     */
    public String getModuleName(int cpx, Function<Integer, String> funcGetDefaultModuleName) {
        String str = funcGetDefaultModuleName.apply(cpx);
        if (inRange(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_MODULE) {
                str = cns.stringVal();
            }
        }
        return str;
    }

    public String getModuleName(int cpx) {
        return getModuleName(cpx, index -> "#" + index);
    }

    /**
     * Public string val - Safely gets the string representation of a ConstantPackage from the CP at a given index.
     * <p>
     * Returns either the Java Package name, or a default class name built by CP index
     * with the function funcGetDefaultPackageName like: index-> "#" + index
     */
    public String getPackageName(int cpx, Function<Integer, String> funcGetDefaultPackageName) {
        String str = funcGetDefaultPackageName.apply(cpx);
        if (inRange(cpx)) {
            Constant cns = pool.get(cpx);
            if (cns != null && cns.tag == TAG.CONSTANT_PACKAGE) {
                str = cns.stringVal();
            }
        }
        return str;
    }

    public String getPackageName(int cpx) {
        return getPackageName(cpx, index -> "#" + index);
    }

    /**
     * Safely gets a Java name from a ConstantUTF8 from the CP at a given index.
     * <p>
     * Returns either null (if invalid), or the Java name value of the UTF8
     */
    public String getName(int cpx) {
        String str = getString(cpx, index -> null);
        if (str == null) {
            return "<invalid constant pool index:" + cpx + ">";
        }
        return Utils.javaName(str);
    }

    /**
     * Safely gets a Java class name from a ConstantClass from the CP at a given index.
     * <p>
     * Returns either the Java class name, or a CP index reference string.
     */
    public String getClassName(int cpx) {
        return getClassName(cpx, index -> "#" + index);
    }

    public String getMethodName(int cpx) {
        Constant<?> cns = pool.get(cpx);
        if (cns != null && cns instanceof CPX2 cpx2) {
            return cpx2.stringVal();
        } else {
            return cns.stringVal();
        }
    }

    /**
     * Gets a string representation of the constant_pool entry at the specified index,
     * pointing to a CONSTANT_NameAndType_info structure with a field descriptor.
     *
     * @param cpx the constant_pool index pointing to CONSTANT_NameAndType_info
     * @return a string presentation of the CONSTANT_NameAndType_info structure
     */
    public String getFieldNameTypeAsString(int cpx) {
        Constant<?> cns = pool.get(cpx);
        if (cns == null || (cns.tag != TAG.CONSTANT_NAMEANDTYPE) || referredBy(cpx, TAG.CONSTANT_METHOD,
                TAG.CONSTANT_INTERFACEMETHOD, TAG.CONSTANT_METHODHANDLE, TAG.CONSTANT_DYNAMIC, TAG.CONSTANT_INVOKEDYNAMIC)) {
            String msg = environment.getLogger().getResourceString("warn.field.nametype.required", cpx);
            return (printCPIndex) ? msg : "??? #%d %s".formatted(cpx, msg);
        }
        return cns.stringVal();
    }

    public boolean referredBy(int cpx, TAG... tags) {
        Optional<Constant> cns = pool.stream().filter(item -> item != null &&
                item.tag.belongsTo(tags)).filter(c -> ((CPX2) c).value2 == cpx).findFirst();
        return cns.isPresent();
    }

    /**
     * Safely gets a Java class name from a ConstantClass from the CP at a given index.
     * <p>
     * Returns either the Java class name, or a message string formatted by cpx: formatString.formatted(cpx)
     */
    public String getJavaClassName(int cpx, String formatString) {
        String str = getClassName(cpx, index -> null);
        return (str == null) ? formatString.formatted(cpx) : str.replace('/', '.');
    }

    /**
     * Safely gets a Java class name from a ConstantClass from the CP at a given index.
     * <p>
     * Returns either the Java class name, or a default class name built by CP index
     * with the function funcGetDefaultClassName like: index-> "#" + index
     */
    public String getClassName(int cpx, Function<Integer, String> funcGetDefaultClassName) {
        Constant cns = getConstantOfClassName(cpx);
        if (cns == null) {
            return funcGetDefaultClassName.apply(cpx);
        }
        return getClassName((CPX) cns);
    }

    public Constant getConstantOfClassName(int cpx) {
        if (!inRange(cpx)) {
            return null;
        }
        Constant cns = pool.get(cpx);
        if (cns == null || cns.tag != TAG.CONSTANT_CLASS) {
            return null;
        }
        return cns;
    }

    /**
     * Safely gets a Java class name from a ConstantClass from a CPX2 constant pool
     * object. (eg. Method/Field/Interface Ref)
     * <p>
     * Returns either the Java class name, or a CP index reference string.
     */
    public String getClassName(CPX2 classConst) {
        return _getClassName(classConst.value);
    }

    /**
     * Safely gets a Java class name from a ConstantClass from a CPX constant pool object.
     * (eg. Class Ref)
     * <p>
     * Returns either the Java class name, or a CP index reference string.
     */
    public String getClassName(CPX classConst) {
        return _getClassName(classConst.value);
    }

    /**
     * Helper for getting class name. It checks ConstantPool bounds, does name conversion.
     */
    private String _getClassName(int nameIndex) {
        String res = "#" + nameIndex;
        if (!inRange(nameIndex)) {
            return res;
        }
        Constant nameConst = pool.get(nameIndex);
        if (nameConst == null || nameConst.tag != TAG.CONSTANT_UTF8) {
            return res;
        }
        CP_Str name = (CP_Str) nameConst;

        String classname = name.value;

        if (Utils.isClassArrayDescriptor(classname)) {
            classname = "\"%s\"".formatted(classname);
        }
        return classname;
    }

    /**
     * Shortens a class name (if the class is in the given package).
     * Works with a string-encoded classname.
     *
     * @param className   fully qualified name of the class
     * @param packageName the package
     */
    public String getShortClassName(String className, String packageName) {
        final int idx = className.lastIndexOf('/');
        if (idx == packageName.length() && className.startsWith(packageName)) {
            return className.substring(idx + 1);
        }
        return className;
    }

    /**
     * Shortens a class name (if the class is in the given package).
     * Works with a CP index to a ConstantClass.
     *
     * @param cpx         the Constant Pool index to a CONSTANT_Class_info
     * @param packageName the package
     */
    public String getShortClassName(int cpx, String packageName) {
        String name = Utils.javaName(getClassName(cpx));
        return getShortClassName(name, packageName);
    }

    /**
     * Pulls the class name out of a string (at the CP index). (drops any array
     * descriptors, and the class descriptors ("L" and ";")
     */
    public String decodeClassDescriptor(int cpx) {
        // enum type is encoded as a descriptor
        // need to remove '"'s and L (class descriptor)

        // TODO: might have to count '['s at the beginning for Arrays
        String rawEnumName = getName(cpx);
        int len = rawEnumName.length();
        int begin = (rawEnumName.startsWith("\"L")) ? 2 : 0;
        int end = (begin > 0) ? len - 2 : len;
        return rawEnumName.substring(begin, end);
    }

    /**
     * Getter that safely gets the string descriptor of a subtag
     */
    private String subtagToString(int subtag) {
        SUBTAG st = subTagHash.get((byte) subtag);
        if (st == null) {
            return "BOGUS_SUBTAG:" + subtag;
        }
        return st.tagName;
    }

    /**
     * Safely gets the string value of any Constant at any CP index.
     */
    public String StringValue(int cpx) {
        if (!inRange(cpx)) {
            return "<invalid constant pool index: %d>".formatted(cpx);
        }
        Constant cnst = pool.get(cpx);
        if (cnst == null) {
            return "<null>";
        }
        return cnst.stringVal();
    }

    /**
     * Safely gets the string value of any Constant at any CP index. This string is either
     * a Constant's String value or a CP index reference string. The Constant string has
     * a tag descriptor in the beginning.
     */
    public String ConstantStrValue(int cpx) {
        if (cpx == 0 || !inRange(cpx)) {
            return "#" + cpx;
        }
        Constant cns = pool.get(cpx);
        if (cns == null) {
            return "#" + cpx;
        }
        if (cns instanceof CPX2 cns2) {
            if (cns2.value == classData.this_cpx && cns2.refersClassMember()) {
                cpx = cns2.value2;
            }
        }
        return cns.tag.tagName + " " + StringValue(cpx);
    }

    @Override
    public boolean isTableOutput() {
        return tableFormat;
    }

    @Override
    protected void jasmPrint() {
        int size;
        if (extraDetailedOutput && !skipComments) {
            printIndentLn("// %s:".formatted(tableToken.printKey()));
        }
        for (int idx = 1; idx < pool.size(); idx += size) {
            Constant cns = pool.get(idx);
            printIndent(this.CPXPlaceHolder, idx);
            if (cns == null) {
                size = 0;
                printIndentLn("null;");
            } else {
                size = cns.size();
                cns.setCommentPadding(getCommentPadding());
                // simple printing
                cns.jasmPrint(toolOutput, this.tagPadding);

            }
        }
        printIndentLn();
    }

    @Override
    protected void tablePrint() {
        int size;
        if (extraDetailedOutput && !skipComments) {
            printIndentLn("// %s:".formatted(tableToken.printKey()));
        }
        for (int idx = 1; idx < pool.size(); idx += size) {
            Constant cns = pool.get(idx);
            printIndent(this.CPXPlaceHolder, idx);
            if (cns == null) {
                size = 0;
                printIndentLn("null;");
            } else {
                size = cns.size();
                cns.setCommentPadding(getCommentPadding());
                // extended printing
                cns.tablePrint(toolOutput, this.tagPadding);
            }
        }
        printIndentLn();
    }

    private String CPXPlaceHolder = "";
    private int CPXPlaceHolderLength = 0;
    private int CPXLength = 0;
    private int tagPadding = 0;

    /**
     * Initialize printing data once the pool was read.
     * Prepare offsets, paddings, etc. needed for printing
     */
    protected ConstantPool InitializePrintData() {
        int maxTagNameLength = pool.stream().mapToInt(elem -> (elem == null) ? 4 :
                elem.tag.getPrintWidth(tableFormat)).max().orElse(10);
        this.CPXLength = pool.size() > 100 ? 4 : 3;
        // similar to javap format like this:
        // Constant pool:
        //    #1 = Methodref          #2.#3         // java/lang/Object."<init>":()V
        //    #2 = Class              #4            // java/lang/Object
        // if (javap-like format) ::
        // int maxTagAliasLength = pool.stream().mapToInt(elem -> (elem == null) ? 4 :
        //      elem.tag.tagAlias().length()).max().orElse(10);
        //      this.tagPadding = maxTagAliasLength + 1;
        //      this.CPXPlaceHolder = "%" + CPXLength + "s = ".formatted("#%d");
        this.tagPadding = maxTagNameLength + 1;
        this.CPXPlaceHolder = "const #%-" + CPXLength + "d = ";
        this.CPXPlaceHolderLength = this.CPXPlaceHolder.formatted(pool.size()).length();
        // --const #XX = --TagPadding-|--commentPadding---//
        // --const #25 = class--------|#34;---------------// TesterInfo$Priority
        // --#XX       =
        // IIhhhhhhhhhhhhTTTTTTTTTTTTTCCCCCCCCCCCCCCCCCCCC
        // 123456789012345678901234567890123 4567890123456789012
        //     II              + hhhhhhhhhhhhhhhhhhhhhhhh  + TTTTTTTTTT + CCCCCCCCCCCCCCCCCCC
        this.commentOffset = getIndentSize() + this.CPXPlaceHolderLength + tagPadding + this.getCommentPadding();
        return this;
    }

    public int getCommentPadding() {
        return max(COMMENT_PADDING, tagPadding);
    }

    public List<IOException> getIssues() {
        return this.pool.stream().filter(Objects::nonNull).
                flatMap(constant -> constant.getIssues().stream().filter(Objects::nonNull)).
                toList();
    }

    public CPX2 getCPX2(TAG tag) {
        return new CPX2(tag);
    }

    public boolean CheckEntryType(int cpIndex, TAG... tags) {
        Constant cell = getConst(cpIndex);
        if (cell != null) {
            return cell.tag.belongsTo(tags);
        }
        return false;
    }

    public TAG getTag(int cpIndex) {
        Constant cell = getConst(cpIndex);
        if (cell != null) {
            return cell.tag;
        }
        return null;
    }


    /**
     * TAG - A Tag descriptor of constants in the constant pool
     */
    public enum TAG {
        CONSTANT_NULL((byte) 0, "null", "CONSTANT_NULL", 1),
        CONSTANT_UTF8((byte) 1, "Utf8", "CONSTANT_UTF8", 1),
        // Obsolete CONSTANT_UNICODE((byte) 2, "unicode", "CONSTANT_UNICODE", 1),
        CONSTANT_INTEGER((byte) 3, "int", "Integer", "CONSTANT_INTEGER", 1),
        CONSTANT_FLOAT((byte) 4, "float", "Float", "CONSTANT_FLOAT", 1),
        CONSTANT_LONG((byte) 5, "long", "Long", "CONSTANT_LONG", 2),
        CONSTANT_DOUBLE((byte) 6, "double", "Double", "CONSTANT_DOUBLE", 2),
        CONSTANT_CLASS((byte) 7, "class", "Class", "CONSTANT_CLASS", 1),
        CONSTANT_STRING((byte) 8, "String", "CONSTANT_STRING", 1),
        CONSTANT_FIELD((byte) 9, "Field", "Fieldref", "CONSTANT_FIELD", 1),
        CONSTANT_METHOD((byte) 10, "Method", "Methodref", "CONSTANT_METHOD", 1),
        CONSTANT_INTERFACEMETHOD((byte) 11, "InterfaceMethod", "InterfaceMethodref", "CONSTANT_INTERFACEMETHOD", 1),
        CONSTANT_NAMEANDTYPE((byte) 12, "NameAndType", "CONSTANT_NAMEANDTYPE", 1),
        CONSTANT_METHODHANDLE((byte) 15, "MethodHandle", "CONSTANT_METHODHANDLE", 1),
        CONSTANT_METHODTYPE((byte) 16, "MethodType", "CONSTANT_METHODTYPE", 1),
        CONSTANT_DYNAMIC((byte) 17, "Dynamic", "CONSTANT_DYNAMIC", 1),
        CONSTANT_INVOKEDYNAMIC((byte) 18, "InvokeDynamic", "CONSTANT_INVOKEDYNAMIC", 1),
        CONSTANT_MODULE((byte) 19, "Module", "CONSTANT_MODULE", 1),
        CONSTANT_PACKAGE((byte) 20, "Package", "CONSTANT_PACKAGE", 1);

        private final Byte value;
        private final String tagName;
        private final String tagAlias;
        private final String printValue;
        private final int size;

        TAG(byte value, String tagName, String printValue, int size) {
            this.value = value;
            this.tagName = tagName;
            this.tagAlias = "";
            this.printValue = printValue;
            this.size = size;
        }

        TAG(byte value, String tagName, String tagAlias, String printValue, int size) {
            this.value = value;
            this.tagName = tagName;
            this.tagAlias = tagAlias;
            this.printValue = printValue;
            this.size = size;
        }

        public String printValue() {
            String[] strings = this.name().split("_");
            return strings[0] + "_" +
                    strings[1].substring(0, 1).toUpperCase() +
                    strings[1].substring(1).toLowerCase();
        }

        public boolean belongsTo(TAG... tags) {
            for (TAG tag : tags) {
                if (this.value.equals(tag.value)) {
                    return true;
                }
            }
            return false;
        }

        public byte value() {
            return value;
        }

        public String tagName() {
            return tagName;
        }

        public int getPrintWidth(boolean tableFormat) {
            String str = tableFormat ? this.tagAlias() : this.tagName;
            return str.length();
        }

        public String tagAlias() {
            return tagAlias.isEmpty() ? tagName : tagAlias;
        }

        public int size() {
            return size;
        }

        @Override
        public String toString() {
            return "<" + tagName + "> ";
        }
    }

    /**
     * SUBTAG - A Tag descriptor of form method-handle constants
     */
    public enum SUBTAG {
        REF_GETFIELD((byte) 1, "REF_getField", "REF_GETFIELD"),
        REF_GETSTATIC((byte) 2, "REF_getStatic", "REF_GETSTATIC"),
        REF_PUTFIELD((byte) 3, "REF_putField", "REF_PUTFIELD"),
        REF_PUTSTATIC((byte) 4, "REF_putStatic", "REF_PUTSTATIC"),
        REF_INVOKEVIRTUAL((byte) 5, "REF_invokeVirtual", "REF_INVOKEVIRTUAL"),
        REF_INVOKESTATIC((byte) 6, "REF_invokeStatic", "REF_INVOKESTATIC"),
        REF_INVOKESPECIAL((byte) 7, "REF_invokeSpecial", "REF_INVOKESPECIAL"),
        REF_NEWINVOKESPECIAL((byte) 8, "REF_newInvokeSpecial", "REF_NEWINVOKESPECIAL"),
        REF_INVOKEINTERFACE((byte) 9, "REF_invokeInterface", "REF_INVOKEINTERFACE");

        private final Byte value;
        private final String tagName;
        private final String printValue;

        SUBTAG(byte val, String tagName, String printValue) {
            value = val;
            this.tagName = tagName;
            this.printValue = printValue;
        }

        public byte value() {
            return value;
        }

        public String description() {
            return printValue;
        }

        @Override
        public String toString() {
            return "<" + tagName + "> ";
        }
    }

    /**
     * Constant - Base class of all constant entries
     */
    public abstract class Constant<T> {
        // tag the descriptor for the constant
        protected final TAG tag;
        protected final T value;
        private final List<IOException> issues = new ArrayList<>();
        // comment shift is used by the print method
        protected int commentPadding = Indenter.COMMENT_PADDING;

        public Constant(TAG tag, T value) {
            this.tag = tag;
            this.value = value;
        }

        public void jasmPrint(ToolOutput out, int spacePadding) {
            out.prints(PadRight(tag.tagName(), spacePadding));
        }

        public void tablePrint(ToolOutput out, int spacePadding) {
            out.prints(PadRight(tag.tagAlias(), spacePadding));
        }

        public int size() {
            return 1;
        }

        public List<IOException> getIssues() {
            return issues;
        }

        public void setIssue(IOException value) {
            issues.add(value);
        }

        public void setCommentPadding(int commentPadding) {
            this.commentPadding = commentPadding;
        }

        public String stringVal() {
            return String.valueOf(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Constant)) return false;
            Constant<?> constant = (Constant<?>) o;
            if (tag != constant.tag) return false;
            return value.equals(constant.value);
        }

        @Override
        public int hashCode() {
            int result = tag.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "<CONSTANT %s %s>".formatted(tag, stringVal());
        }
    }

    /**
     * CP_Str - Constant entries that contain String data. usually is a CONSTANT_UTF8
     */
    class CP_Str extends Constant<String> {

        CP_Str(TAG tag, String value) {
            super(tag, value);
        }

        public String stringVal() {
            return StringUtils.Utf8ToString(value, "\"");
        }


        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            out.printlns(stringVal() + ";");
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            // out.printlns(StringUtils.Utf8ToString(value));
            out.printlns(stringVal() + ";");
        }
    }

    /**
     * CP_Int - Constant entries that contain Integer data. usually is a CONSTANT_INTEGER
     */
    class CP_Int extends Constant<Integer> {

        CP_Int(TAG tag, int value) {
            super(tag, value);
        }

        @Override
        public String stringVal() {
            return classData.printHEX ? HexUtils.toHex(value) : value.toString();
        }

        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            out.printlns(stringVal() + ";");
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            // out.printlns(stringVal());
            out.printlns(stringVal() + ";");
        }
    }

    /**
     * CP_Long - Constant entries that contain LongInteger data. usually is a CONSTANT_LONG
     * These take up 2 slots in the constant pool.
     */
    class CP_Long extends Constant<Long> {

        CP_Long(TAG tag, long value) {
            super(tag, value);
        }

        @Override
        public String stringVal() {
            return classData.printHEX ? HexUtils.toHex(value) + 'l' : value.toString() + 'l';
        }


        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            out.printlns(stringVal() + ";");
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            // out.printlns(stringVal());
            out.printlns(stringVal() + ";");
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /**
     * CP_Float - Constant entries that contain Float data. usually is a CONSTANT_FLOAT
     */
    class CP_Float extends Constant<Float> {

        CP_Float(TAG tag, float value) {
            super(tag, value);
        }

        @Override
        public String stringVal() {
            if (classData.printHEX) {
                return "bits " + HexUtils.toHex(Float.floatToIntBits(value));
            }
            String sf = (value).toString();
            if (value.isNaN() || value.isInfinite()) {
                return sf;
            }
            return sf + "f";
        }

        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            out.printlns(stringVal() + ";");
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            // out.printlns(stringVal());
            out.printlns(stringVal() + ";");

        }
    }

    /**
     * CP_Double - Constant entries that contain double-precision float data. usually is a CONSTANT_DOUBLE
     * These take up 2 slots in the constant pool.
     */
    class CP_Double extends Constant<Double> {

        CP_Double(TAG tag, double value) {
            super(tag, value);
        }

        @Override
        public String stringVal() {
            if (classData.printHEX) {
                return "bits " + HexUtils.toHex(Double.doubleToLongBits(value)) + 'l';
            }
            String sd = value.toString();
            if (value.isNaN() || value.isInfinite()) {
                return sd;
            }
            return sd + "d";
        }

        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            out.printlns(stringVal() + ";");
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            // out.printlns(stringVal());
            out.printlns(stringVal() + ";");
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /**
     * CPX - Constant entries that contain a single constant-pool index. Usually, this includes:
     * CONSTANT_CLASS CONSTANT_METHODTYPE CONSTANT_STRING CONSTANT_MODULE CONSTANT_PACKAGE
     */
    class CPX extends Constant<Integer> {

        // value is Constant Pool index
        CPX(TAG tag, int cpx) {
            super(tag, cpx);
        }

        @Override
        public String stringVal() {
            String str = "UnknownTag";
            switch (tag) {
                case CONSTANT_CLASS -> str = getShortClassName(getClassName(this), classData.packageName);
                case CONSTANT_PACKAGE, CONSTANT_MODULE -> str = getString(value, index -> "#" + index);
                case CONSTANT_METHODTYPE, CONSTANT_STRING -> str = StringValue(value);
                default -> {
                }
            }
            return str;
        }

        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            print();
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            print();
        }

        // Identical printing part for both JASM (simple) and extended (table) formats.
        private void print() {
            switch (tag) {
                case CONSTANT_CLASS, CONSTANT_STRING, CONSTANT_METHODTYPE, CONSTANT_PACKAGE, CONSTANT_MODULE -> {
                    if (skipComments) {
                        println("#" + value + ";");
                    } else {
                        printPadRight("#" + value + ";", commentPadding).println(" // " + stringVal());
                    }
                }
                default -> {
                }
            }
        }

    }

    /**
     * CPX2 - Constant entries that contain two constant-pool indices. Usually, this includes:
     * CONSTANT_FIELD CONSTANT_METHOD CONSTANT_INTERFACEMETHOD CONSTANT_NAMEANDTYPE
     * CONSTANT_METHODHANDLE CONSTANT_DYNAMIC CONSTANT_INVOKEDYNAMIC
     */
    public class CPX2 extends Constant<Integer> {

        protected final int value2;

        // stack to control circular references in bsm arguments.
        final Stack<Constant> stack = new Stack<>();

        CPX2(TAG tag, int cpx1, int cpx2) {
            super(tag, cpx1);
            this.value2 = cpx2;
        }

        CPX2(TAG tag) {
            super(tag, 0);
            this.value2 = 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CPX2)) return false;
            if (!super.equals(o)) return false;
            CPX2 cpx2 = (CPX2) o;
            return value2 == cpx2.value2;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + value2;
            return result;
        }

        @Override
        public String stringVal() {
            String str = "UnknownTag";
            switch (tag) {
                case CONSTANT_FIELD:
                    // CODETOOLS-7902660: the tag Field is not necessary while printing static parameters of a bsm
                    // Example: MethodHandle REF_getField:ClassName.FieldName:"I"
                    str = getShortClassName(getClassName(value), classData.packageName) + "." + StringValue(value2);
                    break;
                case CONSTANT_METHOD:
                case CONSTANT_INTERFACEMETHOD:
                    // CODETOOLS-7902648: added printing of the tag: Method/Interface to clarify
                    // interpreting CONSTANT_MethodHandle_info:reference_kind
                    // Example: invokedynamic InvokeDynamic REF_invokeStatic:Method java/lang/runtime/ObjectMethods.bootstrap
                    str = getPrintedTAG(tag) + getShortClassName(getClassName(value), classData.packageName) + "." +
                            StringValue(value2);
                    break;
                case CONSTANT_NAMEANDTYPE:
                    str = getName(value) + ":" + StringValue(value2);
                    break;
                case CONSTANT_METHODHANDLE:
                    str = subtagToString(value) + ":" + StringValue(value2);
                    break;
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKEDYNAMIC:
                    int bsmAttributeIndex = value;
                    int nameTypeIndex = value2;
                    BootstrapMethodData bsmData;
                    try {
                        bsmData = classData.bootstrapMethods.get(bsmAttributeIndex);
                    } catch (NullPointerException npe) {
                        return "<Missing BootstrapMethods attribute>";
                    } catch (IndexOutOfBoundsException ioobe) {
                        return "<Invalid bootstrap method index:" + bsmAttributeIndex + ">";
                    }
                    int bsm_ref = bsmData.bsmRef;
                    str = "%s:%s%s".formatted(StringValue(bsm_ref),
                            StringValue(nameTypeIndex),
                            bsmArgsAsString(bsmData, "{", "}"));
                default:
                    break;
            }
            return str;
        }

        public String bsmArgsAsString(BootstrapMethodData bsmData) {
            return bsmArgsAsString(bsmData, "", "");
        }

        public String bsmArgsAsString(BootstrapMethodData bsmData, String LeftBracket, String RightBracket) {
            StringBuilder sb = new StringBuilder();
            int bsmArgsLen = bsmData.bsmArguments.size();
            sb.append(LeftBracket);
            if (bsmArgsLen > 0) {
                for (int i = 0; i < bsmArgsLen; i++) {
                    int bsmArgIdx = bsmData.bsmArguments.get(i);
                    boolean notLastIdx = i != bsmArgsLen - 1;
                    sb.append(bsmArgAsString(bsmArgIdx, notLastIdx));
                }
            }
            sb.append(RightBracket);
            return sb.toString();
        }

        public String bsmArgAsString(int bsmArgIdx, boolean notLastIdx) {
            return bsmArgAsString(bsmArgIdx, notLastIdx ? ARGUMENT_DELIMITER : "", notLastIdx ? LINE_SPLITTER : "");
        }

        public String bsmArgWithoutDelimitersAsString(int bsmArgIdx, boolean notLastIdx) {
            return bsmArgAsString(bsmArgIdx, "", notLastIdx ? LINE_SPLITTER : "");
        }


        public String bsmArgAsString(int bsmArgIdx, String argDelimiter, String lineSplitter) {
            String str = "";
            if (inRange(bsmArgIdx)) {
                Constant cnt = pool.get(bsmArgIdx);
                if (stack.search(this) == -1) {
                    stack.push(this);
                    str = str.concat(ConstantStrValue(bsmArgIdx)).concat(argDelimiter);
                    stack.pop();
                } else {
                    String ref;
                    if (cnt instanceof CPX2) {
                        ref = format("%-8s %d:#%d; ", cnt.tag.tagName(), cnt.value, ((CPX2) cnt).value2);
                    } else {
                        ref = format("%-8s #%d; ", cnt.tag.tagName(), cnt.value);
                    }
                    String msg = "circular reference to " + cnt.tag.tagName() + " #" + bsmArgIdx;
                    if (printCPIndex) {
                        str = str.concat(PadRight(ref, CIRCULAR_COMMENT_OFFSET)).concat("<").concat(msg).concat(">").concat(argDelimiter);
                    } else {
                        str = str.concat(PadRight(ref, CIRCULAR_COMMENT_OFFSET)).concat(" // <").concat(msg).concat(">").concat(lineSplitter);
                    }
                    cnt.setIssue(new IOException(msg));
                }
            } else {
                str = environment.getInfo("info.corrupted_cp_entry");
            }
            return str;
        }

        @Override
        public void jasmPrint(ToolOutput out, int spacePadding) {
            super.jasmPrint(out, spacePadding);
            print();
        }

        @Override
        public void tablePrint(ToolOutput out, int spacePadding) {
            super.tablePrint(out, spacePadding);
            print();
        }

        // Identical printing part for both JASM (simple) and extended (table) formats.
        private void print() {
            if (skipComments) {
                switch (tag) {
                    case CONSTANT_FIELD, CONSTANT_METHOD, CONSTANT_INTERFACEMETHOD ->
                            println("#%d.#%d;", value, value2);
                    case CONSTANT_METHODHANDLE, CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC ->
                            println("%d:#%d;", value, value2);
                    case CONSTANT_NAMEANDTYPE -> println("#%d:#%d;", value, value2);
                    default ->
                            printPadRight(format("%d:#%d;", value, value2), commentPadding).println(" // unknown tag: " + tag.tagName);
                }
            } else {
                switch (tag) {
                    case CONSTANT_FIELD, CONSTANT_METHOD, CONSTANT_INTERFACEMETHOD ->
                            printPadRight(format("#%d.#%d;", value, value2), commentPadding).println(" // " + stringVal());
                    case CONSTANT_METHODHANDLE ->
                            printPadRight(format("%d:#%d;", value, value2), commentPadding).println(" // " + stringVal());
                    case CONSTANT_NAMEANDTYPE ->
                            printPadRight(format("#%d:#%d;", value, value2), commentPadding).println(" // " + stringVal());
                    case CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC ->
                            printPadRight(format("%d:#%d;", value, value2), commentPadding).println(" // #%d:%s", value, StringValue(value2));
                    default ->
                            printPadRight(format("%d:#%d;", value, value2), commentPadding).println(" // unknown tag: " + tag.tagName);
                }
            }
        }


        public boolean refersClassMember() {
            return tag == TAG.CONSTANT_FIELD || tag == TAG.CONSTANT_METHOD || tag == TAG.CONSTANT_INTERFACEMETHOD;
        }
    }
}
