/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.DecompilerLogger;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.inputs.FileInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;
import org.openjdk.asmtools.jdis.notations.Type;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.structure.ClassFileContext.*;
import static org.openjdk.asmtools.common.structure.EModifier.ACC_ABSTRACT;
import static org.openjdk.asmtools.common.structure.EModifier.ACC_SUPER;
import static org.openjdk.asmtools.jasm.ClassFileConst.JAVA_MAGIC;
import static org.openjdk.asmtools.jdis.ClassData.COMPILATION_UNIT.MODULE_INFO;

/**
 * Central class data for of the Java Disassembler
 */
public class ClassData extends MemberData<ClassData> {

    // internal status of the class data
    private boolean alreadyPrinted = false;
    private boolean canBePrinted = false;           // Sufficient class info has been read and is now ready to be printed

    // Version info
    protected CFVersion cfVersion = new CFVersion();

    // Constant Pool index to this class
    protected int this_cpx;
    // Constant Pool index to this classes parent (super)
    protected int super_cpx;

    // Pre-initialized Constant Pool names calculated by this_cpx
    protected String className = "";
    protected String packageName = "";
    protected String classShortName = "";

    // The interfaces this class implements
    protected int[] interfaces;

    // The fields of this class
    protected Container<FieldData, ClassData> fields;

    // The methods of this class
    protected Container<MethodData, ClassData> methods;

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

    private int totalAttributes = 0;

    // The SourceFile Attribute (since 45.3)
    protected SourceFileData sourceFileData;

    // The inner-classes of this class (since 45.3)
    protected Container<InnerClassData, ClassData> innerClasses;

    // The EnclosingMethod Attribute (since 49.0)
    protected EnclosingMethodData enclosingMethodData;

    // The record attribute of this class (since class file 58.65535)
    protected RecordData recordData;

    // The bootstrap methods this class implements
    protected Container<BootstrapMethodData, ClassData> bootstrapMethods;

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
    protected LoadableDescriptorsData loadableDescriptorsData;

    // source file data
    private TextLines sourceLines = null;
    private Path classFile = null;

    public <E extends Environment<DecompilerLogger>> ClassData(E environment) {
        super(environment);
        memberType = "ClassData";
        super.environment = environment;
        environment.traceln("printOptions=" + Options.asShortString());
        super.pool = new ConstantPool(this);
        super.init(this);
    }

    public boolean isDetailedOutput() {
        return detailedOutput || extraDetailedOutput;
    }

    /**
     * Read and resolve the field data
     */
    protected void readFields(DataInputStream in) throws IOException {
        int nFields = in.readUnsignedShort();
        environment.traceln("fields=#" + nFields);
        fields = new Container<>(this, FieldData.class, nFields);
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
        methods = new Container<>(this, MethodData.class, nMethods);
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
        totalAttributes++;
        switch (attributeTag) {
            case ATT_Signature -> {
                if (signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "ClassFile");
                }
                signature = new SignatureData(this).read(in, attributeLength);
            }
            case ATT_SourceFile -> {
                // Read SourceFile attribute
                if (sourceFileData != null) {
                    environment.warning("warn.one.attribute.required", "SourceFile", "ClassFile");
                }
                sourceFileData = new SourceFileData(this).read(in, attributeLength);
            }
            case ATT_EnclosingMethod -> {
                // Read EnclosingMethod attribute
                if (enclosingMethodData != null) {
                    environment.warning("warn.one.attribute.required", "EnclosingMethod", "ClassFile");
                }
                enclosingMethodData = new EnclosingMethodData(this).read(in, attributeLength);
            }
            case ATT_SourceDebugExtension ->
                    sourceDebugExtensionData = new SourceDebugExtensionData(this).read(in, attributeLength);
            case ATT_InnerClasses -> {
                // Read InnerClasses Attr
                int count = in.readUnsignedShort();
                if (2 + count * 8 != attributeLength) {
                    if (bestEffort) {
                        environment.getLogger().error(
                                "err.invalid.attribute.length", "InnerClasses_attribute", attributeLength);
                    } else {
                        throw new FormatError(environment.getLogger(),
                                "err.invalid.attribute.length", "InnerClasses_attribute", attributeLength);
                    }
                }
                innerClasses = new Container<>(this, InnerClassData.class, count).
                        setHasSize(!isTableOutput() && !skipComments && !tableFormat);
                innerClasses.setCommentOffset(this.getCommentOffset());
                for (int j = 0; j < count; j++) {
                    InnerClassData innerClass = new InnerClassData(this, innerClasses);
                    innerClass.read(in);
                    innerClasses.add(innerClass);
                }
            }
            case ATT_BootstrapMethods -> {
                // Read BootstrapMethods Attr
                int count = in.readUnsignedShort();
                bootstrapMethods = new Container<>(this, BootstrapMethodData.class, count);
                bootstrapMethods.setCommentOffset(this.getCommentOffset());
                for (int j = 0; j < count; j++) {
                    BootstrapMethodData bsmData = new BootstrapMethodData(this, bootstrapMethods);
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
            case ATT_LoadableDescriptors ->
                // Valhalla
                    loadableDescriptorsData = new LoadableDescriptorsData(this).read(in, attributeLength);
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
                environment.warning("warn.wrong.magic", HexUtils.toHex(JAVA_MAGIC), HexUtils.toHex(magic));
            }
            cfVersion.setMinorVersion(in.readUnsignedShort());
            cfVersion.setMajorVersion(in.readUnsignedShort());

            // Read the constant pool
            pool.read(in).InitializePrintData();

            access = in.readUnsignedShort();
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
            // Final initialization based on just read class data.
            if (cfVersion.isValueObjectContext()) {
                EModifier.setGlobalContext(VALUE_OBJECTS);
            }

        } catch (EOFException eofException) {
            environment.error("err.eof");
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
            sourceFileData.getName();
        }
    }

    /**
     * Determines if this Class has a package
     *
     * @return true if the package exists for this class
     */
    public boolean hasPackage() {
        return !this.packageName.isEmpty();
    }

    public String getClassName() {
        return className;
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

    @Override
    protected <T extends AnnotationData> void printAnnotations(List<T>... annotationLists) throws IOException {
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

    /**
     * Prints the JASM file as much as it was read if exceptions occur during reading.
     */
    public void postPrint() {
        if (!alreadyPrinted && canBePrinted) {
            try {
                this.print();
            } catch (Exception ignored) {
            }
        }
        try {
            environment.getToolOutput().finishClass(className);
        } catch (IOException ignored) {
        }
        environment.getOutputs().flush();
    }

    /**
     * Print jasm file in simple/table format.
     */
    @Override
    public void print() throws IOException {
        if (COMPILATION_UNIT.get(className, access) == MODULE_INFO) {           // module-info compilation unit
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
            canBePrinted = true;
            // Print module attributes
            moduleData.print();
            //
            print(format("} // end of module %s", moduleData.getModuleName()));
            if (moduleData.getModuleVersion() != null)
                print("@" + moduleData.getModuleVersion());
            println();

            // Print the Annotations
            printAnnotations(visibleAnnotations, invisibleAnnotations);
            printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
            if (hasPackage()) {
                println(String.format("package %s version %s;", packageName, cfVersion.asString()));
            }
        } else {                                                                // class/interface compilation unit
            // Print either extended(table) or Jasm-formatted Annotations, Header, and ConstantPool.
            printClassIntroduction();
            canBePrinted = true;
            // Load source file info.
            if (printSourceLines && (sourceFileData != null)) {
                String sourceName = sourceFileData.calculateName();
                if (sourceName != null) {
                    sourceLines = new TextLines(classFile.getParent(), sourceName);
                }
            }

            // get the list of attributes that would be printed. it might be empty.
            final List<? extends Printable> printableAttributes = getListOfPrintableAttributes(
                    signature,
                    sourceFileData,
                    enclosingMethodData,
                    sourceDebugExtensionData,
                    recordData,                     // Print the Record (since class file 58.65535 JEP 359)
                    nestHost,                       // Print the NestHost (since class file: 55.0)
                    innerClasses,
                    nestMembers,                    // Print the NestMembers (since class file: 55.0)
                    permittedSubclassesData,        // Print PermittedSubclasses Attribute (JEP 360 (Sealed types): class file 59.65535)
                    loadableDescriptorsData,
                    bootstrapMethods
            );

            int commentOffset = getCommentOffset();

            // Print the fields
            if (printMemberDataList(fields, commentOffset) && isPrintable(methods) && !printableAttributes.isEmpty()) {
                println();
            }

            setCommentOffset(commentOffset -= getIndentSize());

            // Print the methods
            if (printMemberDataList(methods, commentOffset) && !printableAttributes.isEmpty()) {
                println();
            }

            // Print the attributes
            printAttributes(printableAttributes, commentOffset);

            if (skipComments) {
                println("}");
            } else {
                println(format("} // end Class %s%s",
                        className,
                        sourceFileData != null ? " compiled from \"" + sourceFileData.calculateName() + "\"" : ""));
            }
            alreadyPrinted = true;
        }

        List<IOException> issues = pool.getIssues();
        if (!issues.isEmpty()) {
            for (IOException ioe : issues) {
                environment.error(ioe);
            }
            throw new SyntaxError();
        }
    }

    protected void printClassIntroduction() throws IOException {
        printSysInfo();
        if (hasPackage()) {
            println(format("package %s;%n", packageName));
        }
        // Print class annotations
        printAnnotations(visibleAnnotations, invisibleAnnotations);
        printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
        // Print ClassDeclaration
        printJasmClassDeclaration();
        println("{");
        int thisCommentOffset = pool.getCommentOffset();
        // Print the constant pool
        if (printConstantPool) {
            pool.print();
            setCommentOffset(thisCommentOffset);
        }
        // Print (this|super)_class
        if (extraDetailedOutput && !dropClasses) {
            if (printCPIndex) {
                if (skipComments) {
                    printIndentLn("this_class:  #%d;".formatted(this_cpx), thisCommentOffset);
                    printIndentLn("super_class: #%d;".formatted(super_cpx), thisCommentOffset);
                } else {
                    thisCommentOffset -= getIndentSize();
                    printIndent(PadRight("this_class:  #%d;".formatted(this_cpx), thisCommentOffset)).
                            println(" // " + pool.getClassName(this_cpx, cpx -> "invalid index into the constant_pool table"));
                    printIndent(PadRight("super_class: #%d;".formatted(super_cpx), thisCommentOffset)).
                            println(" // " + pool.getClassName(super_cpx, cpx -> "invalid index into the constant_pool table"));
                }
            } else {
                printIndentLn("this_class:  %s;".
                        formatted(pool.getClassName(this_cpx, cpx -> "invalid index into the constant_pool table")));
                printIndentLn("super_class: %s;".
                        formatted(pool.getClassName(super_cpx, cpx -> "invalid index into the constant_pool table")));
            }
            println();
        }
    }

    @Override
    protected void printSysInfo() {
        if (sysInfo) {
            ToolInput toolInput = environment.getToolInput();
            String thisClassName = pool.getJavaClassName(this_cpx, "<invalid this_cpx #%d>");
            boolean isClass = !EModifier.isInterface(access);
            Date lm;
            String prefix = " *  ";
            println("/**");
            if (toolInput instanceof FileInput) {
                println(prefix + "Classfile " + classFile.toAbsolutePath());
                lm = new Date(classFile.toFile().lastModified());
            } else {
                String name = classFile.toString();
                println(prefix + "Classfile " + name.substring(name.lastIndexOf('/') + 1));
                lm = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            DateFormat df = DateFormat.getDateInstance();
            int length = toolInput.getSize();
            if (length > 0) {
                println(prefix + INDENT_STRING + "Last modified %s; size %d bytes", df.format(lm), length);
            } else {
                println(prefix + INDENT_STRING + "Last modified %s", df.format(lm));
            }
            MessageDigest msd = toolInput.getMessageDigest();
            if (msd != null) {
                byte[] digest = msd.digest();
                if (digest != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digest)
                        sb.append(String.format("%02x", b));
                    println(prefix + INDENT_STRING + msd.getAlgorithm() + " checksum " + sb);
                }
            }
            if (sourceFileData != null) {
                println(prefix + INDENT_STRING + "Compiled from \"%s\"".formatted(sourceFileData.calculateName()));
            }
            // Print java descriptor of a class/interface
            if (isClass) {
                print(prefix + EModifier.asKeywords(access & ~ACC_SUPER.getFlag(), CLASS) +
                        "class " + thisClassName);
            } else {
                print(prefix + EModifier.asKeywords(access & ~ACC_ABSTRACT.getFlag() & ~ACC_SUPER.getFlag(), CLASS) +
                        "interface " + thisClassName);
            }
            if (signature == null) {
                // use info from class file header
                if (isClass && super_cpx != 0) {
                    String superClassName = pool.getJavaClassName(this_cpx, "<invalid super_cpx #%d>");
                    if (!superClassName.equals("java.lang.Object")) {
                        print(" extends " + superClassName);
                    }
                }
                for (int i = 0; i < interfaces.length; i++) {
                    print(i == 0 ? (isClass ? " implements " : " extends ") : ",");
                    print(pool.getJavaClassName(interfaces[i], "<invalid interface_cpx #%d>"));
                }
                println();
            } else {
                Type signType = signature.getSignatureType();
                String sign = signature.getJavaSignature();
                if (signType instanceof Type.ClassSigType)
                    print(sign);
                else if (!signType.isObject()) {
                    print(" extends " + sign);
                }
                println();
            }
            println(prefix + INDENT_STRING + "minor version: " + cfVersion.minor_version());
            println(prefix + INDENT_STRING + "major version: " + cfVersion.major_version());
            println(prefix + INDENT_STRING + "flags: (0x%04x) %s".formatted(access, EModifier.asNames(access, CLASS)));
            println(prefix + INDENT_STRING + "this_class:  %s".formatted(pool.getClassName(this_cpx, cpx -> "invalid index into the constant_pool table")));
            println(prefix + INDENT_STRING + "super_class: %s".formatted(pool.getClassName(super_cpx, cpx -> "invalid index into the constant_pool table")));

            println(prefix + INDENT_STRING + "interfaces: %d, fields: %d, methods: %d, attributes: %d",
                    interfaces.length, fields.size(), methods.size(), totalAttributes);
            println(" */");
        }
    }

    protected void printJasmClassDeclaration() {
        {
            String shortClassName = pool.inRange(this_cpx) ? pool.getShortClassName(this_cpx, this.packageName) : "?? invalid index";
            // In Java SE 8, the ACC_SUPER semantics became mandatory, regardless of the setting of ACC_SUPER or
            // the class file version number, and the flag no longer had any effect.
            // however, to not print it where it was, would cause hotswap of such class to
            // throw java.lang.UnsupportedOperationException: class redefinition failed: attempted to change the class modifiers
            Pair<String, String> signInfo = (signature != null) ?
                    signature.getJasmPrintInfo((i) -> pool.inRange(i)) :
                    new Pair<>("", "");
            // An interface is distinguished by the ACC_INTERFACE flag being set.
            if (EModifier.isInterface(access)) {       // interface compilation unit
                print(EModifier.asKeywords(access & ~ACC_ABSTRACT.getFlag(), CLASS));
                print(printCPIndex ?
                        (skipComments ?
                                format("interface #%d%s", this_cpx, signInfo.first) :
                                format("interface #%d%s /* %s%s */", this_cpx, signInfo.first, shortClassName, signInfo.second)
                        ) :
                        format("interface %s", shortClassName, signInfo.second)
                );
            } else {                                    // class compilation unit
                // add synthetic, deprecated if necessary
                print(EModifier.asKeywords(access, CLASS) + getPseudoFlagsAsString());
                print(printCPIndex ?
                        (skipComments ?
                                format("class #%d%s", this_cpx, signInfo.first) :
                                format("class #%d%s /* %s%s */", this_cpx, signInfo.first, shortClassName, signInfo.second)
                        ) :
                        format("class %s%s", shortClassName, signInfo.second)
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
                    if (skipComments) {
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
        }
    }

    private <P extends Printable> List<P> getListOfPrintableAttributes(P... attributes) {
        return Arrays.stream(attributes).filter(a -> isPrintable(a)).toList();
    }

    // Utility methods to check whether cottages of data would be printed
    private <P extends Printable> boolean isPrintable(P... attributes) {
        return Arrays.stream(attributes).
                anyMatch(attribute -> attribute != null && attribute.isPrintable());
    }

    private <P extends Printable> void printAttributes(List<P> attributeList, int commentOffset) throws
            IOException {
        int len = attributeList.size();
        boolean printed = false;
        for (int i = 0; i < len; i++) {
            P attribute = attributeList.get(i);
            if (isPrintable(attribute)) {
                if (Indenter.class.isAssignableFrom(attribute.getClass())) {
                    ((Indenter) attribute).setCommentOffset(commentOffset);
                }
                attribute.print();
                printed = true;
            }
            if (printed && (i + 1 < len)) {
                println();
                printed = false;
            }
        }
        // TODO: Prints corrupted attributes if any.
    }

    private boolean printMemberDataList(Container<? extends MemberData<ClassData>, ClassData> list,
                                        int commentOffset) throws IOException {
        if (list != null && list.size() > 0) {
            list.setCommentOffset(commentOffset).print();
            return true;
        }
        return false;
    }

    public enum COMPILATION_UNIT {
        MODULE_INFO,
        PACKAGE_INFO,
        CLASS_UNIT;

        /**
         * @param className Pre-initialized Constant Pool name calculated by this_cpx
         * @param access    access flags (modifiers)
         * @return compilation unit
         */
        public static COMPILATION_UNIT get(String className, int access) {
            if (className.endsWith("module-info") || EModifier.isModule(access)) {              // module-info compilation unit
                return MODULE_INFO;
            } else if (className.endsWith("package-info") && EModifier.isInterface(access)) {  // package-info compilation unit
                return PACKAGE_INFO;
            } else {
                return CLASS_UNIT;
            }
        }
    }
}
