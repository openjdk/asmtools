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

import org.openjdk.asmtools.asmutils.Pair;
import org.openjdk.asmtools.common.structure.ClassFileContext;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.common.structure.EModifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.HAS_DEFAULT_VALUE;
import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.PARAMETER_ANNOTATION;

/**
 * Method data for method members in a class of the Java Disassembler
 */
public class MethodData extends MemberData<ClassData> {

    //ConstantPool index to the method name
    protected int name_cpx;

    //ConstantPool index to the method type
    protected int sig_cpx;

    // labelPrefix
    protected String lP;

    //The parameter names for this method
    protected ArrayList<ParamNameData> paramNameDates;

    //The visible parameter annotations for this method
    protected ParameterAnnotationData visibleParameterAnnotations;

    //The invisible parameter annotations for this method
    protected ParameterAnnotationData invisibleParameterAnnotations;

    // The invisible parameter annotations for this method
    protected AnnotationElement.AnnotationValue defaultAnnotation;

    // The code data for this method. May be null
    private CodeData code;

    // The exception table (thrown exceptions) for this method. May be null
    private int[] exc_table = null;

    public MethodData(ClassData classData) {
        super(classData);
        super.memberType = "MethodData";
        setCommentOffset(classData.pool.getCommentOffset());
        lP = printLabelAsIdentifiers ? "L" : "";
        paramNameDates = null;
    }

    /* Read Methods */
    @Override
    protected boolean handleAttributes(DataInputStream in, EAttribute attributeTag, int attributeLength) throws IOException {
        // Read the Attributes
        boolean handled = true;
        switch (attributeTag) {
            case ATT_Code -> {
                code = new CodeData(this);
                code.read(in, attributeLength);
            }
            case ATT_Signature -> {
                if (signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "method_info");
                }
                signature = new SignatureData(data).read(in, attributeLength);
            }
            case ATT_Exceptions -> readExceptions(in);
            case ATT_MethodParameters -> readMethodParameters(in);
            case ATT_RuntimeVisibleParameterAnnotations, ATT_RuntimeInvisibleParameterAnnotations -> {
                boolean invisible = (attributeTag == EAttribute.ATT_RuntimeInvisibleParameterAnnotations);
                ParameterAnnotationData parameterAnnotationData = new ParameterAnnotationData(this, invisible);
                parameterAnnotationData.read(in);
                if (invisible) {
                    invisibleParameterAnnotations = parameterAnnotationData;
                } else {
                    visibleParameterAnnotations = parameterAnnotationData;
                }
            }
            case ATT_AnnotationDefault -> defaultAnnotation = AnnotationElement.readValue(in, data, false);
            default -> handled = false;
        }
        return handled;
    }

    /**
     * Read and resolve the method data called from ClassData.
     * Precondition: NumFields has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        access = in.readUnsignedShort(); // & MM_METHOD; // Q
        name_cpx = in.readUnsignedShort();
        sig_cpx = in.readUnsignedShort();
        environment.traceln("MethodData: {modifiers[%d]}:%s name[%d]=%s signature[%d]=%s",
                access,
                EModifier.asNames(access, ClassFileContext.METHOD).isEmpty() ? "<none>" :
                        EModifier.asNames(access, ClassFileContext.METHOD).isEmpty(),
                name_cpx, data.pool.getString(name_cpx, index -> "???"),
                sig_cpx, data.pool.getString(sig_cpx, index -> "???"));
        // Read the attributes
        readAttributes(in);
    }

    private void readExceptions(DataInputStream in) throws IOException {
        // this is not really a CodeAttr attribute, it's part of the CodeAttr
        int exc_table_len = in.readUnsignedShort();
        environment.traceln("ExceptionsAttr[%d]", exc_table_len);
        exc_table = new int[exc_table_len];
        for (int l = 0; l < exc_table_len; l++) {
            int exc = in.readShort();
            environment.traceln("throws: #" + exc);
            exc_table[l] = exc;
        }
    }

    private void readMethodParameters(DataInputStream in) throws IOException {
        // this is not really a CodeAttr attribute, it's part of the CodeAttr
        int num_params = in.readUnsignedByte();
        environment.traceln("MethodParametersAttr[%d]", num_params);
        paramNameDates = new ArrayList<>(num_params);
        for (int i = 0; i < num_params; i++) {
            short paramNameCpx = (short) in.readUnsignedShort();
            int paramAccess = in.readUnsignedShort();
            environment.traceln("Param[%d] = { name[%d]: \"%s\" modifiers[%d]: %s}", i, paramNameCpx,
                    pool.getString(paramNameCpx, index -> "???"),
                    paramAccess, EModifier.asNames(paramAccess, ClassFileContext.METHOD));
            paramNameDates.add(i, new ParamNameData(paramNameCpx, paramAccess));
        }
    }

    /**
     * prints the parameter annotations for this method. called from CodeAttr (since JASM
     * code integrates the PAnnotation Syntax inside the method body).
     */
    public void printPAnnotations() throws IOException {
        int visSize = 0;
        int invisSize = 0;
        int pNumSize = 0;

        if (visibleParameterAnnotations != null) {
            visSize = visibleParameterAnnotations.numParams();
        }
        if (invisibleParameterAnnotations != null) {
            invisSize = invisibleParameterAnnotations.numParams();
        }
        if (paramNameDates != null) {
            pNumSize = paramNameDates.size();
        }

        int maxParams;
        maxParams = max(pNumSize, invisSize);
        maxParams = max(visSize, maxParams);

        String[] paramNames = getPrintableParameterNames(maxParams);
        int annotOffset = Arrays.stream(paramNames).mapToInt(name -> name == null ? 0 : name.length()).max().orElse(0) + 1;

        for (int paramNum = 0; paramNum < maxParams; paramNum++) {
            ArrayList<AnnotationData> visAnnotationDataList = (visibleParameterAnnotations != null && paramNum < visSize) ?
                    visibleParameterAnnotations.get(paramNum) : null;

            ArrayList<AnnotationData> invisAnnotationDataList = (invisibleParameterAnnotations != null && paramNum < invisSize) ?
                    invisibleParameterAnnotations.get(paramNum) : null;

            ParamNameData paramNameData = (paramNameDates != null) ? paramNameDates.get(paramNum) : null;
            boolean hasAnnotations = ((visAnnotationDataList != null) || (invisAnnotationDataList != null));

            if (paramNameData != null && paramNameData.name_cpx == 0) {
                paramNameData = null;
            }

            if ((paramNameData != null) || hasAnnotations) {

                // Print the Param number (header)
                printIndent(PadRight(paramNum + ": ", 5));

                int offset = annotOffset + 4;
                // Print the Parameter name
                if (paramNameData != null) {
                    printPadRight(paramNames[paramNum], annotOffset);
                    offset++;
                }

                // Print any visible param annotations
                boolean firstTime = printAnnotationDataList(visAnnotationDataList, true, offset);

                // Print any invisible param annotations
                printAnnotationDataList(invisAnnotationDataList, firstTime, annotOffset);

                // Reset the line, if there were parameters
                println();
            }
        }
    }

    /**
     * Prints a list of Visible/Invisible parameter annotations
     */
    private boolean printAnnotationDataList(List<AnnotationData> annotationDataList, boolean firstTime, int offset)
            throws IOException {
        if (annotationDataList != null) {
            for (AnnotationData annot : annotationDataList) {
                if (!firstTime) {
                    println().print(enlargedIndent(offset));
                } else {
                    firstTime = false;
                }
                annot.setElementState(PARAMETER_ANNOTATION).setOffset(offset + getIndentSize()).print();
            }
        }
        return firstTime;
    }

    private String[] getPrintableParameterNames(int maxParams) {
        String[] names = new String[maxParams];
        if (paramNameDates != null) {
            for (int paramNum = 0; paramNum < maxParams; paramNum++) {
                ParamNameData paramNameData = paramNameDates.get(paramNum);
                if (paramNameData == null || paramNameData.name_cpx == 0) {
                    names[paramNum] = "";
                    continue;
                }
                // get printable parameter name
                names[paramNum] = Token.PARAM_NAME.parseKey() + "{ " +
                        data.pool.getString(paramNameData.name_cpx, index -> "#" + index) + ' ' +
                        EModifier.asKeywords(paramNameData.access, ClassFileContext.METHOD_PARAMETERS) +
                        "}";
            }
        }
        return names;
    }

    /**
     * Prints the method data to the current output stream. called from ClassData.
     */
    @Override
    public void print() throws IOException {

        printAnnotations();

        String methSignature = EModifier.asKeywords(access, ClassFileContext.METHOD);
        // add synthetic, deprecated if necessary
        methSignature = methSignature.concat(getPseudoFlagsAsString());
        methSignature = methSignature.concat(Token.METHODREF.parseKey() + " ");

        Pair<String, String> signInfo = (signature != null) ?
                signature.getPrintInfo((i) -> pool.inRange(i)) :
                new Pair<>("", "");

        boolean extraMethodInfo = code != null || exc_table != null || defaultAnnotation != null;
        int newLineIdent;
        if (printCPIndex) {
            // print the CPX method descriptor
            methSignature = methSignature.concat("#" + name_cpx + ":#" + sig_cpx + signInfo.first + (extraMethodInfo ? "" : ";"));
            if (skipComments) {
                if (defaultAnnotation != null) {
                    printIndent(PadRight(methSignature, getCommentOffset() - 1));
                } else {
                    printIndent(methSignature);
                }
                newLineIdent = methSignature.length();
            } else {
                printIndent(PadRight(methSignature, getCommentOffset() - 1));
                String comment = (defaultAnnotation != null ? " /* " : " // ").
//                  concat(String.format("0x%04X ", access)).
                    concat(data.pool.getName(name_cpx) + ":" + data.pool.getName(sig_cpx) + signInfo.second).
                    concat(defaultAnnotation != null ? " */ " : " ");
                newLineIdent = getCommentOffset() + comment.length() - 1;
                print(comment);
            }
        } else {
            methSignature = methSignature.concat(data.pool.getName(name_cpx) + ":").
                    concat(data.pool.getName(sig_cpx) + signInfo.second + (extraMethodInfo ? " " : ";"));
            printIndent(methSignature);
            newLineIdent = methSignature.length();
        }

        // followed by default annotation
        if (defaultAnnotation != null) {
            defaultAnnotation.setCommentOffset(newLineIdent);
            defaultAnnotation.setElementState(HAS_DEFAULT_VALUE);
            defaultAnnotation.print();
            print(((code == null && exc_table == null) ? ";" : " "));
        }
        // followed by exception table
        if (exc_table != null) {
            printExceptionTable(code == null);
        }

        if (code != null) {
            code.setCommentOffset(this.getCommentOffset());
            code.print();
        } else {
            if (exc_table != null) {
                print(";");
            }
            println();
        }
    }

    private void printExceptionTable(boolean abstractMethod) {
        String indexes = "",
                names = "",
                throwsClause = PadRight(Token.THROWS.parseKey(), PROGRAM_COUNTER_PLACEHOLDER_LENGTH);
        for (int i : exc_table) {
            if (printCPIndex)
                indexes = indexes.concat(indexes.isEmpty() ? "" : ", ").concat("#" + i);
            names = names.concat(names.isEmpty() ? "" : ", ").concat(data.pool.getClassName(i));
        }
        println().incIndent();
        if (printCPIndex) {
            if (skipComments) {
                printIndent(throwsClause + indexes + (abstractMethod ? ";" : ""));
            } else {
                printIndent(PadRight(throwsClause +
                        indexes +
                        (abstractMethod ? ";" : ""), getCommentOffset() - getIndentStep() - 1)).
                        print(" // " + names);
            }
        } else {
            printIndent(throwsClause + names);
        }
        decIndent();
    }

    /**
     * MethodParamData
     */
    static class ParamNameData {

        public int access;
        public int name_cpx;

        public ParamNameData(int name, int access) {
            this.access = access;
            this.name_cpx = name;
        }
    }
} // end MethodData
