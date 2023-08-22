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

import org.openjdk.asmtools.common.FormatError;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.asmutils.StringUtils.Utf8ToString;
import static org.openjdk.asmtools.asmutils.StringUtils.isPrintableChar;
import static org.openjdk.asmtools.jasm.ClassFileConst.AnnotationElementType;
import static org.openjdk.asmtools.jasm.ClassFileConst.getAnnotationElementType;
import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.*;

/**
 * Base class of all AnnotationElement entries
 */
public class AnnotationElement<T extends MemberData<T>> extends MemberData<T> {

    //constant pool index for the name of the Annotation Element
    public int name_cpx;
    public AnnotationValue<T> value = null;

    public AnnotationElement(T data) {
        super(data);
    }

    /*
     * Static factory - creates Annotation Elements.
     */
    public static <P extends MemberData<P>> AnnotationValue<P> readValue(DataInputStream in, P data, boolean invisible) throws IOException {
        AnnotationValue<P> val;
        char tg = (char) in.readByte();
        AnnotationElementType tag = getAnnotationElementType(tg);
        switch (tag) {
            // String, Byte, Char, Int  (no need to add keyword), Short, Boolean, Float, Double, Long, Class
            case AE_STRING, AE_BYTE, AE_CHAR, AE_INT, AE_SHORT, AE_BOOLEAN, AE_FLOAT, AE_DOUBLE, AE_LONG, AE_CLASS -> {
                // CPX based Annotation
                int cpx = in.readShort();
                val = new CPX_AnnotationValue<>(tag, data, cpx);
            }
            // Enum
            case AE_ENUM -> {
                // CPX2 based Annotation
                int cpx1 = in.readShort();
                int cpx2 = in.readShort();
                val = new CPX2_AnnotationValue<>(tag, data, cpx1, cpx2);
            }
            // Annotation
            case AE_ANNOTATION -> {
                AnnotationData<P> annotationData = new AnnotationData<>(data, invisible);
                annotationData.read(in);
                val = new Annotation_AnnotationValue<>(tag, data, annotationData);
            }
            // Array
            case AE_ARRAY -> {
                Array_AnnotationValue<P> arrayAnnotationValue = new Array_AnnotationValue<>(tag, data);
                val = arrayAnnotationValue;
                int cnt = in.readShort();
                for (int i = 0; i < cnt; i++) {
                    arrayAnnotationValue.add(readValue(in, data, invisible));
                }
            }
            default -> throw new FormatError(data.environment.getLogger(),
                    "err.unknown.tag", isPrintableChar(tg) ? tg : '?', Integer.toHexString(tg));
        }
        return val;
    }

    /**
     * Read and resolve the method data called from ClassData. precondition: NumFields has
     * already been read from the stream.
     */
    public void read(DataInputStream in, boolean invisible) throws IOException {
        name_cpx = in.readShort();
        value = readValue(in, data, invisible);
        environment.traceln("AnnotationElement: cpIndex#%d=%s value=%s", name_cpx,
                pool.getString(name_cpx, index -> "????"), value.toString());
    }

    public String stringVal() {
        String name = pool.getName(name_cpx);
        if( printCPIndex ) {
            return (skipComments) ? format("#%d", name_cpx) : format("#%d /* %s */", name_cpx, name);
        }
        return name;
    }

    @Override
    public void print() throws IOException {
        printIndent(stringVal() + " = ");
        value.setTheSame(this).incIndent();
        if (value.elementType == AnnotationElementType.AE_ARRAY) {
            if (((Array_AnnotationValue<?>) value).annotationValues.size() == 0) {
                print("{ }");
            } else {
                println().print(getIndentString());
                value.print();
                printIndent("}");
            }
        } else {
            value.setElementState(this.getAnnotationElementState());
            value.print();
        }
    }

    @Override
    public String toString() {
        return "<AnnotationElement " + stringVal() + " = " + value.toString() + ">";
    }

    /**
     * Base class for an annotation value.
     */
    public static class AnnotationValue<T extends MemberData<T>> extends MemberData<T> {

        // tag the descriptor for the constant
        public AnnotationElementType elementType;

        public AnnotationValue(AnnotationElementType elementType, T data) {
            super(data);
            this.elementType = elementType;
            memberType = "AnnotationValue";
        }

        @Override
        public String toString() {
            return format("<%s %s %s>", memberType, elementType.printValue(), stringVal());
        }

        protected String stringVal() {
            return "";
        }

        @Override
        public void print() throws IOException {
            print(PadLeft(elementType.value(), 4));
        }
    }

    /**
     * Annotation value which is described by a single CPX entries (i.e. String, byte, char,
     * int, short, boolean, float, long, double, class reference).
     */
    public static class CPX_AnnotationValue<T extends MemberData<T>> extends AnnotationValue<T> {

        public int cpx;

        public CPX_AnnotationValue(AnnotationElementType elementType, T data, int cpx) {
            super(elementType, data);
            this.cpx = cpx;
            memberType = "CPX_AnnotationValue";
        }

        @Override
        public String stringVal() {
            StringBuilder sb = new StringBuilder();
            switch (elementType) {
                // String
                case AE_STRING -> sb.append(valueAsString("",
                        () -> Utf8ToString(pool.getString(cpx, index -> "#" + cpx), "\"")));
                // Byte, Char, Short
                case AE_BYTE, AE_CHAR, AE_SHORT -> sb.append(valueAsString(elementType.printValue(),
                        () -> pool.getConst(cpx).stringVal()));
                // Int (no need to add keyword), Long, Float, Double
                case AE_INT, AE_FLOAT, AE_DOUBLE, AE_LONG -> sb.append(valueAsString("",
                        () -> pool.getConst(cpx).stringVal()));
                // Boolean
                case AE_BOOLEAN -> sb.append(valueAsString(elementType.printValue(),
                        () -> ((ConstantPool.CP_Int) pool.getConst(cpx)).value == 0 ? "false" : "true"));
                // Class
                case AE_CLASS -> sb.append(valueAsString(elementType.printValue(),
                        () -> pool.decodeClassDescriptor(cpx)));
                default -> {
                }
            }
            return sb.toString();
        }

        private String valueAsString(String prefix, Supplier<String> supplier) {
            String str = prefix.isEmpty() ? "" : prefix + " ";
            if (printCPIndex) {
                if( skipComments ) {
                    str += format("#%d", cpx);
                } else {
                    str += format("#%d /* %s */", cpx, supplier.get());
                }
            } else {
                str += supplier.get();
            }
            return str;
        }

        @Override
        public void print() {
            AnnotationElementState state = getAnnotationElementState();
            if ( state == HAS_DEFAULT_VALUE) {
                print(DEFAULT_VALUE_PREFIX + "%s }", stringVal());
            } else if( state == RIGHT_OPERAND) {
                print(" %s }", stringVal());
            } else {
                print(stringVal());
            }
        }
    }

    /**
     * AnnotationValue that contain 2 cpx indices (i.e. enums).
     */
    public static class CPX2_AnnotationValue<T extends MemberData<T>> extends AnnotationValue<T> {

        public int cpx1;
        public int cpx2;

        public CPX2_AnnotationValue(AnnotationElementType elementType, T data, int cpx1, int cpx2) {
            super(elementType, data);
            this.cpx1 = cpx1;
            this.cpx2 = cpx2;
            memberType = "CPX2_AnnotationValue";
        }

        @Override
        public String stringVal() {
            StringBuilder sb = new StringBuilder();
            // Enum
            if (elementType == AnnotationElementType.AE_ENUM) {
                // print the enum type and constant name
                String className = pool.decodeClassDescriptor(cpx1);
                String name = pool.getName(cpx2);
                sb.append(elementType.printValue()).append(' ');
                if (printCPIndex) {
                    if( skipComments ) {
                        sb.append(format("#%d", cpx1)).append(format(" #%d", cpx2));
                    } else {
                        sb.append(format("#%d /* %s */", cpx1, className)).append(format(" #%d /* %s */", cpx2, name));
                    }
                } else {
                    sb.append(className).append(" ").append(name);
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return format("<%s %s>", memberType, stringVal());
        }

        @Override
        public void print() {
            if (getAnnotationElementState() == HAS_DEFAULT_VALUE) {
                print(DEFAULT_VALUE_PREFIX + "%s }", stringVal());
            } else {
                print(stringVal());
            }
        }
    }

    /**
     * Annotation value that is an array of annotation elements.
     */
    public static class Array_AnnotationValue<T extends MemberData<T>> extends AnnotationValue<T> {

        public ArrayList<AnnotationValue<T>> annotationValues = new ArrayList<>();

        public Array_AnnotationValue(AnnotationElementType elementType, T data) {
            super(elementType, data);
            memberType = "Array_AnnotationValue";
        }

        @Override
        public String stringVal() {
            return super.stringVal() + "={" +
                    annotationValues.stream().map(AnnotationValue::toString).collect(Collectors.joining(",")) +
                    '}';
        }

        public void add(AnnotationValue<T> annotationValue) {
            annotationValues.add(annotationValue);
        }

        @Override
        public void print() throws IOException {
            int count = annotationValues.size();
            if (annotationValues.size() > 0) {
                switch (getAnnotationElementState()) {
                    case HAS_DEFAULT_VALUE -> printDefaultAnnotationElement(count, getItemsPerLine(count, annotationValues.get(0)));
                    case INLINED_ELEMENT -> printDefaultAnnotationElement(count, 1);
                    default -> printAnnotationElement(count);
                }
            } else {
                // Empty default array value.
                print(DEFAULT_VALUE_PREFIX + "{ } }");
            }
        }

        private <P extends AnnotationValue<T>> int getIndent(P value) {
            if (value instanceof Annotation_AnnotationValue || value instanceof CPX_AnnotationValue) {
                // commentShift + "default { { ".length()
                return getCommentOffset() + DEFAULT_VALUE_PREFIX.length() + 2;
            }
            return 1;
        }

        private <P extends AnnotationValue<T>> int getItemsPerLine(int count, P value) {
            if (value instanceof Annotation_AnnotationValue) {
                return 1;
            } else if (value instanceof CPX_AnnotationValue) {
                return (count > 10) ? (count % 2 == 0 ? 4 : 6) : (count % 2 == 0 ? 2 : 3);
            }
            return 1;
        }

        public void printDefaultAnnotationElement(int count, int ItemsPerLine) throws IOException {
            int i = 0, lineIndent = getIndent(annotationValues.get(0));
            println(DEFAULT_VALUE_PREFIX + "{ ");
            printPadLeft(INDENT_STRING, INDENT_OFFSET*2);
            for (AnnotationValue<T> annotationValue : annotationValues) {
                annotationValue.setElementState(INLINED_ELEMENT);
                annotationValue.setCommentOffset(lineIndent);
                if (annotationValue instanceof Annotation_AnnotationValue ||
                        annotationValue instanceof CPX_AnnotationValue) {
                    if (i % ItemsPerLine == 0 && i != 0)
                        printPadLeft(INDENT_STRING, INDENT_OFFSET*2);
                }
                annotationValue.print(); // entry
                if (i < count - 1)
                    print("," + (i % ItemsPerLine == (ItemsPerLine - 1) ? System.lineSeparator() : " "));
                i++;
            }
            println(" }").print("  }");
        }

        public void printAnnotationElement(int count) throws IOException {
            int i = 0;
            println("{");
            for (AnnotationValue<T> annotationValue : annotationValues) {
                annotationValue.setTheSame(this);
                if (annotationValue instanceof CPX_AnnotationValue || annotationValue instanceof CPX2_AnnotationValue) {
                    print(annotationValue.getIndentString());
                }
                annotationValue.print(); // entry
                if (i < count - 1)
                    println(",");
                i++;
            }
            println();
        }
    }

    /**
     * Annotation value that is a reference to an annotation.
     */
    public static class Annotation_AnnotationValue<T extends MemberData<T>> extends AnnotationValue<T> {

        AnnotationData<T> annotationData;

        public Annotation_AnnotationValue(AnnotationElementType annotationElementType,
                                          T data,
                                          AnnotationData<T> annotationData) {
            super(annotationElementType, data);
            this.annotationData = annotationData;
            memberType = "Annotation_AnnotationValue";
        }

        @Override
        public String stringVal() {
            return annotationData.toString();
        }

        @Override
        public Annotation_AnnotationValue<T> setElementState(AnnotationElementState state) {
            super.setElementState(state);
            annotationData.setElementState(state);
            return this;
        }

        @Override
        public void print() throws IOException {
            // set the same offset, Indent etc.
            annotationData.setCommentOffset(this.getCommentOffset());
            annotationData.setTheSame(this);
            annotationData.print();  // check off
        }
    }
}
