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
package org.openjdk.asmtools.jasm;

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.*;

import static org.openjdk.asmtools.common.structure.ClassFileContext.INNER_CLASS;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_DYNAMIC;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_INVOKEDYNAMIC;
import static org.openjdk.asmtools.jasm.ClassFileConst.JAVA_MAGIC;

/**
 * ClassData
 * <p>
 * This is the main data structure for representing parsed class data. This structure
 * renders directly to a class file.
 */
class ClassData extends MemberData<JasmEnvironment> {

    private static final String DEFAULT_EXTENSION = ".class";
    /* ClassData Fields */
    private final JasmEnvironment environment;
    public CDOutputStream cdos;
    String fileExtension = DEFAULT_EXTENSION;
    MethodData curMethod;
    CFVersion cfv;
    ConstCell this_class, super_class;
    String myClassName;
    AttrData sourceFileNameAttr;
    ArrayList<Indexer> interfaces;
    ArrayList<FieldData> fields = new ArrayList<>();
    ArrayList<MethodData> methods = new ArrayList<>();
    DataVectorAttr<InnerClassData> innerClasses = null;
    DataVectorAttr<BootstrapMethodData> bootstrapMethodsAttr = null;
    // JEP 181 - NestHost, NestMembers attributes since class version 55.0
    CPXAttr nestHostAttr;
    NestMembersAttr nestMembersAttr;
    // JEP 261: Module System since class file 53.0
    ModuleAttr moduleAttribute = null;
    // JEP 359 - Record attribute since class file 58.65535
    private RecordData recordData;
    // JEP 360 - PermittedSubclasses attribute since class file 59.65535
    private PermittedSubclassesAttr permittedSubclassesAttr;
    // Valhalla
    private PreloadAttr preloadAttr;

    /**
     * @param environment The error reporting environment.
     * @param cfv         Class file version
     */
    public ClassData(JasmEnvironment environment, CFVersion cfv) {
        super(new ConstantPool(environment), environment);  // for a class, these get initialized in the super - later.
        this.environment = environment;
        this.cdos = new CDOutputStream();
        this.cfv = cfv;
    }

    /**
     * Initializes the ClassData.
     *
     * @param this_class  The constant pool reference to this class
     * @param super_class The constant pool reference to the super class
     * @param interfaces  A list of interfaces that this class implements
     */
    public final void init(int access, ConstCell this_class, ConstCell super_class, ArrayList<Indexer> interfaces) {
        this.access = access;
        // normalize the modifiers to access flags
        if (EModifier.hasPseudoMod(access)) {
            createPseudoMod();
        }
        this.this_class = this_class;
        this.super_class = super_class;
        this.interfaces = interfaces;
        // Set default class file version if it is not set.
        this.cfv.initClassDefaultVersion();
    }

    public final void initAsPackageInfo(int access, String className) {
        this.access = access;
        // this_class: class "package_name/package-info"
        this.myClassName = className;
        this.cfv.initClassDefaultVersion();
    }

    public final void initAsModule() {
        this.access = EModifier.ACC_MODULE.getFlag();
        // If the ACC_MODULE flag is set in the access_flags item
        // super_class: zero
        this.super_class = new ConstCell(0);
        this.cfv.initModuleDefaultVersion();
    }

    /**
     * Predicate that describes if this class has an access flag indicating that it is an
     * interface.
     *
     * @return True if the classes access flag indicates it is an interface.
     */
    public final boolean isInterface() {
        return EModifier.isInterface(access);
    }

    /**
     * Predicate that describes if this class has a primitive flag indicating that it is the primitive class.
     *
     * @return True if the classes access flag indicates it is the primitive class.
     */
    public final boolean isPrimitive() {
        return EModifier.isPrimitive(access);
    }

    /**
     * Predicate that describes if this class has an abstract flag indicating that it is the abstract class.
     *
     * @return True if the classes access flag indicates it is the abstract class.
     */
    public final boolean isAbstract() {
        return EModifier.isAbstract(access);
    }

    /*
     * After a constant pool has been explicitly declared,
     * this method links the Constant_InvokeDynamic Constant_Dynamic
     * constants with any bootstrap methods that they index in the
     * Bootstrap Methods Attribute
     */
    protected void relinkBootstrapMethods() {
        if (bootstrapMethodsAttr == null) {
            return;
        }

        environment.traceln("relinkBootstrapMethods");

        for (ConstCell cell : pool) {
            ConstValue ref = null;
            if (cell != null) {
                ref = cell.ref;
            }
            if (ref != null
                    && (ref.tag == CONSTANT_INVOKEDYNAMIC || ref.tag == CONSTANT_DYNAMIC)) {
                // Find only the Constant
                ConstantPool.ConstValue_BootstrapMethod refval = (ConstantPool.ConstValue_BootstrapMethod) ref;
                BootstrapMethodData bsmdata = refval.bsmData();
                // only care about BSM Data that were placeholders
                if (bsmdata != null && bsmdata.isPlaceholder()) {
                    // find the real BSM Data at the index
                    int bsmindex = bsmdata.cpIndex;
                    if (bsmindex < 0 || bsmindex > bootstrapMethodsAttr.size()) {
                        // bad BSM index --
                        // give a warning, but place the index in the arg anyway
                        environment.traceln("Warning: (ClassData.relinkBootstrapMethods()): Bad bootstrapMethods index: " + bsmindex);
                        // env.error("const.bsmindex", bsmindex);
                        bsmdata.cpIndex = bsmindex;
                    } else {
                        // make the IndyPairs BSM Data point to the one from the attribute
                        refval.setBsmData(bootstrapMethodsAttr.get(bsmindex));
                    }
                }
            }
        }
    }

    protected void numberBootstrapMethods() {
        environment.traceln("Numbering Bootstrap Methods");
        if (bootstrapMethodsAttr == null) {
            return;
        }
        boolean duplicateExists = false;
        // remove duplicates in BootstrapMethod_Attribute if found
        // Fix 7902888: Excess entries in BootstrapMethods with the same bsm, bsmKind, bsmArgs
        ArrayList<ConstCell<?>> list = this.getPool().getPoolCellsByType(CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC);
        BootstrapMethodData[] bsmAttributes = new BootstrapMethodData[list.size()];
        HashMap<BootstrapMethodData, Integer> bsmHashMap = new HashMap<>();
        int index = 0;
        //
        for (int i = 0; i < list.size(); i++) {
            ConstantPool.ConstValue_BootstrapMethod cell = (ConstantPool.ConstValue_BootstrapMethod) list.get(i).ref;
            BootstrapMethodData bsmData = ((ConstantPool.ConstValue_BootstrapMethod) list.get(i).ref).bsmData();
            if (bsmHashMap.keySet().contains(bsmData)) {
                duplicateExists = true;
                cell.setBsmData(bsmAttributes[bsmHashMap.get(bsmData)]);
            } else {
                bsmAttributes[i] = bsmData.clone(index++);
                cell.setBsmData(bsmAttributes[i]);
                bsmHashMap.put(bsmData, i);
            }
        }
        if( duplicateExists ) {
            bootstrapMethodsAttr.replaceAll(Arrays.stream(bsmAttributes).filter(i -> i != null).toList());
        }
    }

    // API
    // Record
    public RecordData setRecord(int where) {
        if (recordAttributeExists()) {
            environment.warning(where, "warn.record.repeated");
        }
        this.recordData = new RecordData(this);
        return this.recordData;
    }

    /**
     * Rejects a record: removes the record attribute if there are no components
     */
    public void rejectRecord() {
        this.recordData = null;
    }

    // Field
    public ConstantPool.ConstValue_FieldRef makeFieldRef(ConstCell name, ConstCell descriptor) {
        return new ConstantPool.ConstValue_FieldRef(name, descriptor);
    }

    public FieldData addFieldIfAbsent(int access, ConstCell name, ConstCell descriptor) {
        ConstantPool.ConstValue_FieldRef fieldRef = makeFieldRef(name, descriptor);
        environment.traceln(" [ClassData.addFieldIfAbsent]:  #" +
                fieldRef.value.first.cpIndex + ":#" +
                fieldRef.value.second.cpIndex);
        FieldData fd = getField(fieldRef);
        if (fd == null) {
            environment.traceln(" [ClassData.addFieldIfAbsent]:  new field.");
            fd = addField(access, fieldRef);
        }
        return fd;
    }

    private FieldData getField(ConstantPool.ConstValue_FieldRef nameAndType) {
        for (FieldData fd : fields) {
            if (fd.getNameDesc().equals(nameAndType)) {
                return fd;
            }
        }
        return null;
    }

    public FieldData addField(int access, ConstantPool.ConstValue_FieldRef fieldRef) {
        environment.traceln(" [ClassData.addField]:  #" +
                fieldRef.value.first.cpIndex + ":#" +
                fieldRef.value.second.cpIndex);
        FieldData res = new FieldData(this, access, fieldRef);
        fields.add(res);
        return res;
    }

    public FieldData addField(int access, ConstCell name, ConstCell sig) {
        return addField(access, makeFieldRef(name, sig));
    }

    public MethodData StartMethod(int access, ConstCell name, ConstCell sig, ArrayList exc_table) {
        EndMethod();
        environment.traceln(" [ClassData.StartMethod]:  #" + name.cpIndex + ":#" + sig.cpIndex);
        curMethod = new MethodData(this, access, name, sig, exc_table);
        methods.add(curMethod);
        return curMethod;
    }

    public void EndMethod() {
        curMethod = null;
    }

    public ConstCell LocalMethodRef(ConstValue nape) {
        return pool.findCell(ConstType.CONSTANT_METHODREF, this_class, pool.findCell(nape));
    }

    public ConstCell LocalMethodRef(ConstCell name, ConstCell sig) {
        return LocalMethodRef(makeFieldRef(name, sig));
    }

    void addLocVarData(int opc, Indexer arg) {
    }

    public void addInnerClass(int access, ConstCell name, ConstCell innerClass, ConstCell outerClass) {
        environment.traceln("addInnerClass (with indexes: Name (" + name.toString() + "), Inner (" + innerClass.toString() + "), Outer (" + outerClass.toString() + ").");
        if (innerClasses == null) {
            innerClasses = new DataVectorAttr<>(pool, EAttribute.ATT_InnerClasses);
        }
        innerClasses.add(new InnerClassData(access, name, innerClass, outerClass));
    }

    public void addBootstrapMethod(BootstrapMethodData bsmData) {
        environment.traceln("addBootstrapMethod");
        if (bootstrapMethodsAttr == null) {
            bootstrapMethodsAttr = new DataVectorAttr<>(pool, EAttribute.ATT_BootstrapMethods);
        }
        bootstrapMethodsAttr.add(bsmData);
    }

    public void addNestHost(ConstCell hostClass) {
        environment.traceln("addNestHost");
        nestHostAttr = new CPXAttr(pool, EAttribute.ATT_NestHost, hostClass);
    }

    public void addNestMembers(List<ConstCell> classes) {
        environment.traceln("addNestMembers");
        nestMembersAttr = new NestMembersAttr(pool, classes);
    }

    public void addPermittedSubclasses(List<ConstCell> classes) {
        environment.traceln("addPermittedSubclasses");
        permittedSubclassesAttr = new PermittedSubclassesAttr(pool, classes);
    }

    public void addPreloads(List<ConstCell> classes) {
        environment.traceln("addPreloads");
        preloadAttr = new PreloadAttr(pool, classes);
    }

    public void endClass() {
        if (super_class == null) {
            super_class = pool.findClassCell("java/lang/Object");
        }
        pool.itemizePool();
        super_class = pool.specifyCell(super_class);
        this_class = pool.specifyCell(this_class);
        pool.checkGlobals();
        pool.fixIndexesInPool();
        itemizeAnnotationAttributes(annotAttrInv, annotAttrVis);
        numberBootstrapMethods();
        try {
            ConstantPool.ConstValue_Class this_class_value = (ConstantPool.ConstValue_Class) this_class.ref;
            ConstantPool.ConstValue_UTF8 this_class_name = this_class_value.value.ref;
            myClassName = this_class_name.value;
            environment.traceln("this_class  = " + this_class);
            environment.traceln("super_class = " + super_class);
            environment.traceln("-- Constant Pool ---");
            environment.traceln("--------------------");
            pool.printPool();
            environment.traceln("--------------------");
            environment.traceln("-- Inner Classes ---");
            environment.traceln("--------------------");
            printInnerClasses();
            environment.traceln("--------------------");
        } catch (Throwable e) {
            environment.traceln("check name:" + e);
            environment.error("err.no.classname");
            e.printStackTrace();
        }
    }

    public void endPackageInfo() {
        this.this_class = pool.findClassCell(this.myClassName);
        // super_class: class "java/lang/Object"
        this.super_class = pool.findClassCell("java/lang/Object");
        pool.itemizePool();
        super_class = pool.specifyCell(super_class);
        this_class = pool.specifyCell(this_class);
        pool.checkGlobals();
    }

    public void endModule(ModuleAttr moduleAttr) {
        moduleAttribute = moduleAttr.build();
        this.myClassName = "module-info";
        this.this_class = pool.findClassCell(this.myClassName);
        pool.itemizePool();
        this_class = pool.specifyCell(this_class);
        pool.checkGlobals();
        // a module is annotated
        itemizeAnnotationAttributes(annotAttrInv, annotAttrVis);
    }

    /**
     * Scans all attribute values which only have cap Index != 0 when they are found
     * sets value and type from Constant Pool by cpIndex
     *
     * @param annotationList annotation attributes
     */
    private void itemizeAnnotationAttributes(DataVectorAttr<AnnotationData>... annotationList) {
        for (DataVectorAttr<AnnotationData> annotations : annotationList) {
            if (annotations != null) {
                for (AnnotationData annotationData : annotations) {
                    annotationData.visit(pool);
                }
            }
        }
    }

    private void printInnerClasses() {
        if (innerClasses != null) {
            int i = 1;
            for (InnerClassData entry : innerClasses) {
                environment.trace(" InnerClass[" + i++ + "]: (" + EModifier.asNames(entry.access, INNER_CLASS) + "), ");
                environment.trace("Name:  " + entry.name.toString() + " ");
                environment.trace("InnerClass_info:  " + entry.innerClass.toString() + " ");
                environment.traceln("OuterClass_info:  " + entry.outerClass.toString() + " ");
            }
        } else {
            environment.traceln("<< NO INNER CLASSES >>");
        }
    }

    public void write(CheckedDataOutputStream out) throws IOException {
        // Write the header
        out.writeInt(JAVA_MAGIC);
        out.writeShort(cfv.minor_version());
        out.writeShort(cfv.major_version());

        pool.write(out);
        out.writeShort(access); // & MM_CLASS; // Q
        out.writeShort(this_class.cpIndex);
        out.writeShort(super_class.cpIndex);

        // Write the interface names
        if (interfaces != null) {
            out.writeShort(interfaces.size());
            for (Indexer intf : interfaces) {
                out.writeShort(intf.cpIndex);
            }
        } else {
            out.writeShort(0);
        }

        // Write the fields
        if (fields != null) {
            out.writeShort(fields.size());
            for (FieldData field : fields) {
                field.write(out);
            }
        } else {
            out.writeShort(0);
        }

        // Write the methods
        if (methods != null) {
            out.writeShort(methods.size());
            for (MethodData method : methods) {
                method.write(out);
            }
        } else {
            out.writeShort(0);
        }

        // Write the attributes
        DataVector attrs = getAttrVector();
        attrs.write(out);
    } // end ClassData.write()

    @Override
    protected DataVector getAttrVector() {
        if (moduleAttribute != null) {
            return populateAttributesList(annotAttrVis, annotAttrInv, moduleAttribute);
        } else {
            return populateAttributesList(sourceFileNameAttr,
                    recordData,                                     // JEP 359 since class file 58.65535
                    innerClasses, syntheticAttr, deprecatedAttr, signatureAttr,
                    annotAttrVis, annotAttrInv,
                    type_annotAttrVis, type_annotAttrInv,
                    bootstrapMethodsAttr,
                    nestHostAttr, nestMembersAttr,                  // since class version 55.0
                    permittedSubclassesAttr,                        // since class version 59.65535 (JEP 360)
                    preloadAttr                                     // Valhalla
            );
        }
    }

    private <T extends DataWriter> DataVector populateAttributesList(T... attributes) {
        DataVector attrVector = new DataVector();
        for (T attribute : attributes) {
            if (attribute != null) {
                attrVector.add(attribute);
            }
        }
        return attrVector;
    }

    /**
     * Writes to the directory passed with -d option
     */
    public void write(File destDir) throws IOException {
        final String fileSeparator = FileSystems.getDefault().getSeparator();
        File outfile;
        if (destDir == null) {
            int startOfName = myClassName.lastIndexOf(fileSeparator);
            if (startOfName != -1) {
                myClassName = myClassName.substring(startOfName + 1);
            }
            outfile = new File(myClassName + fileExtension);
        } else {
            environment.traceln("writing -d " + destDir.getPath());
            if (!fileSeparator.equals("/")) {
                myClassName = myClassName.replace("/", fileSeparator);
            }
            outfile = new File(destDir, myClassName + fileExtension);
            File outDir = new File(outfile.getParent());
            if (!outDir.exists() && !outDir.mkdirs()) {
                environment.error("err.cannot.write", outDir.getPath());
                return;
            }
        }
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)))) {
            cdos.setDataOutputStream(dos);
            write(cdos);
        } catch (Exception ex) {
            environment.error("err.cannot.write", ex.getMessage());
            throw ex;
        }
    }  // end write()

    public void setByteLimit(int bytelimit) {
        cdos.enable();
        cdos.setLimit(bytelimit);
    }

    public boolean nestHostAttributeExists() {
        return nestHostAttr != null;
    }

    public boolean nestMembersAttributesExist() {
        return nestMembersAttr != null;
    }

    public boolean recordAttributeExists() {
        return recordData != null;
    }

    public boolean preloadAttributeExists() {
        return preloadAttr != null;
    }

    /**
     * This is a wrapper for DataOutputStream, used for debugging purposes. it allows
     * writing the byte-stream of a class up to a given byte number.
     */
    static private class CDOutputStream implements CheckedDataOutputStream {

        public boolean enabled = false;
        private int byteLimit;
        private DataOutputStream dos;

        public CDOutputStream() {
            dos = null;
        }

        public CDOutputStream(OutputStream out) {
            setOutputStream(out);
        }

        public final void setOutputStream(OutputStream out) {
            dos = new DataOutputStream(out);
        }

        public void setDataOutputStream(DataOutputStream dos) {
            this.dos = dos;
        }

        public void setLimit(int limit) {
            byteLimit = limit;
        }

        public void enable() {
            enabled = true;
        }

        private synchronized void check(String loc) throws IOException {
            if (enabled && dos.size() >= byteLimit) {
                throw new IOException(loc);
            }
        }

        @Override
        public synchronized void write(int b) throws IOException {
            dos.write(b);
            check("Writing byte: " + b);
        }

        @Override
        public synchronized void write(byte b[], int off, int len) throws IOException {
            dos.write(b, off, len);
            check("Writing byte-array: " + b);
        }

        @Override
        public final void writeBoolean(boolean v) throws IOException {
            dos.writeBoolean(v);
            check("Writing writeBoolean: " + (v ? "true" : "false"));
        }

        @Override
        public final void writeByte(int v) throws IOException {
            dos.writeByte(v);
            check("Writing writeByte: " + v);
        }

        @Override
        public void writeShort(int v) throws IOException {
            dos.writeShort(v);
            check("Writing writeShort: " + v);
        }

        @Override
        public void writeChar(int v) throws IOException {
            dos.writeChar(v);
            check("Writing writeChar: " + v);
        }

        @Override
        public void writeInt(int v) throws IOException {
            dos.writeInt(v);
            check("Writing writeInt: " + v);
        }

        @Override
        public void writeLong(long v) throws IOException {
            dos.writeLong(v);
            check("Writing writeLong: " + v);
        }

        @Override
        public void writeFloat(float v) throws IOException {
            dos.writeFloat(v);
            check("Writing writeFloat: " + v);
        }

        @Override
        public void writeDouble(double v) throws IOException {
            dos.writeDouble(v);
            check("Writing writeDouble: " + v);
        }

        @Override
        public void writeBytes(String s) throws IOException {
            dos.writeBytes(s);
            check("Writing writeBytes: " + s);
        }

        @Override
        public void writeChars(String s) throws IOException {
            dos.writeChars(s);
            check("Writing writeChars: " + s);
        }

        @Override
        public void writeUTF(String s) throws IOException {
            dos.writeUTF(s);
            check("Writing writeUTF: " + s);
        }
    }
}// end class ClassData