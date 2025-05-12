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
import org.openjdk.asmtools.jdis.notations.Signature;
import org.openjdk.asmtools.jdis.notations.Type;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static org.openjdk.asmtools.common.structure.ClassFileContext.METHOD;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.METHOD_DATA;
import static org.openjdk.asmtools.jasm.TableFormatModel.Token.SIGNATURE;
import static org.openjdk.asmtools.jdis.ConstantPool.funcInvalidCPIndex;
import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.HAS_DEFAULT_VALUE;
import static org.openjdk.asmtools.jdis.MemberData.AnnotationElementState.PARAMETER_ANNOTATION;

/**
 * Method data for method members in a class of the Java Disassembler
 */
public class MethodData extends MemberData<ClassData> {

    //ConstantPool index to the method name
    protected int name_cpx;

    //ConstantPool index to the method descriptor, representing the types of parameters that the method takes,
    // and a return descriptor, representing the type of the value (if any) that the method returns.
    protected int descriptor_cpx;

    //The parameter names for this method
    protected ArrayList<MethodParameterData> methodParameters;

    //The visible parameter annotations for this method
    protected ParameterAnnotationData visibleParameterAnnotations;

    //The invisible parameter annotations for this method
    protected ParameterAnnotationData invisibleParameterAnnotations;

    // The invisible parameter annotations for this method
    protected AnnotationElement.AnnotationValue defaultAnnotation;

    // The exception table (thrown exceptions) for this method. Maybe null
    protected ExceptionData exceptions;

    // The code data for this method. Maybe null
    private CodeData code;

    public MethodData(ClassData classData) {
        super(classData);
        tableToken = METHOD_DATA;
        super.memberType = "MethodData";
        setCommentOffset(classData.pool.getCommentOffset());
        methodParameters = null;
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
                if (this.signature != null) {
                    environment.warning("warn.one.attribute.required", "Signature", "method_info");
                }
                setSignature(new SignatureData(data).read(in, attributeLength));
            }
            case ATT_Exceptions -> exceptions = new ExceptionData(data).read(in, attributeLength);
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
     * Precondition: Meth has already been read from the stream.
     */
    public void read(DataInputStream in) throws IOException {
        // read the Methods CP indexes
        access = in.readUnsignedShort(); // & MM_METHOD; // Q
        name_cpx = in.readUnsignedShort();
        descriptor_cpx = in.readUnsignedShort();
        environment.traceln(() -> "MethodData: {modifiers[%d]}:%s name[%d]=%s signature[%d]=%s"
                .formatted(access,
                        EModifier.asNames(access, ClassFileContext.METHOD).isEmpty() ? "<none>" :
                                EModifier.asNames(access, ClassFileContext.METHOD).isEmpty(),
                        name_cpx, data.pool.getString(name_cpx, index -> "???"),
                        descriptor_cpx, data.pool.getString(descriptor_cpx, index -> "???")));
        // Read the attributes
        readAttributes(in);
    }

    private void readMethodParameters(DataInputStream in) throws IOException {
        // this is not really a CodeAttr attribute, it's part of the CodeAttr
        int num_params = in.readUnsignedByte();
        environment.traceln(() -> "MethodParametersAttr[%d]".formatted(num_params));
        methodParameters = new ArrayList<>(num_params);
        for (int i = 0; i < num_params; i++) {
            short paramNameCpx = (short) in.readUnsignedShort();
            int paramAccess = in.readUnsignedShort();
            environment.traceln("()->MethodParameter[%d] = { name[%d]: \"%s\" modifiers[%d]: %s}".
                    formatted(i, paramNameCpx,
                            pool.getString(paramNameCpx, index -> "???"),
                            paramAccess, EModifier.asNames(paramAccess, ClassFileContext.METHOD)));
            methodParameters.add(i, new MethodParameterData(paramNameCpx, paramAccess));
        }
    }

    private record ParameterAnnotationsSizes(int visibleParameterAnnotationsCount,
                                             int invisibleParameterAnnotationsCount) {
        boolean hasParameterAnnotations() {
            return visibleParameterAnnotationsCount > 0 || invisibleParameterAnnotationsCount > 0;
        }
    }

    private ParameterAnnotationsSizes parameterAnnotationsSizes = null;

    private ParameterAnnotationsSizes ParameterAnnotationsSizes() {
        if (parameterAnnotationsSizes == null) {
            parameterAnnotationsSizes = new ParameterAnnotationsSizes(
                    visibleParameterAnnotations != null ? visibleParameterAnnotations.numParameters() : 0,
                    invisibleParameterAnnotations != null ? invisibleParameterAnnotations.numParameters() : 0);
        }
        return parameterAnnotationsSizes;
    }

    private boolean hasAnnotationParameters() {
        return ParameterAnnotationsSizes().hasParameterAnnotations() ||
                methodParameters != null && methodParameters.size() > 0;
    }

    private boolean hasDefaultAnnotation() {
        return defaultAnnotation != null;
    }

    /**
     * Print The MethodParameters Attribute and the parameter annotations for this method.
     * Called from CodeAttr (since JASM code integrates the ParameterAnnotation Syntax inside the method body).
     */
    public void printMethodParameters() throws IOException {
        // ParameterAnnotation(s) or MethodParameters found.
        if (hasAnnotationParameters()) {
            incIndent();
            int totalWidth = printProgramCounter ? 7 : 5;
            int pNumSize = methodParameters != null ? methodParameters.size() : 0;
            int maxParams = max(pNumSize, parameterAnnotationsSizes.invisibleParameterAnnotationsCount());
            maxParams = max(parameterAnnotationsSizes.visibleParameterAnnotationsCount(), maxParams);

            String[] paramNames = getPrintableParameterNames(maxParams);

            for (int paramNum = 0; paramNum < maxParams; paramNum++) {
                ArrayList<AnnotationData> visAnnotationDataList =
                        (visibleParameterAnnotations != null && paramNum < parameterAnnotationsSizes.visibleParameterAnnotationsCount()) ?
                                visibleParameterAnnotations.get(paramNum) : null;

                ArrayList<AnnotationData> invisAnnotationDataList =
                        (invisibleParameterAnnotations != null && paramNum < parameterAnnotationsSizes.invisibleParameterAnnotationsCount()) ?
                                invisibleParameterAnnotations.get(paramNum) : null;

                MethodParameterData methodParameterData = (methodParameters != null) ? methodParameters.get(paramNum) : null;
                boolean hasAnnotations = ((visAnnotationDataList != null) || (invisAnnotationDataList != null));

                if ((methodParameterData != null) || hasAnnotations) {
                    // Print the Param number (header)
                    int annotOffset = 3;
                    printIndent(PadRight("%2d: ".formatted(paramNum), totalWidth));

                    // Print the Parameter name
                    if (methodParameterData != null) {
                        printPadRight(paramNames[paramNum], annotOffset);
                    }

                    // Print any visible param annotations
                    printAnnotationDataList(visAnnotationDataList, annotOffset);

                    // Print any invisible param annotations
                    printAnnotationDataList(invisAnnotationDataList, annotOffset);

                    // Reset the line if there were parameters
                    println();
                }
            }
            decIndent();
        }
    }

    /**
     * Prints a list of Visible/Invisible parameter annotations
     */
    private void printAnnotationDataList(List<AnnotationData> annotationDataList, int offset)
            throws IOException {
        if (annotationDataList != null) {
            for (AnnotationData annot : annotationDataList) {
                println().print(enlargedIndent(offset));
                annot.setElementState(PARAMETER_ANNOTATION).setOffset(offset + getIndentSize()).print();
            }
        }
    }

    private String[] getPrintableParameterNames(int maxParams) {
        String[] names = new String[maxParams];
        if (methodParameters != null) {
            for (int paramNum = 0; paramNum < maxParams; paramNum++) {
                MethodParameterData methodParameterData = methodParameters.get(paramNum);
                // get printable parameter name
                names[paramNum] = Token.PARAM_NAME.parseKey() + "{ ";
                if (printCPIndex) {
                    names[paramNum] += "#%d ".formatted(methodParameterData.name_cpx);
                    if (!skipComments && methodParameterData.name_cpx != 0) {
                        names[paramNum] += "/* %s */ ".formatted(data.pool.getString(methodParameterData.name_cpx, index -> "#" + index));
                    }
                    if (methodParameterData.access != 0) {
                        names[paramNum] += EModifier.asKeywords(methodParameterData.access, ClassFileContext.METHOD_PARAMETERS);
                    }
                } else {
                    if (methodParameterData.name_cpx != 0) {
                        names[paramNum] += data.pool.getString(methodParameterData.name_cpx, index -> "#" + index) + " ";
                    } else {
                        names[paramNum] += "#0 ";
                    }
                    if (methodParameterData.access != 0) {
                        names[paramNum] += EModifier.asKeywords(methodParameterData.access, ClassFileContext.METHOD_PARAMETERS);
                    }
                }
                names[paramNum] += "}";
            }
        }
        return names;
    }

    private String getMethodModifiers() {
        return EModifier.asKeywords(access, ClassFileContext.METHOD).
                // add synthetic, deprecated if necessary
                        concat(getPseudoFlagsAsString());
    }

    /**
     * Prints the method data to the current output stream. Called from ClassData.
     */
    @Override
    protected void jasmPrint(int index, int size) throws IOException {
        boolean isSignaturePrintable = this.signature != null && this.signature.isPrintable();
        boolean tableSignatureFormat = isSignaturePrintable && this.signature.isTableOutput();
        boolean hasExceptions = exceptions != null;
        boolean hasCodeInfo = code != null || hasAnnotationParameters() || hasExceptions;
        boolean noExtraInfo = !hasCodeInfo && !tableSignatureFormat;
        if (index > 0) {
            // Print empty line between methods
            println();
        }
        printSysInfo();
        super.printAnnotations(visibleAnnotations, invisibleAnnotations);
        super.printAnnotations(visibleTypeAnnotations, invisibleTypeAnnotations);
        String methSignature = getMethodModifiers();
        methSignature = methSignature.concat(Token.METHODREF.parseKey());
        methSignature = PadRight(methSignature, max(methSignature.length() + 1, SIGNATURE.parseKey().length() + getIndentSize() * 2));
        int keywordPadding = methSignature.length() - getIndentSize();
        // get JASM Signature info
        Pair<String, String> jasmSignInfo = (isSignaturePrintable) ?
                signature.getJasmPrintInfo((i) -> pool.inRange(i)) :
                new Pair<>("", "");

        if (printCPIndex) {
            // print the CPX method descriptor
            methSignature = methSignature.concat("#%d:#%d".formatted(name_cpx, descriptor_cpx)).concat(jasmSignInfo.first);
            if (noExtraInfo) {
                methSignature = methSignature.concat(";");
            }
            if (skipComments) {
                if (defaultAnnotation != null) {
                    printIndent(PadRight(methSignature, getCommentOffset()));
                } else {
                    printIndent(methSignature);
                }
            } else {
                printIndent(PadRight(methSignature, getCommentOffset()));
                String comment = (defaultAnnotation != null ? " /* " : " // ").
                        concat("%s:%s".formatted(data.pool.getName(name_cpx), data.pool.getName(descriptor_cpx), jasmSignInfo.second)).
                        concat(jasmSignInfo.second).
                        concat(defaultAnnotation != null ? " */ " : " ");
                print(comment);
            }
        } else {
            methSignature = methSignature.concat(data.pool.getName(name_cpx) + ":").
                    concat(data.pool.getName(descriptor_cpx)).
                    concat(jasmSignInfo.second);
            if (noExtraInfo) {
                methSignature = methSignature.concat(";");
            } else if (!hasAnnotationParameters() && tableSignatureFormat) {
                methSignature = methSignature.concat(": ");
            } else {
                methSignature = methSignature.concat(" ");
            }
            printIndent(methSignature);
        }

        // followed by default annotation (JLS 9.6.2)
        // public abstract Method #7:#8       /* ivalue:"()I" */ default { #10 /* 1 */ };
        if (hasDefaultAnnotation()) {
            // printIndent(PadRight(methSignature, getCommentOffset()));
            defaultAnnotation.incIndent().setCommentOffset(getIndentSize() - getIndentStep());
            defaultAnnotation.setElementState(HAS_DEFAULT_VALUE);
            defaultAnnotation.print();
            // finish up the method declaration
            if (noExtraInfo) {
                print(";");
            }
        }

        if (tableSignatureFormat) {
            // print separately
            println();
            signature.disableNewLine().setKeywordPadding(keywordPadding).incIndent().
                    setCommentOffset(this.getCommentOffset() - getIndentStep());
            signature.print();
        }

        // followed by exception table
        if (exceptions != null) {
            println();
            exceptions.setKeywordPadding(keywordPadding).incIndent().setCommentOffset(this.getCommentOffset() - getIndentStep());
            exceptions.print();
        } else {
            println();
        }

        if (code != null) {
            code.setCommentOffset(this.getCommentOffset());
            code.print();
        } else {
            if (hasAnnotationParameters()) {
                printMethodParameters();
            } else if (index == size - 1) {
                println();
            }
        }
    }

    @Override
    public void tablePrint(int index, int size) throws IOException {
        //There are no differences between the simple (jasm) and extended (table) presentations of record_component_info.
        jasmPrint(index, size);
    }


    @Override
    protected void printSysInfo() {
        if (sysInfo) {
            int paramCount = EModifier.isStatic(access) ? 0 : 1;
            String prefix = getIndentString() + " *  ";
            String methodModifiers = getMethodModifiers();
            Type signatureType = signature != null ? signature.getSignatureType() :
                    new Signature<>(environment.getLogger(), descriptor_cpx).getType(pool);
            String methodName = data.pool.getString(name_cpx, funcInvalidCPIndex);
            boolean isConstructor = methodName.equals("<init>");
            String methodSignature = signatureType.toString();
            if (signatureType instanceof Type.MethodType methodType) {
                paramCount += methodType.paramTypes.size();
            }
            int i = 0;
            if (isConstructor) {
                methodName = data.getClassName();
                methodSignature = methodSignature.substring(methodSignature.indexOf("("));
            } else {
                i = methodSignature.indexOf('(');
            }
            methodName = methodSignature.substring(0, i).concat(methodName).
                    concat(methodSignature.substring(i)).replaceAll("/", ".");
            String descriptor = data.pool.getString(descriptor_cpx, funcInvalidCPIndex);

            printIndentLn("/**");
            println(prefix + methodModifiers + methodName);
            prefix = prefix.concat(getIndentString());
            println(prefix + "descriptor: " + descriptor);
            if (signature != null) {
                println(prefix + "signature:  " + data.pool.getString(signature.getCPIndex(), funcInvalidCPIndex));
            }
            println(prefix + "flags: (0x%04x) %s".formatted(access, EModifier.asNames(access, METHOD)));
            println(prefix + "stack: %d, locals: %d, args_size: %d".formatted(this.code.max_stack,
                    this.code.max_locals, paramCount));
            printIndentLn(" */");
        }
    }

    /**
     * MethodParamData
     */
    static class MethodParameterData {

        public int access;
        public int name_cpx;

        public MethodParameterData(int name, int access) {
            this.access = access;
            this.name_cpx = name;
        }
    }
} // end MethodData
