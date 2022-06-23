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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.asmutils.HexUtils;
import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.structure.ClassFileContext.CLASS;
import static org.openjdk.asmtools.common.structure.ClassFileContext.MODULE;
import static org.openjdk.asmtools.common.structure.EModifier.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.JAVA_MAGIC;

/**
 * Central class data for of the Java Disassembler
 */
public class ClassData extends MemberData<ClassData> {

    // -----------------------------------------------------------------------------------------------
    // Header Info
    // -----------------------------------------------------------------------------------------------
    // Version info
    protected CFVersion cfVersion = new CFVersion();

    // Constant Pool index to this class
    protected int this_cpx;

    // Pre-initialized Constant Pool names calculated by this_cpx
    protected String className = "";
    protected String packageName = "";
    protected String classShortName = "";

    // Constant Pool index to a file reference to the Java source
    protected int source_cpx = 0;
    protected String sourceName = null;

    // Constant Pool index to this classes parent (super)
    protected int super_cpx;

    // -----------------------------------------------------------------------------------------------
    // Interfaces, Fields, Methods && Attributes
    // -----------------------------------------------------------------------------------------------
    // The interfaces this class implements
    protected int[] interfaces;

    // The fields of this class
    protected ArrayList<FieldData> fields;

    // The methods of this class
    protected ArrayList<MethodData> methods;

    // The record attribute of this class (since class file 58.65535)
    protected RecordData recordData;

    // The inner-classes of this class
    protected ArrayList<InnerClassData> innerClasses;

    // The bootstrap methods this class implements
    protected ArrayList<BootstrapMethodData> bootstrapMethods;

    //The module this class file presents
    protected ModuleData moduleData;

    // The NestHost of this class (since class file: 55.0)
    protected NestHostData nestHost;

    // The NestMembers of this class (since class file: 55.0)
    protected NestMembersData nestMembers;

    // The PermittedSubclasses of this class (JEP 360 (Sealed types): class file 59.65535)
    protected PermittedSubclassesData permittedSubclassesData;

    // Valhalla
    protected PreloadData preloadData;

    // source file data
    private TextLines sourceLines = null;
    private Path classFile = null;

    public ClassData(JdisEnvironment environment) {
        super(environment);
        memberType = "ClassData";
        super.environment = environment;
        environment.traceln("printOptions=" + Options.asShortString());
        super.pool = new ConstantPool(this);
        super.init(this);
    }

    public void read(File inputFile) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputFile))) {
            read(dis);
        }
        classFile = inputFile.toPath();
    }

    public void read(String inputFileName) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputFileName))) {
            read(dis);
        }
        classFile = Paths.get(inputFileName);
    }

    /**
     * Read and resolve the field data
     */
    protected void readFields(DataInputStream in) throws IOException {
        int nFields = in.readUnsignedShort();
        environment.traceln("fields=#" + nFields);
        fields = new ArrayList<>(nFields);
        for (int k = 0; k < nFields; k++) {
            FieldData field = new FieldData(this);
            environment.traceln("  FieldData: #" + k);
            field.read(in);
            fields.add(field);
        }
    }

    /**
     * Read and resolve the method data
     */
    protected void readMethods(DataInputStream in) throws IOException {
        int nMethods = in.readUnsignedShort();
        environment.traceln("methods=#" + nMethods);
        methods = new ArrayList<>(nMethods);
        for (int k = 0; k < nMethods; k++) {
            MethodData method = new MethodData(this);
            environment.traceln("MethodData: #" + k);
            method.read(in);
            methods.add(method);
        }
    }

    /**
     * Read and resolve the interface data
     */
    protected void readInterfaces(DataInputStream in) throws IOException {
        // Read the interface names
        int nInterfaces = in.readUnsignedShort();
        environment.traceln("interfaces=#" + nInterfaces);
        interfaces = new int[nInterfaces];
        for (int i = 0; i < nInterfaces; i++) {
            int interfaceCpx = in.readShort();
            environment.traceln("  InterfaceCpx[" + i + "]=" + interfaceCpx);
            interfaces[i] = interfaceCpx;
        }
    }

    /**
     * Read and resolve the attribute data
     */
    @Override
    protected boolean handleAttributes(DataInputStream in, EAttribute attributeTag, int attributeLength) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attributeTag) {
            case ATT_Signature -> {
                if (signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "ClassFile");
                }
                signature = new SignatureData(this).read(in, attributeLength);
            }
            case ATT_SourceFile -> {
                // Read SourceFile Attr
                if (attributeLength != 2) {
                    throw new FormatError("err.invalid.attribute.length", "SourceFile_attribute", attributeLength);
                }
                if (source_cpx != 0) {
                    environment.warning("warn.one.attribute.required", "SourceFile", "ClassFile");
                }
                source_cpx = in.readUnsignedShort();
            }
            case ATT_InnerClasses -> {
                // Read InnerClasses Attr
                int num1 = in.readUnsignedShort();
                if (2 + num1 * 8 != attributeLength) {
                    throw new FormatError("err.invalid.attribute.length", "InnerClasses_attribute", attributeLength);
                }
                innerClasses = new ArrayList<>(num1);
                for (int j = 0; j < num1; j++) {
                    InnerClassData innerClass = new InnerClassData(this);
                    innerClass.read(in);
                    innerClasses.add(innerClass);
                }
            }
            case ATT_BootstrapMethods -> {
                // Read BootstrapMethods Attr
                int num2 = in.readUnsignedShort();
                bootstrapMethods = new ArrayList<>(num2);
                for (int j = 0; j < num2; j++) {
                    BootstrapMethodData bsmData = new BootstrapMethodData(this);
                    bsmData.read(in);
                    bootstrapMethods.add(bsmData);
                }
            }
            case ATT_Module -> {
                // Read Module Attribute
                moduleData = new ModuleData(this);
                moduleData.read(in);
            }
            case ATT_NestHost ->
                    // Read NestHost Attribute (since class file: 55.0)
                    nestHost = new NestHostData(this).read(in, attributeLength);
            case ATT_NestMembers ->
                    // Read NestMembers Attribute (since class file: 55.0)
                    nestMembers = new NestMembersData(this).read(in, attributeLength);
            case ATT_Record -> recordData = new RecordData(this).read(in);
            case ATT_PermittedSubclasses ->
                    // Read PermittedSubclasses Attribute (JEP 360 (Sealed types): class file 59.65535)
                    permittedSubclassesData = new PermittedSubclassesData(this).read(in, attributeLength);
            case ATT_Preload ->
                    // Valhalla
                    preloadData = new PreloadData(this).read(in, attributeLength);
            default -> handled = false;
        }
        return handled;
    }

    /**
     * Read and resolve the class data
     */
    private void read(DataInputStream in) throws IOException {
        // Read the header
        try {
            int magic = in.readInt();

            if (magic != JAVA_MAGIC) {
                throw new ClassCastException("wrong magic: " + HexUtils.toHex(magic) + ", expected " + HexUtils.toHex(JAVA_MAGIC));
            }

            cfVersion.setMinorVersion((short) in.readUnsignedShort());
            cfVersion.setMajorVersion((short) in.readUnsignedShort());

            // Read the constant pool
            pool.read(in);
            access = in.readUnsignedShort(); // & MM_CLASS; // Q
            this_cpx = in.readUnsignedShort();
            super_cpx = in.readUnsignedShort();

            environment.traceln("0x%04X [ %s] this_cpx=%d super_cpx=%d", access,
                    EModifier.asNames(access, EModifier.isModule(access) ? CLASS : MODULE),
                    this_cpx, super_cpx);

            // Read the interfaces
            readInterfaces(in);

            // Read the fields
            readFields(in);

            // Read the methods
            readMethods(in);

            // Read the attributes
            readAttributes(in);

            //Pre-initialize names,indexes needed for printing.
            initClassNames(this_cpx);
            //
            environment.traceln("\n<< Reading is done >>");
        } catch (EOFException eofException) {
            throw new FormatError("err.eof");
        }
    }

    /**
     * Initializes class, package names needed for printing asm file
     *
     * @param this_cpx The constant_pool entry at that index is a CONSTANT_Class_info structure (§4.4.1)
     *                 representing the class or interface defined by this class file or
     *                 this_class is module-info in the case of a module.
     */
    private void initClassNames(int this_cpx) {
        this.className = pool.getClassName(this_cpx);
        final int idx = className.lastIndexOf('/');
        if (idx != -1) {
            this.packageName = className.substring(0, idx);
            this.classShortName = className.substring(idx + 1);
        } else {
            this.classShortName = className;
        }
        if (source_cpx != 0) {
            sourceName = pool.getString(source_cpx, index -> "#" + index);
        }
    }

    /**
     * Determines if this Class has a package
     *
     * @return true if the package exists for this class
     */
    public boolean hasPackage() {
        return this.packageName.length() != 0;
    }

    /**
     * Read and resolve the attribute data
     */
    public String getSrcLine(int lineNum) {
        if (sourceLines == null) {
            return null;  // impossible call
        }
        String line;
        try {
            line = sourceLines.getLine(lineNum);
        } catch (ArrayIndexOutOfBoundsException e) {
            line = format("Line number %d is out of bounds", lineNum);
        }
        return line;
    }

    @SafeVarargs
    private <T extends AnnotationData> void printAnnotations(List<T>... annotationLists) throws IOException {
        if (annotationLists != null) {
            for (List<T> list : annotationLists) {
                if (list != null) {
                    for (T annotation : list) {
                        annotation.initIndent(0);
                        annotation.print();
                        println();
                    }
                }
            }
        }
    }

    /**
     * Print asm file.
     */
    @Override
    public void print() throws IOException {
        if (className.endsWith("module-info") ||
                EModifier.isModule(access)) {            // module-info compilation unit
            // Print the Annotations
            printAnnotations(visibleAnnotations, invisibleAnnotations);
            // Print Module Header
            if (moduleData == null) {
                // exception: 4.7.25. The Module Attribute is empty.
                moduleData = new ModuleData(this);
            }
            print(moduleData.getModuleHeader(format("version %s", cfVersion.asString())));
            println();
            println("{");
            // Print the constant pool
            if (printConstantPool) {
                pool.print();
            }
            // Print module attributes
            moduleData.print();
            //
            print(format("} // end of module %s", moduleData.getModuleName()));
            if (moduleData.getModuleVersion() != null)
                print("@" + moduleData.getModuleVersion());
            println();
        } else if (className.endsWith("package-info")) {    // package-info compilation unit
            if (printConstantPool) {
                pool.print();
            }
            // Print the Annotations
            printAnnotations(visibleAnnotations, invisibleAnnotations);
            printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
            if (hasPackage()) {
                println(String.format("package %s version %s;", packageName, cfVersion.asString()));
            }
        } else {                                            // class/interface compilation unit
            if (hasPackage()) {
                println(format("package %s;%n", packageName));
            }
            // Print the Annotations
            printAnnotations(visibleAnnotations, invisibleAnnotations);
            printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
            // In Java SE 8, the ACC_SUPER semantics became mandatory, regardless of the setting of ACC_SUPER or
            // the class file version number, and the flag no longer had any effect.
            // Skip printing "super modifier"
            access &= ~ACC_SUPER.getFlag();

            String name = pool.inRange(this_cpx) ? pool.getShortClassName(this_cpx, this.packageName) :  "?? invalid index";
            Pair<String, String> signInfo = ( signature != null) ?
                    signature.getPrintInfo((i)->pool.inRange(i)) :
                    new Pair<>("", "");
            // An interface is distinguished by the ACC_INTERFACE flag being set.
            if (EModifier.isInterface(access)) {       // interface compilation unit
                print(EModifier.asKeywords(access & ~ACC_ABSTRACT.getFlag(), CLASS));
                print(printCPIndex ?
                        format("interface #%d%s /* %s%s */", this_cpx, signInfo.first, name, signInfo.second) :
                        format("interface %s", name, signInfo.second)
                );
            } else {                                    // class compilation unit
                // add synthetic, deprecated if necessary
                print(EModifier.asKeywords(access, CLASS) + getPseudoFlagsAsString());
                print(printCPIndex ?
                        format("class #%d%s /* %s%s */", this_cpx, signInfo.first, name, signInfo.second) :
                        format("class %s%s", name, signInfo.second)
                );
                // if base class is not j.l.Object prints the "extends" statement
                if (this_cpx < pool.size() && this_cpx > 0) {
                    if (!pool.getClassName(super_cpx).equals("java/lang/Object")) {
                        print(printCPIndex ?
                                format(" extends #%d /* %s */", super_cpx, pool.getShortClassName(super_cpx, this.packageName)) :
                                format(" extends %s", pool.getShortClassName(super_cpx, this.packageName))
                        );
                    }
                } else {
                    print(printCPIndex ? format(" extends #%d /* ?? invalid index */", super_cpx) : " extends ??");
                }
            }
            // print the "implements" statement
            int numInterfaces = interfaces.length;
            if (numInterfaces > 0) {
                String statement;
                String sNames = Arrays.stream(interfaces).
                        mapToObj(cpx -> pool.getShortClassName(cpx, this.packageName)).
                        collect(Collectors.joining(", "));
                if (printCPIndex) {
                    String sIndexes = Arrays.stream(interfaces).
                            mapToObj(cpx -> format("#%d", cpx)).
                            collect(Collectors.joining(", "));
                    statement = format("%simplements %s /* %s */", (numInterfaces > 1) ? "\n" + getIndentString() : " ", sIndexes, sNames);
                } else {
                    statement = format("%simplements %s", (numInterfaces > 1) ? "\n" + getIndentString() : " ", sNames);
                }
                print(statement);
            }
            println("%sversion %s", (numInterfaces > 1) ? "\n" + getIndentString() : " ", cfVersion.asString());
            println("{");
            if (printSourceLines && (source_cpx != 0)) {
                sourceName = pool.getString(source_cpx, index -> null);
                if (sourceName != null) {
                    sourceLines = new TextLines(classFile.getParent(), sourceName);
                }
            }

            // Print the constant pool
            if (printConstantPool) {
                pool.print();
                setCommentOffset(pool.getCommentOffset());
            }

            // Print the fields
            if (printMemberDataList(fields, getCommentOffset()) &&
                    (isPrintable(recordData, permittedSubclassesData, preloadData, nestHost, nestMembers) ||
                            isPrintable(methods, innerClasses) || (printCPIndex && isPrintable(bootstrapMethods)))) {
                println();
            }

            // Print the methods
            if (printMemberDataList(methods, getCommentOffset() - getIndentSize()) &&
                    (isPrintable(recordData, permittedSubclassesData, preloadData, nestHost, nestMembers) ||
                            isPrintable(innerClasses) || (printCPIndex && isPrintable(bootstrapMethods)))) {
                println();
            }

            // Print the Record (since class file 58.65535 JEP 359)
            if (recordData != null) {
                recordData.setCommentOffset(getCommentOffset() - getIndentSize());
                recordData.print();
                if (isPrintable(permittedSubclassesData, preloadData, nestHost, nestMembers) ||
                        isPrintable(innerClasses) || (printCPIndex && isPrintable(bootstrapMethods))) {
                    println();
                }
            }

            // Print PermittedSubclasses Attribute (JEP 360 (Sealed types): class file 59.65535)
            if (permittedSubclassesData != null) {
                permittedSubclassesData.setCommentOffset(getCommentOffset() - getIndentSize());
                permittedSubclassesData.print();
                if (isPrintable(preloadData, nestHost, nestMembers) || isPrintable(innerClasses) ||
                        (printCPIndex && isPrintable(bootstrapMethods))) {
                    println();
                }
            }
            // Print the NestHost (since class file: 55.0)
            if (nestHost != null) {
                nestHost.setCommentOffset(getCommentOffset() - getIndentSize()); // 1 Indent
                nestHost.print();
                if (isPrintable(preloadData, nestMembers) || isPrintable(innerClasses) ||
                        (printCPIndex && isPrintable(bootstrapMethods))) {
                    println();
                }
            }
            // Print the NestMembers (since class file: 55.0)
            if (nestMembers != null) {
                nestMembers.setCommentOffset(getCommentOffset() - getIndentSize()); // 1 Indent
                nestMembers.print();
                if (isPrintable(preloadData) || isPrintable(innerClasses) || (printCPIndex && isPrintable(bootstrapMethods))) {
                    println();
                }
            }
            // Print the inner classes
            if (innerClasses != null && !innerClasses.isEmpty()) {
                for (InnerClassData icd : innerClasses) {
                    icd.setCommentOffset(getCommentOffset() - getIndentSize());
                    icd.print();
                }
                if (isPrintable(preloadData) || (printCPIndex && isPrintable(bootstrapMethods))) {
                    println();
                }
            }

            if (preloadData != null) {
                preloadData.setCommentOffset(getCommentOffset() - getIndentSize());
                preloadData.print();
                if (printCPIndex && isPrintable(bootstrapMethods)) {
                    println();
                }
            }

            // Print the BootstrapMethods
            //
            // Only print these if printing extended constants
            if (printCPIndex && isPrintable(bootstrapMethods)) {
                for (BootstrapMethodData bootstrapMethodData : bootstrapMethods) {
                    bootstrapMethodData.print();
                }
            }
            println(format("} // end Class %s%s",
                    name,
                    sourceName != null ? " compiled from \"" + sourceName + "\"" : ""));
        }
        List<IOException> issues = pool.getIssues();
        if (!issues.isEmpty()) {
            for (IOException ioe : issues) {
                environment.error(ioe);
            }
            throw new RuntimeException();
        }
    }

    /**
     * Prints list of either fields or methods
     *
     * @param list a list of fields or methods to be printed
     * @return true if something were printed
     * @throws IOException if something goes wrong
     */
    private boolean printMemberDataList(List<? extends MemberData> list, int commentOffset) throws IOException {
        if (list != null) {
            int count = list.size();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    MemberData md = list.get(i);
                    md.setCommentOffset(commentOffset);
                    if (i != 0 && md.getAnnotationsCount() > 0)
                        println();
                    md.print();
                }
                return true;
            }
        }
        return false;
    }

    // Utility methods to check the cottages of data would be printed
    boolean isPrintable(List<?>... lists) {
        for (List<?> list : lists)
            if (list != null && !list.isEmpty())
                return true;
        return false;
    }

    @SafeVarargs
    final <T extends Indenter> boolean isPrintable(T... list) {
        for (T data : list)
            if (data != null)
                return true;
        return false;
    }
}
