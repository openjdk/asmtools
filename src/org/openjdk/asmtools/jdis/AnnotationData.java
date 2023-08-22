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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.*;

public class AnnotationData<T extends MemberData> extends MemberData {
    protected String visibleAnnotationToken = "@+";
    protected String invisibleAnnotationToken = "@-";
    protected String dataName = "AnnotationData";
    private final ArrayList<AnnotationElement> annotationElements = new ArrayList<>();

    private int type_cpx = 0;       //an index into the constant pool indicating the annotation type for this annotation.
    private final boolean invisible;

    public <T extends MemberData> AnnotationData(T data, boolean invisible) {
        super(data);
        this.invisible = invisible;
    }

    public void read(DataInputStream in) throws IOException {
        type_cpx = in.readShort();
        int elemValueLength = in.readShort();
        for (int i = 0; i < elemValueLength; i++) {
            AnnotationElement elem = new AnnotationElement(data);
            elem.read(in, invisible);
            annotationElements.add(elem);
        }
    }

    @Override
    public void print() throws IOException {
        printHeader();
        printBody();
    }

    protected void printHeader() {
        //Print annotation Header, which consists of the
        // Annotation Token ('@'), visibility ('+', '-'),
        // and the annotation name (type index, CPX).
        // Mark whether it is invisible or not.
        String annotationName = pool.getString(type_cpx, index -> "#" + index);
        // TODO: check Valhalla InlinableReferenceType: Q ClassName ;
        // converts class type to java class name
        if ((annotationName.startsWith("L") || annotationName.startsWith("Q")) && annotationName.endsWith(";")) {
            annotationName = annotationName.substring(1, annotationName.length() - 1);
        }
        switch (getAnnotationElementState()) {
            case HAS_DEFAULT_VALUE -> {
                print(DEFAULT_VALUE_PREFIX).print(invisible ? invisibleAnnotationToken : visibleAnnotationToken).print(annotationName);
                setCommentOffset(getCommentOffset() + DEFAULT_VALUE_PREFIX.length());
            }
            case PARAMETER_ANNOTATION, INLINED_ELEMENT -> print(invisible ? invisibleAnnotationToken : visibleAnnotationToken).print(annotationName);
            default -> printIndent(invisible ? invisibleAnnotationToken : visibleAnnotationToken).print(annotationName);
        }
    }

    protected void printBody() throws IOException {
        // For a standard annotation, print out brackets,
        // and list the name/value pairs.
        if (isEmpty()) {
            // Marker annotation
            print(" { }");
        } else {
            switch (getAnnotationElementState()) {
                case HAS_DEFAULT_VALUE -> {
                    println(" {");
                    printBodyOfDefaultData();
                    print(" }");
                }
                case INLINED_ELEMENT -> {
                    incIndent().printIndentLn("{");
                    printBodyOfData();
                    printIndent("}").decIndent();
                }
                default -> {
                    println("{");
                    printBodyOfData();
                    printIndent("}");
                }
            }
        }
    }

    // Prints the annotation value that is the default.
    protected void printBodyOfDefaultData() throws IOException {
        int prefixLength = getCommentOffset();
        for (AnnotationElement annotationElement : annotationElements) {
            print(enlargedIndent(prefixLength));
            annotationElement.setElementState(RIGHT_OPERAND);
            annotationElement.print();
        }
    }

    // Prints the annotation value that is the default.
    protected void printBodyOfData() throws IOException {
        int i = 0;
        for (AnnotationElement annotationElement : annotationElements) {
            annotationElement.setCommentOffset(getCommentOffset()).setTheSame(this).incIndent();
            annotationElement.print();
            println((i++ < annotationElements.size() - 1) ? "," : "");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String annotationName = pool.getString(type_cpx, index -> "#" + index);
        // TODO: check 401 InlinableReferenceType: Q ClassName ;
        // converts class type to java class name
        if ((annotationName.startsWith("L") || annotationName.startsWith("Q")) && annotationName.endsWith(";")) {
            annotationName = annotationName.substring(1, annotationName.length() - 1);
        }
        sb.append(invisible ? invisibleAnnotationToken : visibleAnnotationToken);
        sb.append(annotationName).append("{");
        sb.append(annotationElements.stream().map(AnnotationElement::toString).collect(Collectors.joining(",")));
        return sb.append("}").toString();
    }

    /**
     * @return true if annotation has no elements
     */
    public boolean isEmpty() {
        return annotationElements.isEmpty();
    }
}
