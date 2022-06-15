/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.ToolLogger;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.util.ArrayList;

import static org.openjdk.asmtools.common.structure.EModifier.DEPRECATED_ATTRIBUTE;
import static org.openjdk.asmtools.common.structure.EModifier.SYNTHETIC_ATTRIBUTE;

/**
 * The common base structure for field_info, method_info, and component_info
 */
abstract public class MemberData<T extends Environment<? extends ToolLogger>> {

    protected final ConstantPool pool;
    private final T environment;
    protected int access;
    protected AttrData syntheticAttr, deprecatedAttr;
    protected DataVectorAttr<AnnotationData> annotAttrVis = null;
    protected DataVectorAttr<AnnotationData> annotAttrInv = null;
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrVis = null;
    protected DataVectorAttr<TypeAnnotationData> type_annotAttrInv = null;
    protected AttrData signatureAttr;

    public MemberData(ConstantPool pool, T environment) {
        this(pool, environment, 0);
    }

    public MemberData(ConstantPool pool, T environment, int access) {
        this.pool = pool;
        this.environment = environment;
        this.access = access;
    }

    public void createPseudoMod() {
        // If a member has a Pseudo-modifier

        // create the appropriate marker attributes,
        // and clear the PseudoModifiers from the access flags.
        if (EModifier.isSyntheticPseudoMod(access)) {
            syntheticAttr = new AttrData(pool, EAttribute.ATT_Synthetic);
            access &= ~SYNTHETIC_ATTRIBUTE.getFlag();
        }
        if (EModifier.isDeprecatedPseudoMod(access)) {
            deprecatedAttr = new AttrData(pool, EAttribute.ATT_Deprecated);
            access &= ~DEPRECATED_ATTRIBUTE.getFlag();
        }
    }

    public T getEnvironment() {
        return environment;
    }

    public ConstantPool getPool() {
        return pool;
    }

    public void setSignatureAttr(ConstCell value_cpx) {
        signatureAttr = new CPXAttr(pool, EAttribute.ATT_Signature, value_cpx);
    }

    protected abstract <D extends DataWriter> DataVector<D> getAttrVector();

    @SafeVarargs
    protected final <D extends DataWriter> DataVector<D> getDataVector(D... extraAttrs) {
        DataVector<D> attrs = new DataVector();
        for (D extra : extraAttrs) {
            if (extra != null) {
                attrs.add(extra);
            }
        }
        // common set for [ FieldData, MethodData, RecordData ]
        if (annotAttrVis != null) {
            attrs.add((D) annotAttrVis);
        }
        if (annotAttrInv != null) {
            attrs.add((D) annotAttrInv);
        }
        if (type_annotAttrVis != null) {
            attrs.add((D) type_annotAttrVis);
        }
        if (type_annotAttrInv != null) {
            attrs.add((D) type_annotAttrInv);
        }
        return attrs;
    }

    public void addAnnotations(ArrayList<AnnotationData> list) {
        for (AnnotationData item : list) {
            boolean invisible = item.invisible;

            if (item instanceof TypeAnnotationData typeAnnotationData) {
                // Type Annotations
                if (invisible) {
                    if (type_annotAttrInv == null) {
                        type_annotAttrInv = new DataVectorAttr<>(pool,
                                EAttribute.ATT_RuntimeInvisibleTypeAnnotations);
                    }
                    type_annotAttrInv.add(typeAnnotationData);
                } else {
                    if (type_annotAttrVis == null) {
                        type_annotAttrVis = new DataVectorAttr<>(pool,
                                EAttribute.ATT_RuntimeVisibleTypeAnnotations);
                    }
                    type_annotAttrVis.add(typeAnnotationData);
                }
            } else {
                // Regular Annotations
                if (invisible) {
                    if (annotAttrInv == null) {
                        annotAttrInv = new DataVectorAttr<>(pool,
                                EAttribute.ATT_RuntimeInvisibleAnnotations);
                    }
                    annotAttrInv.add(item);
                } else {
                    if (annotAttrVis == null) {
                        annotAttrVis = new DataVectorAttr<>(pool,
                                EAttribute.ATT_RuntimeVisibleAnnotations);
                    }
                    annotAttrVis.add(item);
                }
            }
        }
    }
}
