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

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.structure.CFVersion;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.openjdk.asmtools.common.structure.ClassFileContext.INNER_CLASS;
import static org.openjdk.asmtools.jasm.ClassData.CoreClasses.PLACE.HEADER;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_DYNAMIC;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_INVOKEDYNAMIC;
import static org.openjdk.asmtools.jasm.ClassFileConst.JAVA_MAGIC;
import static org.openjdk.asmtools.jasm.Indexer.NotSet;

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
    String myClassName;
    // Core classes of the class file: this_class, super_class
    CoreClasses coreClasses = new CoreClasses();
    AttrData sourceFileAttr;
    SourceDebugExtensionAttr sourceDebugExtensionAttr;
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
    //
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
    public final void init(int access, ConstCell<?> this_class, ConstCell<?> super_class, ArrayList<Indexer> interfaces) {
        this.access = access;
        // normalize the modifiers to access flags
        if (EModifier.hasPseudoMod(access)) {
            createPseudoMod();
        }
        this.coreClasses.this_class(HEADER, this_class);
        this.coreClasses.super_class(HEADER, super_class);
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
        this.coreClasses.super_class(HEADER, new ConstCell(0));
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
     * this method links the Constant_InvokeDynamic|Constant_Dynamic
     * constants with any bootstrap methods that they index in the
     * Bootstrap Methods Attribute
     */
    protected void relinkBootstrapMethods() {
        if (bootstrapMethodsAttr != null) {
            ArrayList<ConstCell<?>> cells = pool.getPoolValuesByRefType(CONSTANT_INVOKEDYNAMIC, CONSTANT_DYNAMIC);
            environment.traceln("relinkBSMs: %d items", cells.size());
            for (ConstCell<?> cell : cells) {
                ConstantPool.ConstValue_BootstrapMethod refVal = (ConstantPool.ConstValue_BootstrapMethod) cell.ref;
                BootstrapMethodData bsmData = refVal.bsmData();
                if (refVal.isSet() & refVal.value.ref == null) {
                    ConstCell<?> c = pool.getCell(((ConstCell<?>) refVal.value).cpIndex);
                    refVal.setValue(c);
                }
                if (bsmData != null && bsmData.hasMethodAttrIndex()) {
                    // find the real BSM Data at the index
                    int methodAttrIndex = bsmData.getMethodAttrIndex();
                    if (methodAttrIndex < 0 || methodAttrIndex > bootstrapMethodsAttr.size()) {
                        // bad BSM index - give a warning, but place the index in the arg anyway
                        environment.warning("warn.bootstrapmethod.attr.bad", methodAttrIndex);
                        bsmData.setMethodAttrIndex(methodAttrIndex);
                    } else {
                        // make the IndyPairs BSM Data point to the one from the attribute
                        refVal.setBsmData(bootstrapMethodsAttr.get(methodAttrIndex), methodAttrIndex);
                    }
                }
            }
        }
    }

    /**
     * Finds first BSM data element by value in a collection
     */
    private <T extends Collection<BootstrapMethodData>> int getFirstIndex(T collection, BootstrapMethodData bsmData) {
        if (!collection.isEmpty()) {
            BootstrapMethodData[] array = collection.toArray(BootstrapMethodData[]::new);
            for (int i = 0; i < array.length; i++) {
                if (bsmData.equalsByValue(array[i])) {
                    return i;
                }
            }
        }
        return NotSet;
    }

    /**
     * Relinks BSM data (BootstrapMethod Attribute) and Constant Pool Constant_InvokeDynamic|Constant_Dynamic entries if
     * at least one CP cell has undefined method attribute index also the method removes duplicates in BootstrapMethod
     * Attribute if found
     */
    private void uniquifyBootstrapMethods() {
        if (bootstrapMethodsAttr != null) {
            int index = 0;
            final List<BootstrapMethodData> cpBsmList = this.getPool().
                    getPoolCellsByType(CONSTANT_DYNAMIC, CONSTANT_INVOKEDYNAMIC).
                    stream().map(item -> ((ConstantPool.ConstValue_BootstrapMethod) item.ref).
                            bsmData()).toList();
            if (cpBsmList.stream().anyMatch(item -> !item.hasMethodAttrIndex())) {
                environment.traceln("numberBSM: %d items", cpBsmList.size());
                // remove duplicates in BootstrapMethod_Attribute if found
                // Fix 7902888: Excess entries in BootstrapMethods with the same bsm, bsmKind, bsmArgs
                final ArrayList<BootstrapMethodData> newBsmList = new ArrayList<>(cpBsmList.size());
                for (int i = 0; i < cpBsmList.size(); i++) {
                    BootstrapMethodData bsmData = cpBsmList.get(i);
                    int cachedIndex = getFirstIndex(newBsmList, bsmData);
                    if (cachedIndex != NotSet) {
                        bsmData.setMethodAttrIndex(cachedIndex);
                    } else {
                        if (getFirstIndex(this.bootstrapMethodsAttr, bsmData) == NotSet) {
                            environment.warning("warn.bootstrapmethod.attr.expected", bsmData.toString());
                        } else {
                            bsmData.setMethodAttrIndex(index++);
                        }
                        newBsmList.add(bsmData);
                    }
                }
                bootstrapMethodsAttr.replaceAll(newBsmList);
            }
        }
    }

    public AttrData setSourceFileAttr(ConstCell value_cpx) {
        this.sourceFileAttr = new CPXAttr(pool, EAttribute.ATT_SourceFile, value_cpx);
        return this.sourceFileAttr;
    }

    public SourceDebugExtensionAttr setSourceDebugExtensionAttr() {
        this.sourceDebugExtensionAttr = new SourceDebugExtensionAttr(pool);
        return this.sourceDebugExtensionAttr;
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
        return pool.findCell(ConstType.CONSTANT_METHODREF, coreClasses.this_class(), pool.findCell(nape));
    }

    public ConstCell LocalMethodRef(ConstCell name, ConstCell sig) {
        return LocalMethodRef(makeFieldRef(name, sig));
    }

    void addLocVarData(int opc, Indexer arg) {
    }

    public void addInnerClass(int access, ConstCell name, ConstCell innerClass, ConstCell outerClass) {
        environment.traceln("addInnerClass (with indexes: Name (" + name.toString() +
                "), Inner (" + innerClass.toString() + "), Outer (" + outerClass.toString() + ").");
        if (innerClasses == null) {
            innerClasses = new DataVectorAttr<>(pool, EAttribute.ATT_InnerClasses);
        }
        innerClasses.add(new InnerClassData(access, name, innerClass, outerClass));
    }

    public void addBootstrapMethod(BootstrapMethodData bsmData) {
        if (bootstrapMethodsAttr == null) {
            bootstrapMethodsAttr = new DataVectorAttr<>(pool, EAttribute.ATT_BootstrapMethods);
        }
        bootstrapMethodsAttr.add(bsmData);
        environment.traceln("addBootstrapMethod: " + bsmData.toString());
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
        if (coreClasses.super_class() == null) {
            coreClasses.super_class(pool.findClassCell("java/lang/Object"));
        }
        pool.itemizePool();
        coreClasses.specifyClasses(pool);
        coreClasses.cleanConstantPool(pool);
        pool.checkGlobals();
        pool.fixIndexesInPool();
        itemizeAttributes(new DataVectorAttr<>(pool, EAttribute.ATT_ConstantValue).
                        addAll(fields.stream().map(f -> f.getInitialValue())),
                annotAttrInv, annotAttrVis);
        uniquifyBootstrapMethods();
        try {
            myClassName = coreClasses.getFileName();
            environment.traceln("ClassFileName = " + myClassName);
            environment.traceln("this_class    = " + coreClasses.this_class());
            environment.traceln("super_class   = " + coreClasses.super_class());
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
        coreClasses.this_class(pool.findClassCell(this.myClassName));
        // super_class: class "java/lang/Object"
        coreClasses.super_class(pool.findClassCell("java/lang/Object"));
        pool.itemizePool();
        coreClasses.super_class(pool.specifyCell(coreClasses.super_class()));
        coreClasses.this_class(pool.specifyCell(coreClasses.this_class()));
        pool.checkGlobals();
    }

    public void endModule(ModuleAttr moduleAttr) {
        moduleAttribute = moduleAttr.build();
        this.myClassName = "module-info";
        coreClasses.this_class(pool.findClassCell(this.myClassName));
        pool.itemizePool();
        coreClasses.this_class(pool.specifyCell(coreClasses.this_class()));
        pool.checkGlobals();
        // a module is annotated
        itemizeAttributes(annotAttrInv, annotAttrVis);
    }

    /**
     * Scans all attributes that
     * 1. only have cpIndex != 0 and undefined values, types if they are found the method sets their values and types.
     * It applies to DataVectorAttr<AnnotationData>
     * 2. only have values and undefined cpIndex if they are found the method finds the identical values in CP and
     * assigns their cpIndexes instead of  undefined indexes.
     * It works for DataVectorAttr<?>
     *
     * @param attributeList list of attribute's list
     */
    private <A extends AttrData> void itemizeAttributes(A... attributeList) {
        for (A attributes : attributeList) {
            if (attributes != null) {
                if (attributes instanceof DataVectorAttr<?>) {
                    ((DataVectorAttr<?>) attributes).getElements().stream().
                            map(e -> (ConstantPoolDataVisitor) e).forEach(v -> v.visit(pool));
                } else if (attributes instanceof AttrData) {
                    attributes.visit(pool);
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
        out.writeShort(coreClasses.this_class().cpIndex);
        out.writeShort(coreClasses.super_class().cpIndex);

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
            return populateAttributesList(
                    sourceFileAttr,
                    sourceDebugExtensionAttr,
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
    public void write(ToolOutput toolOutput) throws IOException {
        try (DataOutputStream dos = toolOutput.getDataOutputStream()) {
            cdos.setDataOutputStream(dos);
            write(cdos);
        } catch (Exception ex) {
            environment.error("err.cannot.write", ex.getMessage());
            throw ex;
        }
    }

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

    /**
     * Container holds 2 pairs of core classes: this_class, super_class, and functionality to get output file name.
     *  jasm supports the values:
     *  [CLASS_MODIFIERS] class|interface CLASSNAME [ extends SUPERCLASSNAME ] { // HEADER
     *  this_class[:]  (#ID | IDENT); // CLASSNAME                                  CLASSFILE
     *  super_class[:] (#ID | IDENT); // SUPERCLASSNAME                             CLASSFILE
     */
    public static class CoreClasses {
        public enum PLACE {
            // A place where this_class, super_class pair is defined.
            HEADER, CLASSFILE
        }

        private String fileName;

        // This and Super classes  are defined on the top in the header:
        // [CLASS_MODIFIERS] class|interface CLASSNAME [ extends SUPERCLASSNAME ] { // HEADER
        //
        // public super class #11 extends #14 version 66:0
        // public super class package/ClassName extends package/SuperClassName version 66:0
        Pair<ConstCell<?>, ConstCell<?>> header = new Pair<>(null, null);
        // This and Super classes are defined in a class file:
        // this_class[:]  (#ID | IDENT);    // CLASSNAME                               CLASSFILE
        // super_class[:] (#ID | IDENT);   // SUPERCLASSNAME                           CLASSFILE
        //
        // this_class  #7;                 // package/ClassName
        // super_class java/lang/Object;   // java/lang/Object
        Pair<ConstCell<?>, ConstCell<?>> classfile = new Pair<>(null, null);

        public void this_class(PLACE where, ConstCell<?> this_class) {
            if (where == PLACE.CLASSFILE) {
                classfile.first = this_class;
            } else {
                header.first = this_class;
            }
        }

        public void super_class(PLACE where, ConstCell<?> super_class) {
            if (where == PLACE.CLASSFILE) {
                classfile.second = super_class;
            } else {
                header.second = super_class;
            }
        }

        public void this_class(ConstCell<?> this_class) {
            if (classfile.first != null) {
                classfile.first = this_class;
            } else {
                header.first = this_class;
            }
        }

        public void super_class(ConstCell<?> super_class) {
            if (classfile.second != null) {
                classfile.second = super_class;
            } else {
                header.second = super_class;
            }
        }

        public ConstCell<?> this_class() {
            return (classfile.first != null) ? classfile.first : header.first;
        }

        public ConstCell<?> super_class() {
            return (classfile.second != null) ? classfile.second : header.second;
        }

        public String getFileName() {
            if (fileName == null) {
                fileName = calculateFileName();
            }
            return fileName;
        }

        private String calculateFileName() {
            if (header.first != null) {
                ConstantPool.ConstValue_Class this_class_value = (ConstantPool.ConstValue_Class) header.first.ref;
                ConstantPool.ConstValue_UTF8 this_class_name = this_class_value.value.ref;
                this.fileName = this_class_name.value;
                return this_class_name.value;
            }
            return null;
        }

        /**
         * If jasm file contains this_class/super_class value then this value overwrites the class/super class defined on the top:
         * "public super class ClassName extends #9 version 66:0 {"
         * ie just added to ConstantPool classes: ClassName && #9 should be removed
         *
         * @param constantPool constant pool
         */
        public void cleanConstantPool(ConstantPool constantPool) {
            if (classfile.first != null && classfile.first.cpIndex != header.first.cpIndex) {
                calculateFileName();
                constantPool.removeClassCell((ConstCell<ConstantPool.ConstValue_Class>)header.first);
            }
            if (classfile.second != null && header.second != null &&
                    classfile.second.cpIndex != header.second.cpIndex) {
                constantPool.removeClassCell((ConstCell<ConstantPool.ConstValue_Class>)header.second);
            }
        }
        public void specifyClasses(ConstantPool constantPool) {
            if (header.first != null)
                header.first = constantPool.specifyCell(header.first);
            if (header.second != null)
                header.second = constantPool.specifyCell(header.second);
            if (classfile.first != null)
                classfile.first = constantPool.specifyCell(classfile.first);
            if (classfile.second != null)
                classfile.second = constantPool.specifyCell(classfile.second);
        }
    }
}// end class ClassData
