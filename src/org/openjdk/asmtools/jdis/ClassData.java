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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.structure.ClassFileContext.CLASS;
import static org.openjdk.asmtools.common.structure.ClassFileContext.MODULE;
import static org.openjdk.asmtools.common.structure.EModifier.ACC_ABSTRACT;
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

    // Constant Pool index to this classes parent (super)
    protected int super_cpx;

    // -----------------------------------------------------------------------------------------------
    // Interfaces, Fields, Methods && Attributes
    // -----------------------------------------------------------------------------------------------
    // The interfaces this class implements
    protected int[] interfaces;

    // The fields of this class
    protected Container<FieldData> fields;

    // The methods of this class
    protected Container<MethodData> methods;

    /**
     * Attributes:
     * SourceFile                                   45.3
     * InnerClasses                                 45.3
     * EnclosingMethod                              49.0
     * SourceDebugExtension                         49.0
     * BootstrapMethods                             51.0
     * Module,   ModulePackages, ModuleMainClass    53.0
     * NestHost, NestMembers                        55.0
     * Record                                       60.0
     * PermittedSubclasses                          61.0
     * -------------------------------------------------
     * Synthetic                                    45.3
     * Deprecated                                   45.3
     * Signature                                    49.0
     * RuntimeVisibleAnnotations                    49.0
     * RuntimeInvisibleAnnotations                  49.0
     * RuntimeVisibleTypeAnnotations                52.0
     * RuntimeInvisibleTypeAnnotations              52.0
     */

    // The SourceFile Attribute
    protected SourceFileData sourceFileData;

    // The inner-classes of this class
    protected Container<InnerClassData> innerClasses;

    // The record attribute of this class (since class file 58.65535)
    protected RecordData recordData;


    // The bootstrap methods this class implements
    protected Container<BootstrapMethodData> bootstrapMethods;

    //The module this class file presents
    protected ModuleData moduleData;

    // The NestHost of this class (since class file: 55.0)
    protected NestHostData nestHost;

    // The NestMembers of this class (since class file: 55.0)
    protected NestMembersData nestMembers;

    // The PermittedSubclasses of this class (JEP 360 (Sealed types): class file 59.65535)
    protected PermittedSubclassesData permittedSubclassesData;

    protected SourceDebugExtensionData sourceDebugExtensionData;

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
            read(dis, inputFile.toPath());
        }
    }

    public void read(String inputFileName) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputFileName))) {
            read(dis, Paths.get(inputFileName));
        }
    }

    /**
     * Read and resolve the field data
     */
    protected void readFields(DataInputStream in) throws IOException {
        int nFields = in.readUnsignedShort();
        environment.traceln("fields=#" + nFields);
        fields = new Container<>(nFields);
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
        methods = new Container<>(nMethods);
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
                    throw new FormatError(environment.getLogger(),
                            "err.invalid.attribute.length", "SourceFile_attribute", attributeLength);
                }
                if (sourceFileData != null) {
                    environment.warning("warn.one.attribute.required", "SourceFile", "ClassFile");
                }
                sourceFileData = new SourceFileData(this).read(in, attributeLength);
            }
            case ATT_SourceDebugExtension -> {
                sourceDebugExtensionData = new SourceDebugExtensionData(this).read(in, attributeLength);
            }
            case ATT_InnerClasses -> {
                // Read InnerClasses Attr
                int count = in.readUnsignedShort();
                if (2 + count * 8 != attributeLength) {
                    throw new FormatError(environment.getLogger(),
                            "err.invalid.attribute.length", "InnerClasses_attribute", attributeLength);
                }
                innerClasses = new Container<>(count);
                for (int j = 0; j < count; j++) {
                    InnerClassData innerClass = new InnerClassData(this);
                    innerClass.read(in);
                    innerClasses.add(innerClass);
                }
            }
            case ATT_BootstrapMethods -> {
                // Read BootstrapMethods Attr
                int count = in.readUnsignedShort();
                bootstrapMethods = new Container<BootstrapMethodData>(count).setPrintable(printCPIndex);
                for (int j = 0; j < count; j++) {
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
    public void read(final DataInputStream in, final Path src) throws IOException {
        classFile = src;
        // Read the header
        try {
            int magic = in.readInt();

            if (magic != JAVA_MAGIC) {
                throw new ClassCastException("wrong magic: " + HexUtils.toHex(magic) + ", expected " + HexUtils.toHex(JAVA_MAGIC));
            }

            cfVersion.setMinorVersion( in.readUnsignedShort());
            cfVersion.setMajorVersion( in.readUnsignedShort());

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
            throw new FormatError(environment.getLogger(), "err.eof");
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
        if (sourceFileData != null) {
            sourceFileData.setSourceName();
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
        // Number of read corrupted attributes if any
        int numCorruptedAttributes = 0;
        if (className.endsWith("module-info") || EModifier.isModule(access)) {          // module-info compilation unit
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
            // however, to not print it where it was, would cause hotswap of such class to
            // throw java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the class modifiers

            String name = pool.inRange(this_cpx) ? pool.getShortClassName(this_cpx, this.packageName) : "?? invalid index";
            Pair<String, String> signInfo = (signature != null) ?
                    signature.getPrintInfo((i) -> pool.inRange(i)) :
                    new Pair<>("", "");
            // An interface is distinguished by the ACC_INTERFACE flag being set.
            if (EModifier.isInterface(access)) {       // interface compilation unit
                print(EModifier.asKeywords(access & ~ACC_ABSTRACT.getFlag(), CLASS));
                print(printCPIndex ?
                        (skipComments ?
                                format("interface #%d%s", this_cpx, signInfo.first) :
                                format("interface #%d%s /* %s%s */", this_cpx, signInfo.first, name, signInfo.second)
                        ) :
                        format("interface %s", name, signInfo.second)
                );
            } else {                                    // class compilation unit
                // add synthetic, deprecated if necessary
                print(EModifier.asKeywords(access, CLASS) + getPseudoFlagsAsString());
                print(printCPIndex ?
                        (skipComments ?
                                format("class #%d%s", this_cpx, signInfo.first) :
                                format("class #%d%s /* %s%s */", this_cpx, signInfo.first, name, signInfo.second)
                        ) :
                        format("class %s%s", name, signInfo.second)
                );
                // if base class is not j.l.Object prints the "extends" statement
                if (this_cpx < pool.size() && this_cpx > 0) {
                    if (!pool.getClassName(super_cpx).equals("java/lang/Object")) {
                        print(printCPIndex ?
                                (skipComments ?
                                        format(" extends #%d", super_cpx) :
                                        format(" extends #%d /* %s */", super_cpx, pool.getShortClassName(super_cpx, this.packageName))
                                ) :
                                format(" extends %s", pool.getShortClassName(super_cpx, this.packageName))
                        );
                    }
                } else {
                    print(printCPIndex ?
                            (skipComments ?
                                    format(" extends #%d", super_cpx) :
                                    format(" extends #%d /* ?? invalid index */", super_cpx)
                            ) : " extends ??");
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
                    if( skipComments ) {
                        statement = format("%simplements %s", (numInterfaces > 1) ? "\n" + getIndentString() : " ", sIndexes);
                    } else {
                        statement = format("%simplements %s /* %s */", (numInterfaces > 1) ? "\n" + getIndentString() : " ", sIndexes, sNames);
                    }
                } else {
                    statement = format("%simplements %s", (numInterfaces > 1) ? "\n" + getIndentString() : " ", sNames);
                }
                print(statement);
            }
            println("%sversion %s", (numInterfaces > 1) ? "\n" + getIndentString() : " ", cfVersion.asString());
            println("{");
            if (printSourceLines && (sourceFileData != null)) {
                String sourceName = sourceFileData.getSourceName();
                if (sourceName != null) {
                    sourceLines = new TextLines(classFile.getParent(), sourceName);
                }
            }

            // Print the constant pool
            if (printConstantPool) {
                pool.print();
                setCommentOffset(pool.getCommentOffset());
            }

            // get the list of attributes that would be printed. it might be empty.
            final List<? extends Printable> printableAttributes = getListOfPrintableAttributes(
                    sourceFileData,
                    recordData,                     // Print the Record (since class file 58.65535 JEP 359)
                    permittedSubclassesData,        // Print PermittedSubclasses Attribute (JEP 360 (Sealed types): class file 59.65535)
                    nestHost,                       // Print the NestHost (since class file: 55.0)
                    nestMembers,                    // Print the NestMembers (since class file: 55.0)
                    innerClasses,
                    preloadData,
                    bootstrapMethods,
                    sourceDebugExtensionData
            );

            // Print the fields
            if (printMemberDataList(fields, getCommentOffset()) && isPrintable(methods) && !printableAttributes.isEmpty()) {
                println();
            }

            // Print the methods
            if (printMemberDataList(methods, getCommentOffset() - getIndentSize()) && !printableAttributes.isEmpty()) {
                println();
            }

            // Print the attributes
            numCorruptedAttributes = printAttributes(getCommentOffset() - getIndentSize(), printableAttributes);
            if( skipComments ) {
                println("}");
            } else {
                println(format("} // end Class %s%s",
                        name,
                        sourceFileData != null ? " compiled from \"" + sourceFileData.getSourceName() + "\"" : ""));
            }
        }

        // TODO: This isn't necessary. The warning info is already inlined into the jasm code.
        // TODO: Or the "-nowarn" option should be added.
        // if( numCorruptedAttributes > 0 ) {
        //      environment.warning("warn.corrupted_attributes", numCorruptedAttributes);
        // }

        List<IOException> issues = pool.getIssues();
        if (!issues.isEmpty()) {
            for (IOException ioe : issues) {
                environment.error(ioe);
            }
            throw new RuntimeException();
        }
    }

    private <P extends Printable> List<P> getListOfPrintableAttributes(P... attributes) {
        return Arrays.stream(attributes).filter(a -> isPrintable(a)).toList();
    }

    // Utility methods to check whether cottages of data would be printed
    private <P extends Printable> boolean isPrintable(P... attributes) {
        for (P attribute : attributes)
            if (attribute != null) {
                if (attribute.isPrintable()) {
                    if (attribute instanceof Container<?>) {
                        Container<P> container = (Container<P>) attribute;
                        for (P item : container) {
                            if (item.isPrintable())
                                return true;
                        }
                        return false;
                    }
                    return true;
                }
            }
        return false;
    }

    /**
     * Returns number of corrupted attributes if any
     */
    private <P extends Printable> int printAttributes(int commentOffset, List<P> attributeList) throws
            IOException {
        int len = attributeList.size();
        boolean printed = false;
        for (int i = 0; i < len; i++) {
            P attribute = attributeList.get(i);
            if (isPrintable(attribute)) {
                if (Container.class.isAssignableFrom(attribute.getClass())) {
                    for (P item : (Container<P>) attribute) {
                        if (Indenter.class.isAssignableFrom(item.getClass())) {
                            ((Indenter) item).setCommentOffset(commentOffset);
                        }
                        item.print();
                    }
                } else {
                    if (Indenter.class.isAssignableFrom(attribute.getClass())) {
                        ((Indenter) attribute).setCommentOffset(commentOffset);
                    }
                    attribute.print();
                }
                printed = true;
            }
            if (printed && (i + 1 < len)) {
                println();
                printed = false;
            }
        }
        // Prints corrupted attributes if any.
        List<AttrData> corruptedList = getCorruptedAttributes();
        if (!corruptedList.isEmpty()) {
            printIndentLn();
            printIndentLn(format("// == Ignored %d corrupted attribute(s): ==", corruptedList.size()));
            for (int i = 0; i < corruptedList.size(); i++) {
                printIndentLn("// attribute_info {");
                printIndentLn(format("//    u2 attribute_name_index: #%d;", corruptedList.get(i).getNameCpx()));
                printIndentLn(format("//    u4 attribute_length:     %d;", corruptedList.get(i).getLength()));
                printIndentLn("//    u1 info[attribute_length];");
                printIndentLn("// }");
            }
            printIndentLn();
        }
        return corruptedList.size();
    }

    private List<AttrData> getCorruptedAttributes() {
        return attributes.stream().filter(AttrData::isCorrupted).toList();
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
}
