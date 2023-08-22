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

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.FormatError;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.JasmTokens;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Base class for ClassData, MethodData, FieldData and RecordData(JEP 360)
 */
public abstract class  MemberData<T extends MemberData> extends Indenter {

    // String prefix to print Defaults for Annotation Interface Elements
    protected static final String DEFAULT_VALUE_PREFIX = "default { ";

    protected T data;
    protected JdisEnvironment environment;          // Environment of this data
    protected ConstantPool  pool;
    protected String        memberType = "";

    // access flags (modifiers)
    protected int access;

    // extra flags
    protected boolean isSynthetic = false;
    protected boolean isDeprecated = false;

    // indicates the state of the annotation element
    public enum AnnotationElementState {
        HAS_DEFAULT_VALUE,       // An annotation interface element has a default value
        DEFAULT_STATE,
        PARAMETER_ANNOTATION,
        RIGHT_OPERAND,
        INLINED_ELEMENT          // An annotation element is element of an annotation element.
    }

    private AnnotationElementState annotationElementState = AnnotationElementState.DEFAULT_STATE;

    // Signature can be located in ClassFile, field_info, method_info, and component_info
    protected SignatureData signature;

    // The visible annotations for this class, member( field or method) or record component
    protected ArrayList<AnnotationData> visibleAnnotations;

    // The invisible annotations for this class, member( field or method) or record component
    protected ArrayList<AnnotationData> invisibleAnnotations;

    // The visible annotations for this class, member( field or method) or record component
    protected ArrayList<TypeAnnotationData> visibleTypeAnnotations;

    // The invisible annotations for this class, member( field or method) or record component
    protected ArrayList<TypeAnnotationData> invisibleTypeAnnotations;

    // The remaining attributes of this class, member( field or method) or record component
    protected ArrayList<AttrData> attributes;

    public MemberData(T data) {
        super(data.toolOutput);
        this.environment = data.environment;
        init(data);
    }

    public MemberData(JdisEnvironment environment) {
        super(environment.getToolOutput());
        this.environment = environment;
    }

    public void init( T data) {
        this.data = data;
        this.pool = data.pool;
    }

    public ConstantPool getConstantPool() {
        return pool;
    }

    protected boolean handleAttributes(DataInputStream in,
                                       EAttribute attributeTag,
                                       int attributeLength) throws IOException {
        // sub-classes override
        return false;
    }

    protected String getPseudoFlagsAsString() {
        String s = "";
        if (isSynthetic)
            s += JasmTokens.Token.SYNTHETIC.parseKey() + " ";
        if (isDeprecated)
            s += JasmTokens.Token.DEPRECATED.parseKey() + " ";
        return s;
    }

    /**
     * Gets a state of the annotation element
     */
    public AnnotationElementState getAnnotationElementState() {
        return annotationElementState;
    }

    /**
     * Sets the state of the annotation element
     */
    public MemberData setElementState(AnnotationElementState state) {
        annotationElementState = state;
        return this;
    }

    final protected int getAnnotationsCount() {
        return  ((visibleAnnotations == null) ? 0 : visibleAnnotations.size()) +
                ((invisibleAnnotations == null) ? 0 : invisibleAnnotations.size()) +
                ((visibleTypeAnnotations == null) ? 0 : visibleTypeAnnotations.size()) +
                ((invisibleTypeAnnotations == null) ? 0 : invisibleTypeAnnotations.size());
    }

    final protected void printAnnotations() throws IOException {
        if( getAnnotationsCount() > 0 ) {
            if (visibleAnnotations != null) {
                for (AnnotationData va : visibleAnnotations) {
                    va.setTheSame(this).print();
                    println();
                }
            }
            if (invisibleAnnotations != null) {
                for (AnnotationData ia : invisibleAnnotations) {
                    ia.setTheSame(this).print();
                    println();
                }
            }
            if (visibleTypeAnnotations != null) {
                for (TypeAnnotationData vta : visibleTypeAnnotations) {
                    vta.setTheSame(this).print();
                    println();
                }
            }
            if (invisibleTypeAnnotations != null) {
                for (TypeAnnotationData ita : invisibleTypeAnnotations) {
                    ita.setTheSame(this).print();
                    println();
                }
            }
        }
    }

    /**
     * Prints field or a record component
     * @param prefix      the field prefix: "private static final Field" or the component prefix: "synthetic Component"
     * @param postfix     String presentation of the initial value if exists ( = String "ABC" )
     * @param name_cpx    Field/Component name cpIndex
     * @param type_cpx    Field/Component type cpIndex
     * @param value_cpx   either cpIndex of an initial value of a field or 0
     *                    if it's a component or the field doesn't have an initial value.
     */
    protected void printVar(StringBuilder prefix, String postfix, int name_cpx, int type_cpx, int value_cpx) {

        Pair<String, String> signInfo = ( signature != null) ?
                signature.getPrintInfo((i)->pool.inRange(i)) :
                new Pair<>("", "");

        if(printCPIndex) {
            prefix.append('#').append(name_cpx).append(":#").append(type_cpx).append(signInfo.first);
            if(value_cpx != 0) {
                prefix.append(" = #").append(value_cpx);
            }
            prefix.append(';');
            if( skipComments ) {
               print(prefix.toString());
            } else {
                printPadRight(prefix.toString(), getCommentOffset() - 1).print(" // ");
                print(data.pool.getName(name_cpx) + ":" +
                        data.pool.getName(type_cpx) +
                        signInfo.second +
                        (postfix != null ? postfix : ""));
            }
        } else {
            prefix.append(data.pool.getName(name_cpx)).append(':').
                    append(data.pool.getName(type_cpx)).
                    append(signInfo.second);
            if( postfix != null ) {
                prefix.append(postfix);
            }
            print(prefix+";");
        }
        println();
    }

    protected void readAttributes(DataInputStream in) throws IOException {
        // Read the Attributes
        int attributesCount = in.readUnsignedShort();
        attributes = new ArrayList<>(attributesCount);
        environment.traceln(format("%s - Attributes[%d]", memberType , attributesCount));
        AttrData attrData;
        for (int k = 0; k < attributesCount; k++) {
            int name_cpx = in.readUnsignedShort();
            attrData = new AttrData(this.environment);
            attributes.add(attrData);
            String attr_name = data.pool.getString(name_cpx, index->"#"+index);
            environment.traceln(format("Attribute#%d name[%d]=\"%s\"", k, name_cpx, attr_name));
            EAttribute tag = EAttribute.get(attr_name);
            int attrLength = in.readInt();
            switch (tag) {
                case ATT_Synthetic:
                    // Read Synthetic Attribute
                    if (attrLength != 0) {
                        throw new FormatError(environment.getLogger(),
                                "err.invalid.attribute.length",  tag.printValue(), attrLength);
                    }
                    isSynthetic = true;
                    break;
                case ATT_Deprecated:
                    // Read Deprecated Attribute
                    if (attrLength != 0) {
                        throw new FormatError(environment.getLogger(),
                                "err.invalid.attribute.length", tag.printValue(), attrLength);
                    }
                    isDeprecated = true;
                    break;
                case ATT_RuntimeVisibleAnnotations:
                case ATT_RuntimeInvisibleAnnotations:
                    // Read Annotations Attribute
                    int count = in.readShort();
                    ArrayList<AnnotationData> annotations = new ArrayList<>(count);
                    boolean invisible = (tag == EAttribute.ATT_RuntimeInvisibleAnnotations);
                    for (int i = 0; i < count; i++) {
                        AnnotationData annotationData = new AnnotationData(data, invisible);
                        annotationData.read(in);
                        annotations.add(annotationData);
                    }
                    if (invisible) {
                        invisibleAnnotations = annotations;
                    } else {
                        visibleAnnotations = annotations;
                    }
                    break;
                case ATT_RuntimeVisibleTypeAnnotations:
                case ATT_RuntimeInvisibleTypeAnnotations:
                    // Read Type Annotations Attribute
                    count = in.readShort();
                    ArrayList<TypeAnnotationData> typeAnnotations = new ArrayList<>(count);
                    invisible = (tag == EAttribute.ATT_RuntimeInvisibleTypeAnnotations);
                    for (int i = 0; i < count; i++) {
                        TypeAnnotationData typeAnnotationData = new TypeAnnotationData(data, invisible);
                        typeAnnotationData.read(in);
                        typeAnnotations.add(typeAnnotationData);
                    }
                    if (invisible) {
                        invisibleTypeAnnotations = typeAnnotations;
                    } else {
                        visibleTypeAnnotations = typeAnnotations;
                    }
                    break;
                default:
                    boolean handled = handleAttributes(in, tag, attrLength);
                    if (!handled) {
                        attrData.read(name_cpx, attrLength, in);
                    } else {
                        attrData.setNameCpx(name_cpx).setLength(attrLength);
                    }
                    break;
            }
        }
    }
}
