/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.jasm.JasmTokens;
import org.openjdk.asmtools.jasm.Modifiers;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.asmtools.jasm.RuntimeConstants.*;
import static org.openjdk.asmtools.jasm.Tables.*;

/**
 * Central class data for of the Java Disassembler
 */
public class ClassData extends MemberData {

    /*-------------------------------------------------------- */
    /* ClassData Fields                                        */
    // -----------------------------
    // Header Info
    // -----------------------------
    /**
     * Version info
     */
    protected int minor_version, major_version;

    /**
     * Constant Pool index to this class
     */
    protected int this_cpx;
    /**
     * Constant Pool index to this classes parent (super)
     */
    protected int super_cpx;
    /**
     * Constant Pool index to a file reference to the Java source
     */
    protected int source_cpx = 0;

    /**
     * The Constant Pool
     */
    protected ConstantPool pool;

    /**
     * The interfaces this class implements
     */
    protected int[] interfaces;

    /**
     * The fields of this class
     */
    protected ArrayList<FieldData> fields;

    /**
     * The methods of this class
     */
    protected ArrayList<MethodData> methods;

    /**
     * The inner-classes of this class
     */
    protected ArrayList<InnerClassData> innerClasses;

    /**
     * The bootstrapmethods this class implements
     */
    protected ArrayList<BootstrapMethodData> bootstrapMethods;

    /**
     * The module this class file presents
     */
    protected ModuleData moduleData;

    // other parsing fields
    protected PrintWriter out;
    protected String pkgPrefix = "";
    private int pkgPrefixLen = 0;
    private int length; //The number of elements in the buffer
    private TextLines source = null;
    private static final String initialTab = ""; //The number of elements in the buffer


    /* -------------------------------------------------------- */
    /* ClassData Methods */
    public ClassData(PrintWriter out) {
        this.out = out;
        init(this);
        memberType = "ClassData";
        TraceUtils.traceln("printOptions=" + options.toString());
        pool = new ConstantPool(this);
    }

    /*========================================================*/
    /* Read Methods */
    public void read(File in) throws IOException {
        read(new DataInputStream(new FileInputStream(in)));
    }

    public void read(String in) throws IOException {
        read(new DataInputStream(new FileInputStream(in)));
    }

    /**
     *
     * readFields
     *
     * read and resolve the field data
     *
     */
    protected void readFields(DataInputStream in) throws IOException {
        int nfields = in.readUnsignedShort();
        TraceUtils.traceln("nfields=" + nfields);
        fields = new ArrayList<>(nfields);
        for (int k = 0; k < nfields; k++) {
            FieldData field = new FieldData(this);
            TraceUtils.traceln("  FieldData: #" + k);
            field.read(in);
            fields.add(field);
        }
    }

    /**
     *
     * readMethods
     *
     * read and resolve the method data
     *
     */
    protected void readMethods(DataInputStream in) throws IOException {
        int nmethods = in.readUnsignedShort();
        TraceUtils.traceln("nmethods=" + nmethods);
        methods = new ArrayList<>(nmethods);
        for (int k = 0; k < nmethods; k++) {
            MethodData method = new MethodData(this);
            TraceUtils.traceln("  MethodData: #" + k);
            method.read(in);
            methods.add(method);
        }
    }

    /**
     *
     * readInterfaces
     *
     * read and resolve the interface data
     *
     */
    protected void readInterfaces(DataInputStream in) throws IOException {
        // Read the interface names
        int numinterfaces = in.readUnsignedShort();
        TraceUtils.traceln("numinterfaces=" + numinterfaces);
        interfaces = new int[numinterfaces];
        for (int i = 0; i < numinterfaces; i++) {
            int intrf_cpx = in.readShort();
            TraceUtils.traceln("  intrf_cpx[" + i + "]=" + intrf_cpx);
            interfaces[i] = intrf_cpx;
        }
    }

    /**
     *
     * readAttributes
     *
     * read and resolve the attribute data
     *
     */
    @Override
    protected boolean handleAttributes(DataInputStream in, AttrTag attrtag, int attrlen) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attrtag) {
            case ATT_SourceFile:
                // Read SourceFile Attr
                if (attrlen != 2) {
                    throw new ClassFormatError("invalid attr length");
                }
                source_cpx = in.readUnsignedShort();
                break;
            case ATT_InnerClasses:
                // Read InnerClasses Attr
                int num1 = in.readUnsignedShort();
                if (2 + num1 * 8 != attrlen) {
                    throw new ClassFormatError("invalid attr length");
                }
                innerClasses = new ArrayList<>(num1);
                for (int j = 0; j < num1; j++) {
                    InnerClassData innerClass = new InnerClassData(this);
                    innerClass.read(in);
                    innerClasses.add(innerClass);
                }
                break;
            case ATT_BootstrapMethods:
                // Read BootstrapMethods Attr
                int num2 = in.readUnsignedShort();
                bootstrapMethods = new ArrayList<>(num2);
                for (int j = 0; j < num2; j++) {
                    BootstrapMethodData bsmData = new BootstrapMethodData(this);
                    bsmData.read(in);
                    bootstrapMethods.add(bsmData);
                }
                break;
            case ATT_Module:
                // Read Module Attribute
                moduleData = new ModuleData(this);
                moduleData.read(in);
                break;
            default:
                handled = false;
                break;
        }
        return handled;
    }

    /**
     *
     * read
     *
     * read and resolve the class data
     *
     */
    public void read(DataInputStream in) throws IOException {
        // Read the header
        // -----------------------------------------------
        int magic = in.readInt();
        if (magic != JAVA_MAGIC) {
            throw new ClassFormatError("wrong magic: " + HexUtils.toHex(magic) + ", expected " + HexUtils.toHex(JAVA_MAGIC));
        }
        minor_version = in.readShort();
        major_version = in.readShort();

        // Read the constant pool
        // -----------------------------------------------
        pool.read(in);
        access = in.readUnsignedShort(); // & MM_CLASS; // Q
        this_cpx = in.readUnsignedShort();
        super_cpx = in.readUnsignedShort();
        TraceUtils.traceln("access=" + access + " " + Modifiers.accessString(access, CF_Context.CTX_INNERCLASS) + " this_cpx=" + this_cpx + " super_cpx=" + super_cpx);

        // Read the interfaces
        // -----------------------------------------------
        readInterfaces(in);

        // Read the fields
        // -----------------------------------------------
        readFields(in);

        // Read the methods
        // -----------------------------------------------
        readMethods(in);

        // Read the attributes
        readAttributes(in);

        TraceUtils.traceln("");
        TraceUtils.traceln("--------------------------------------------------------------------");
        TraceUtils.traceln("");
    }

    /*========================================================*/
    /**
     *
     * getSrcLine
     *
     * read and resolve the attribute data
     *
     */
    public String getSrcLine(int lnum) {
        if (source == null) {
            return null;  // impossible call
        }
        String line;
        try {
            line = source.getLine(lnum);
        } catch (ArrayIndexOutOfBoundsException e) {
            line = "Line number " + lnum + " is out of bounds";
        }
        return line;
    }

    private <T extends AnnotationData> void printAnnotations(List<T> annotations) {
        if (annotations != null) {
            for (T ad : annotations) {
                ad.print(out, initialTab);
                out.println();
            }
        }
    }

    public void print() throws IOException {
        int k, l;
        String className = "";
        if( isModuleUnit() ) {
            // Print the Annotations
            printAnnotations(visibleAnnotations);
            printAnnotations(invisibleAnnotations);
        } else {
            className = pool.getClassName(this_cpx);
            pkgPrefixLen = className.lastIndexOf("/") + 1;
            // Write the header
            // package-info compilation unit
            if (className.endsWith("package-info")) {
                // Print the Annotations
                printAnnotations(visibleAnnotations);
                printAnnotations(invisibleAnnotations);
                printAnnotations(visibleTypeAnnotations);
                printAnnotations(invisibleTypeAnnotations);
                if (pkgPrefixLen != 0) {
                    pkgPrefix = className.substring(0, pkgPrefixLen);
                    out.print("package  " + pkgPrefix.substring(0, pkgPrefixLen - 1) + " ");
                    out.print("version " + major_version + ":" + minor_version + ";");
                }
                out.println();
                return;
            }
            if (pkgPrefixLen != 0) {
                pkgPrefix = className.substring(0, pkgPrefixLen);
                out.println("package  " + pkgPrefix.substring(0, pkgPrefixLen - 1) + ";");
                className = pool.getShortClassName(this_cpx, pkgPrefix);
            }
            out.println();
            // Print the Annotations
            printAnnotations(visibleAnnotations);
            printAnnotations(invisibleAnnotations);
            printAnnotations(visibleTypeAnnotations);
            printAnnotations(invisibleTypeAnnotations);
            if ((access & ACC_SUPER) != 0) {
                out.print("super ");
                access = access & ~ACC_SUPER;
            }
        }
// see if we are going to print: abstract interface class
// then replace it with just: interface
printHeader:
        {
printSugar:
            {
                if ((access & ACC_ABSTRACT) == 0) {
                    break printSugar;
                }
                if ((access & ACC_INTERFACE) == 0) {
                    break printSugar;
                }
                if (options.contains(Options.PR.CPX)) {
                    break printSugar;
                }
                if (this_cpx == 0) {
                    break printSugar;
                }

                // make sure the this_class is a valid class ref
                ConstantPool.Constant this_const = pool.getConst(this_cpx);
                if (this_const == null || this_const.tag != ConstantPool.TAG.CONSTANT_CLASS) {
                    break printSugar;
                }

                // all conditions met, print syntactic sugar:
                out.print(Modifiers.accessString(access & ~ACC_ABSTRACT, CF_Context.CTX_CLASS));
                if (isSynthetic) {
                    out.print("synthetic ");
                }
                if (isDeprecated) {
                    out.print("deprecated ");
                }
                out.print(" " + pool.getShortClassName(this_cpx, pkgPrefix));
                break printHeader;
            }

            if(isModuleUnit()) {
                out.print(moduleData.getModuleHeader());
            } else {
                // not all conditions met, print header in ordinary way:
                out.print(Modifiers.accessString(access, CF_Context.CTX_CLASS));
                if (isSynthetic) {
                    out.print("synthetic ");
                }
                if (isDeprecated) {
                    out.print("deprecated ");
                }
                if (options.contains(Options.PR.CPX)) {
                    out.print("\t#" + this_cpx + "; //");
                }
                pool.PrintConstant(out, this_cpx);
            }
        }
        out.println();
        if(!isModuleUnit()) {
            if (!pool.getClassName(super_cpx).equals("java/lang/Object")) {
                out.print("\textends ");
                pool.printlnClassId(out, super_cpx);
                out.println();
            }
        }
        l = interfaces.length;

        if (l > 0) {
            for (k = 0; k < l; k++) {
                if (k == 0) {
                    out.print("\timplements ");
                } else {
                    out.print("\t\t ");
                }
                boolean printComma = (l > 1 && k < (l - 1));
                pool.printlnClassId(out, interfaces[k], printComma);
                out.println();
            }
        }
        out.println("\tversion " + major_version + ":" + minor_version);
        out.println("{");

        if ((options.contains(Options.PR.SRC)) && (source_cpx != 0)) {
            String source_name = pool.getName(source_cpx);
            out.println("\t// Compiled from " + source_name);
            try {
                source = new TextLines(source_name);
            } catch (IOException ignored) {}
        }
        // keep this new line for classes to pass huge test suite.
        if(!isModuleUnit())
            out.println();

        // Print the constant pool
        if (options.contains(Options.PR.CP)) {
            pool.print(out);
            out.println();
        }
        // Don't print fields, methods, inner classes and bootstrap methods if it is module-info entity
        if ( !isModuleUnit() ) {

            // Print the fields
            if (fields != null && !fields.isEmpty()) {
                for (FieldData curf : fields) {
                    curf.print();
                }
            }

            // Print the methods
            if (methods != null && !methods.isEmpty()) {
                for (MethodData curm : methods) {
                    boolean skipBlankLine = false;
                    curm.print(skipBlankLine);
                }
                out.println();
            }

            // Print the inner classes
            if (innerClasses != null && !innerClasses.isEmpty()) {
                for (InnerClassData icd : innerClasses) {
                    icd.print();
                }
                out.println();
            }
            // Print the BootstrapMethods
            //
            // Only print these if printing extended constants
            if ((options.contains(Options.PR.CPX)) && bootstrapMethods != null && !bootstrapMethods.isEmpty()) {
                for (BootstrapMethodData bsmdd : bootstrapMethods) {
                    bsmdd.print();
                }
                out.println();
            }
            out.println("} // end Class " + className);
        } else {
            // Print module attributes
            moduleData.print();
            out.print("} // end Module ");
            out.print( moduleData.getModuleName());
            if(moduleData.getModuleVersion() != null)
                out.print(" @" + moduleData.getModuleVersion());
            out.println();
        }
    } // end ClassData.print()

    // Gets the type of processed binary
    private boolean isModuleUnit() {
        return moduleData != null;
    }
}// end class ClassData

