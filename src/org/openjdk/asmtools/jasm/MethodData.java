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

import org.openjdk.asmtools.common.CompilerConstants;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

class MethodData extends MemberData<JasmEnvironment> {

    // MethodParameterData Attribute
    static class MethodParameterData implements DataWriter {

        int access;
        ConstCell name;

        public MethodParameterData(int access, ConstCell name) {
            this.access = access;
            this.name = name;
        }

        @Override
        public int getLength() {
            return 4;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeShort((name == null) ? 0 : name.cpIndex);
            out.writeShort(access);
        }
    }// end class MethodParameterData

    /**
     * Used to store Parameter Arrays (as attributes)
     */
    static public class DataPArrayAttr<T extends DataWriter> extends AttrData implements CompilerConstants {

        TreeMap<Integer, ArrayList<T>> elements; // Data
        int paramsTotal;

        public DataPArrayAttr(ConstantPool pool, EAttribute attribute, int paramsTotal, TreeMap<Integer, ArrayList<T>> elements) {
            super(pool, attribute);
            this.paramsTotal = paramsTotal;
            this.elements = elements;
        }

        public DataPArrayAttr(ConstantPool pool, EAttribute attribute, int paramsTotal) {
            this(pool, attribute, paramsTotal, new TreeMap<>());
        }

        public void put(int paramNum, T element) {
            ArrayList<T> v = get(paramNum);
            if (v == null) {
                v = new ArrayList<>();
                elements.put(paramNum, v);
            }

            v.add(element);
        }

        public ArrayList<T> get(int paramNum) {
            return elements.get(paramNum);
        }

        @Override
        public int attrLength() {
            int length = 1;  // One byte for the parameter count

            // calculate overall size here rather than in add()
            // because it may not be available at the time of invoking of add()
            for (int i = 0; i < paramsTotal; i++) {
                ArrayList<T> attrarray = get(i);
                if (attrarray != null) {
                    for (DataWriter item : attrarray) {
                        length += item.getLength();
                    }
                }
                length += 2; // 2 bytes for the annotation count for each parameter
            }

            return length;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            super.write(out);  // attr name, attr len
            out.writeByte(paramsTotal); // number of parameters total (in byte)

            for (int i = 0; i < paramsTotal; i++) {
                ArrayList<T> attrarray = get(i);
                if (attrarray != null) {
                    // write out the number of annotations for the current param
                    out.writeShort(attrarray.size());
                    for (T item : attrarray) {
                        item.write(out); // write the current annotation
                    }
                } else {
                    out.writeShort(0);
                    // No annotations to write out
                }
            }
        }
    }// end class DataPArrayAttr

    /* Method Data Fields */
    protected ClassData classData;
    protected ConstCell<?> nameCell, sigCell;
    protected CodeAttr code;
    protected DataVectorAttr<ConstCell<?>> exceptions = null;
    protected DataVectorAttr<MethodParameterData> methodParameters = null;
    protected DataPArrayAttr<AnnotationData> pannotAttrVis = null;
    protected DataPArrayAttr<AnnotationData> pannotAttrInv = null;
    protected DefaultAnnotationAttr defaultAnnot = null;

    public MethodData(ClassData classData, int access, ConstCell<?> name, ConstCell<?> signature, ArrayList<ConstCell<?>> exc_table) {
        super(classData.pool, classData.getEnvironment(), access);
        this.classData = classData;
        nameCell = name;
        sigCell = signature;
        if ((exc_table != null) && (!exc_table.isEmpty())) {
            exceptions = new DataVectorAttr<>(classData.pool, EAttribute.ATT_Exceptions, exc_table);
        }
        // Normalize the modifiers to access flags
        if (EModifier.hasPseudoMod(access)) {
            createPseudoMod();
        }
    }

    public void addMethodParameter(int totalParams, int paramNum, ConstCell<?> name, int access) {
        getEnvironment().traceln("addMethodParameter Param[" + paramNum + "] (name: " + name.toString() + ", Flags (" + access + ").");
        if (methodParameters == null) {
            methodParameters = new DataVectorAttr<>(classData.pool, EAttribute.ATT_MethodParameters, true);
            for (int i = 0; i < totalParams; i++) {
                // initialize the paramName array (in case the name is not given in Jasm syntax)
                methodParameters.add(new MethodParameterData(0, null));
            }
        }
        methodParameters.set(paramNum, new MethodParameterData(access, name));
    }

    public CodeAttr startCode(int paramCount, Indexer max_stack, Indexer max_locals) {
        code = new CodeAttr(this, paramCount, max_stack, max_locals);
        return code;
    }

    public void addDefaultAnnotation(DefaultAnnotationAttr data) {
        defaultAnnot = data;
    }

    public void addParamAnnotation(int totalParams, int paramNum, AnnotationData data) {
        if (!data.invisible) {
            if (pannotAttrVis == null) {
                pannotAttrVis = new DataPArrayAttr<>(classData.pool,
                        EAttribute.ATT_RuntimeVisibleParameterAnnotations,totalParams);
            }
            pannotAttrVis.put(paramNum, data);

        } else {
            if (pannotAttrInv == null) {
                pannotAttrInv = new DataPArrayAttr<>(classData.pool,
                        EAttribute.ATT_RuntimeInvisibleParameterAnnotations, totalParams);
            }
            pannotAttrInv.put(paramNum, data);
        }
    }

    @Override
    protected DataVector getAttrVector() {
        DataVector dv = getDataVector( exceptions, syntheticAttr, deprecatedAttr, signatureAttr, methodParameters, code, defaultAnnot);
        if (pannotAttrVis != null) {
            dv.add(pannotAttrVis);
        }
        if (pannotAttrInv != null) {
            dv.add(pannotAttrInv);
        }
        return dv;
    }

    public void write(CheckedDataOutputStream out) throws IOException, Parser.CompilerError {
        out.writeShort(access);
        out.writeShort(nameCell.cpIndex);
        out.writeShort(sigCell.cpIndex);
        getAttrVector().write(out);
    }
} // end MethodData
