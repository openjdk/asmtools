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


import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.EAttribute;
import org.openjdk.asmtools.jasm.TypeAnnotationTargetInfoData.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.openjdk.asmtools.jasm.ClassFileConst.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.AnnotationElementType.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;
import static org.openjdk.asmtools.jasm.ConstantPool.ConstValue_UTF8;
import static org.openjdk.asmtools.jasm.JasmTokens.AnnotationType.isInvisibleAnnotationToken;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jasm.TypeAnnotationTypes.*;

/**
 * ParserAnnotation
 * <p>
 * ParserAnnotation is a parser class owned by Parser.java. It is primarily responsible
 * for parsing Annotations (for classes, methods or fields).
 * <p>
 * ParserAnnotation can parse the different types of Annotation Attributes:
 * Runtime(In)Visible Annotations (JDK 6+) Default Annotations (JDK 6+)
 * Runtime(In)VisibleParameter Annotations (JDK 7+) Runtime(In)VisibleType Annotations
 * (JSR308, JDK8+)
 */
public class ParserAnnotation extends ParseBase {

    /**
     * local handles on the scanner, main parser, and the error reporting env
     */
    private static final TargetTypeVisitor targetTypeVisitor = new TargetTypeVisitor();

    protected ParserAnnotation(Parser parentParser) {
        super.init(parentParser);
        targetTypeVisitor.init(scanner);
    }

    protected void scanParamName(int totalParams, int paramNum, MethodData curMethod) throws SyntaxError {
        scanner.debugScan(" - - - > [ParserAnnotation.scanParamName]: Begin ");
        scanner.scan();
        scanner.expect(Token.LBRACE);
        // First scan the Name (String, or CPX to name)
        ConstCell nameCell;
        if ((scanner.token == Token.IDENT) || scanner.checkTokenIdent()) {
            // Got a Class Name
            nameCell = parser.parseName();
        } else if (scanner.token == Token.CPINDEX) {
            int cpx = scanner.intValue;
            nameCell = parser.pool.getCell(cpx);
            // check the constant
            ConstValue nameCellValue = nameCell.ref;
            if (!(nameCellValue instanceof ConstValue_UTF8)) {
                // throw an error
                environment.error(scanner.pos, "err.paramname.constnum.invaltype", cpx);
                throw new SyntaxError();
            }
        } else {
            // throw scan error - unexpected token
            environment.error(scanner.pos, "err.paramname.token.unexpected", scanner.stringValue);
            throw new SyntaxError();
        }

        // Got the name cell. Next, scan the access flags
        int mod = parser.scanModifiers();

        scanner.expect(Token.RBRACE);

        curMethod.addMethodParameter(totalParams, paramNum, nameCell, mod);

        scanner.debugScan(" - - - > [ParserAnnotation.scanParamName]: End ");
    }

    /**
     * The main entry for parsing an annotation list.
     *
     * @return An ArrayList of parsed annotations
     */
    ArrayList<AnnotationData> scanAnnotations() throws SyntaxError {
        ArrayList<AnnotationData> list = new ArrayList<>();
        while (scanner.token == Token.ANNOTATION) {
            if (JasmTokens.AnnotationType.isAnnotationToken(scanner.stringValue)) {
                list.add(parseAnnotation());
            } else if (JasmTokens.AnnotationType.isTypeAnnotationToken(scanner.stringValue)) {
                list.add(parseTypeAnnotation());
            } else {
                return null;
            }
        }
        return (list.size() > 0) ? list : null;
    }

    /**
     * parseDefaultAnnotation
     * <p>
     * parses a default Annotation attribute
     *
     * @return the parsed Annotation Attribute
     * @throws SyntaxError
     * @throws SyntaxError if a scanner error occurs
     */
    protected DefaultAnnotationAttr parseDefaultAnnotation() throws SyntaxError {
        scanner.scan();
        DefaultAnnotationAttr attr;
        DataWriter value = null;
        scanner.expect(Token.LBRACE);

        if ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            value = scanAnnotationData("default");
        }
        scanner.expect(Token.RBRACE);
        attr = new DefaultAnnotationAttr(parser.pool, EAttribute.ATT_AnnotationDefault, value);
        return attr;
    }

    /**
     * Parses Parameter Annotations attributes.
     *
     * @param totalParams
     * @param curMethod
     * @throws SyntaxError if a scanner error occurs
     */
    protected void parseParamAnnots(int totalParams, MethodData curMethod) throws SyntaxError {
        scanner.debugScan(" - - - > [ParserAnnotation.parseParamAnnots]: Begin, totalParams =  " + totalParams + " ");
        // The _method thinks there are N+1 params in the signature
        // (N = total params in the call list) + 1 (return value)
        // int totalParams = totalParams - 1;
        TreeMap<Integer, ArrayList<AnnotationData>> pAnnots = new TreeMap<>();

        while (scanner.token == INTVAL) {
            // Create the Parameter Array for  Param Annotations

            // Do something with Parameter annotations
            // --------------------
            // First - validate that the parameter number (integer)
            // (eg >= 0, < numParams, and param num is not previously set)
            int paramNum = scanner.intValue;
            Integer iParamNum = Integer.valueOf(paramNum);
            if (paramNum < 0 || paramNum >= totalParams) {
                //invalid Parameter number.  Throw an error.
                environment.error(scanner.pos, "err.invalid.paramnum", paramNum);
            }
            if (pAnnots.get(iParamNum) != null) {
                // paramter is already populated with annotations/pnames, Throw an error.
                environment.error(scanner.pos, "err.duplicate.paramnum", paramNum);
            }
            // 2nd - Parse the COLON (invalid if not present)
            scanner.scan();
            scanner.expect(Token.COLON);

            // 3rd - parse either an optional ParamName, or a list of annotations
            if (scanner.token == Token.PARAM_NAME) {
                //parse the ParamName
                scanParamName(totalParams, iParamNum, curMethod);
            }

            // 4th - parse each Annotation (followed by comma, followed by annotation
            //       assign array of annotations to param array
            if (scanner.token == Token.ANNOTATION) {
                ArrayList<AnnotationData> pAnnot = scanAnnotations();
                pAnnots.put(iParamNum, pAnnot);

                for (AnnotationData data : pAnnot) {
                    curMethod.addParamAnnotation(totalParams, paramNum, data);
                }
            }
        }
    }

    /**
     * parseTypeAnnotation - parses an individual annotation.
     *
     * @return a parsed annotation.
     * @throws SyntaxError if a scanner error occurs
     */
    private AnnotationData parseTypeAnnotation() throws SyntaxError {
        boolean invisible = isInvisibleAnnotationToken(scanner.stringValue);
        scanner.scan();
        scanner.debugScan("     [ParserAnnotation.parseTypeAnnotation]: id = " + scanner.stringValue + " ");
        String annoName = "L" + scanner.stringValue + ";";
        TypeAnnotationData anno = new TypeAnnotationData(parser.pool.findUTF8Cell(annoName), invisible);
        scanner.scan();
        scanner.debugScan("     [ParserAnnotation.parseTypeAnnotation]:new Type annotation: " + annoName + " ");

        scanner.expect(Token.LBRACE);

        // Scan the usual annotation data
        _scanAnnotation(anno);

        // scan the Target (u1: target_type, union{...}: target_info)
        _scanTypeTarget(anno);

        if (scanner.token != Token.RBRACE) {
            // scan the Location (type_path: target_path)
            _scanTargetPath(anno);
        }

        scanner.expect(Token.RBRACE);
        return anno;
    }

    /**
     * Parses an individual annotation.
     *
     * @return a parsed annotation.
     * @throws SyntaxError if a scanner error occurs
     */
    private AnnotationData parseAnnotation() throws SyntaxError {
        scanner.debugScan(" - - - > [ParserAnnotation.parseAnnotation]: Begin ");
        boolean invisible = isInvisibleAnnotationToken(scanner.stringValue);
        scanner.scan();
        String annoName = "L" + scanner.stringValue + ";";

        AnnotationData anno = new AnnotationData(parser.pool.findUTF8Cell(annoName), invisible);
        scanner.scan();
        scanner.debugScan("[ParserAnnotation.parseAnnotation]: new annotation: " + annoName);
        _scanAnnotation(anno);

        return anno;
    }

    /**
     * Parses an individual annotation-data.
     *
     * @return a parsed annotation.
     * @throws SyntaxError if a scanner error occurs
     */
    private void _scanAnnotation(AnnotationData annotData) throws SyntaxError {
        scanner.debugScan(" - - - > [ParserAnnotation._scanAnnotation]: Begin");
        scanner.expect(Token.LBRACE);

        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            ConstCell nameCell = parser.parseName();
            scanner.expect(Token.ASSIGN);
            if (nameCell.isSet()) {
                ConstValue refValue = nameCell.ref;
                if (refValue.tag != ConstType.CONSTANT_UTF8) {
                    throw new SyntaxError();
                }
                String name = refValue.asString();
                scanner.debugScan("     [ParserAnnotation._scanAnnotation]: Annot - Field Name: " + name);
                DataWriter dataWriter = scanAnnotationData(name);
                annotData.add(new AnnotationData.ElemValuePair(nameCell, dataWriter));
            } else {
                if (scanner.token == CPINDEX) {
                    ConstCell refCell = parser.parseName();
                    scanner.debugScan("     [ParserAnnotation._scanAnnotation]: " + nameCell.cpIndex + " = " + refCell.cpIndex);
                    annotData.add(new AnnotationData.ElemValuePair(nameCell, refCell));
                } else {
                    DataWriter dataWriter = scanAnnotationData("unknown");
                    annotData.add(new AnnotationData.ElemValuePair(nameCell, dataWriter));
                }
            }
            // consume tokens inbetween annotation fields
            if (scanner.token == Token.COMMA) {
                scanner.scan();
            }
        }
        scanner.expect(Token.RBRACE);
    }

    /**
     * Parses an individual annotation-data.
     *
     * @return a parsed annotation.
     * @throws SyntaxError if a scanner error occurs
     */
    private void _scanTypeTarget(TypeAnnotationData annotData) throws SyntaxError {
        scanner.debugScan("     [ParserAnnotation._scanTypeTarget]: Begin ");
        scanner.expect(Token.LBRACE);

        //Scan the target_type and the target_info
        scanner.expect(Token.IDENT);
        scanner.debugScan("     [ParserAnnotation._scanTypeTarget]: TargetType: " + scanner.idValue);
        ETargetType targetType = ETargetType.getTargetType(scanner.idValue);
        if (targetType == null) {
            environment.error(scanner.pos, "err.incorrect.typeannot.target", scanner.idValue);
            throw new SyntaxError();
        }

        scanner.debugScan("     [ParserAnnotation._scanTypeTarget]: Got TargetType: " + targetType);

        if (targetTypeVisitor.scanner == null) {
            targetTypeVisitor.scanner = scanner;
        }
        targetTypeVisitor.visitExcept(targetType);

        annotData.targetInfo = targetTypeVisitor.getTargetInfo();
        annotData.targetType = targetType;
        scanner.debugScan("     [ParserAnnotation._scanTypeTarget]: Got TargetInfo: " + annotData.targetInfo);

        scanner.expect(Token.RBRACE);
    }

    /**
     * _scanTargetPath
     * <p>
     * parses and fills the type_path structure (4.7.20.2)
     * <p>
     * type_path {
     * u1 path_length;
     * {   u1 type_path_kind;
     * u1 type_argument_index;
     * } path[path_length];
     * }
     *
     * @throws SyntaxError if a scanner error occurs
     */
    private void _scanTargetPath(TypeAnnotationData annotData) throws SyntaxError {
        // parse the location info
        scanner.expect(Token.LBRACE);

        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            TypePathEntry tpe = _scanTypePathEntry();
            annotData.addTypePathEntry(tpe);
            // throw away comma
            if (scanner.token == Token.COMMA) {
                scanner.scan();
            }
        }

        scanner.expect(Token.RBRACE);
    }

    /* ************************* Private Members  *************************** */

    /**
     * _scanTypeLocation
     * <p>
     * parses a path entry of the type_path.
     * <p>
     * {   u1 type_path_kind;
     * u1 type_argument_index;
     * }
     *
     * @return a parsed type path.
     * @throws SyntaxError if a scanner error occurs
     */
    private TypePathEntry _scanTypePathEntry() throws SyntaxError {
        TypePathEntry tpe;

        if ((scanner.token != Token.EOF) && scanner.token.possibleTypePathKind()) {
            EPathKind pathKind = EPathKind.getPathKind(scanner.stringValue);
            if (pathKind == EPathKind.TYPE_ARGUMENT) {
                scanner.scan();
                // need to scan the index
                // Take the form:  TYPE_ARGUMENT{#}
                scanner.expect(Token.LBRACE);
                int index;
                if ((scanner.token != Token.EOF) && (scanner.token == INTVAL)) {
                    index = scanner.intValue;
                    scanner.scan();
                } else {
                    // incorrect Arg index
                    environment.error(scanner.pos, "err.incorrect.typeannot.pathentry.argindex", scanner.token);
                    throw new SyntaxError();
                }
                tpe = new TypePathEntry(pathKind, index);
                scanner.expect(Token.RBRACE);
            } else {
                tpe = new TypePathEntry(pathKind, 0);
                scanner.scan();
            }
        } else {
            // unexpected Type Path
            environment.error(scanner.pos, "err.incorrect.typeannot.pathentry", scanner.token);
            throw new SyntaxError();
        }

        return tpe;
    }

    /**
     * scanAnnotationArray
     * <p>
     * Scans an Array of annotations.
     *
     * @param name Name of the annotation
     * @return Array Element
     * @throws SyntaxError if a scanner error occurs
     */
    private ArrayElemValue scanAnnotationArray(String name) throws SyntaxError {
        scanner.scan();
        ArrayElemValue arrayElem = new ArrayElemValue();

        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            DataWriter dataWriter = scanAnnotationData(name + " {}");
            arrayElem.add(dataWriter);

            // consume tokens inbetween annotation fields
            if (scanner.token == Token.COMMA) {
                scanner.scan();
            }
        }

        scanner.expect(Token.RBRACE);
        return arrayElem;
    }

    /**
     * Scans an annotation enumeration.
     *
     * @param name Annotation Name
     * @return Constant element value for the Class Annotation.
     * @throws SyntaxError if a scanner error occurs
     */
    private DataWriter scanAnnotationClass(String name) throws SyntaxError {
        DataWriter constVal;
        // scan the next identifier.
        // if it is an Ident, consume it as the class name.
        scanner.scan();
        switch (scanner.token) {
            case IDENT:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Constant Class Field: " + name + " = " + scanner.stringValue);
                //need to encode the stringval as an (internal) descriptor.
                String desc = parser.encodeClassString(scanner.stringValue);

                // note: for annotations, a class field points to a string with the class descriptor.
                constVal = new ConstElemValue(AE_CLASS.tag(), parser.pool.findUTF8Cell(desc));
                scanner.scan();
                break;
            case CPINDEX:
                // could be a reference to a class name
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Constant Class Field: " + name + " = " + scanner.stringValue);
                Integer ConstNmCPX = Integer.valueOf(scanner.stringValue);
                constVal = new ClassElemValue(parser.pool.getCell(ConstNmCPX));
                scanner.scan();
                break;
            default:
                environment.error(scanner.pos, "err.incorrect.annot.class", scanner.stringValue);
                throw new SyntaxError();
        }

        return constVal;
    }

    /**
     * Scans an annotation enum val.
     *
     * @param name Annotation Name
     * @return Enumeration Element Value
     * @throws SyntaxError if a scanner error occurs
     */
    private EnumElemValue scanAnnotationEnum(String name) throws SyntaxError {
        scanner.scan();
        EnumElemValue enumval = null;
        switch (scanner.token) {
            case IDENT:
                // could be a string identifying enum class and name
                String enumClassName = scanner.stringValue;
                scanner.scan();
                // could be a string identifying enum class and name
                switch (scanner.token) {
                    case IDENT:
                        // could be a string identifying enum class and name
                        String enumTypeName = scanner.stringValue;
                        environment.traceln("[ParserAnnotation.scanAnnotationEnum]:: Constant Enum Field: " + name + " = " + enumClassName + " " + enumTypeName);
                        String encodedClass = parser.encodeClassString(enumClassName);
                        ConstElemValue classConst = new ConstElemValue(AE_STRING.tag(), parser.pool.findUTF8Cell(encodedClass));
                        ConstElemValue typeConst = new ConstElemValue(AE_STRING.tag(), parser.pool.findUTF8Cell(enumTypeName));
                        enumval = new EnumElemValue(classConst.constCell, typeConst.constCell);
                        scanner.scan();
                        break;
                    default:
                        environment.error(scanner.pos, "err.incorrect.annot.enum", scanner.stringValue);
                        throw new SyntaxError();
                }
                break;
            case CPINDEX:
                int typeNmCPX = Integer.parseInt(scanner.stringValue);
                scanner.scan();
                //need two indexes to form a proper enum
                if (scanner.token == CPINDEX) {
                    int ConstNmCPX = Integer.parseInt(scanner.stringValue);
                    environment.traceln("[ParserAnnotation.scanAnnotationEnum]:: Enumeration Field: " + name + " = #" + typeNmCPX + " #" + ConstNmCPX);
                    enumval = new EnumElemValue(parser.pool.getCell(typeNmCPX), parser.pool.getCell(ConstNmCPX));
                    scanner.scan();
                } else {
                    environment.error(scanner.pos, "err.incorrect.annot.enum.cpx");
                    throw new SyntaxError();
                }
                break;
        }
        return enumval;
    }

    /**
     * scanAnnotationData
     * <p>
     * parses the internals of an annotation.
     *
     * @param name Annotation Name
     * @return a Data data structure containing the annotation data.
     * @throws IOException for scanning errors.
     */
    private DataWriter scanAnnotationData(String name) {
        DataWriter dataWriter;
        switch (scanner.token) {
            // This handles the Annotation types (as normalized in the constant pool)
            // Some primitive types (Boolean, char, short, byte) are identified by a keyword.
            case INTVAL:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Integer Field: " + name + " = " + scanner.intValue);
                dataWriter = new ConstElemValue(CONSTANT_INTEGER.getAnnotationElementTypeValue(),
                        parser.pool.findIntegerCell(scanner.intValue));
                scanner.scan();
                break;
            case DOUBLEVAL:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Double Field: " + name + " = " + scanner.doubleValue);
                double dval = scanner.doubleValue;
                Long val = Double.doubleToLongBits(dval);
                dataWriter = new ConstElemValue(CONSTANT_DOUBLE.getAnnotationElementTypeValue(),
                        parser.pool.findDoubleCell(val));
                scanner.scan();
                break;
            case FLOATVAL:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Float Field: " + name + " = " + scanner.floatValue);
                float fval = scanner.floatValue;
                Integer val1 = Float.floatToIntBits(fval);
                dataWriter = new ConstElemValue(CONSTANT_FLOAT.getAnnotationElementTypeValue(),
                        parser.pool.findFloatCell(val1));
                scanner.scan();
                break;
            case LONGVAL:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Long Field: " + name + " = " + scanner.longValue);
                dataWriter = new ConstElemValue(CONSTANT_LONG.getAnnotationElementTypeValue(),
                        parser.pool.findLongCell(scanner.longValue));
                scanner.scan();
                break;
            case STRINGVAL:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: String Field: " + name + " = " + scanner.stringValue);
                dataWriter = new ConstElemValue(CONSTANT_UTF8.getAnnotationElementTypeValue(),
                        parser.pool.findUTF8Cell(scanner.stringValue));
                scanner.scan();
                break;
            case CLASS:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Class) keyword: " + scanner.stringValue);
                dataWriter = scanAnnotationClass(name);
                break;
            case ENUM:
                // scan the next two identifiers (eg ident.ident), or 2 CPRefs.
                // if it is an Ident, use consume it as the class name.
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Enum) keyword: " + scanner.stringValue);
                dataWriter = scanAnnotationEnum(name);
                break;
            case IDENT:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: JASM Keyword: (annotation field name: " + name + ") keyword: " + scanner.stringValue);
                dataWriter = scanAnnotationIdent(scanner.stringValue, name);
                break;
            case ANNOTATION:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Annotation Field: " + name + " = " + scanner.stringValue);
                dataWriter = new AnnotationElemValue(parseAnnotation());
                break;
            case LBRACE:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Annotation Array Field: " + name);
                dataWriter = scanAnnotationArray(name);
                break;
            case CPINDEX:
                environment.traceln("[ParserAnnotation.scanAnnotationData]:: Constant Field by index: " + name + " = #" + scanner.stringValue);
                int cpIndex = Integer.parseInt(scanner.stringValue);
                dataWriter = getElementValueByCPIndex(cpIndex);
                scanner.scan();
                break;
            default:
                environment.error(scanner.pos, "err.incorrect.annot.token", scanner.token);
                throw new SyntaxError();
        }
        return dataWriter;
    }

    private DataWriter getElementValueByCPIndex(int cpIndex) {
        DataWriter dataWriter;
        ConstCell cell = parser.pool.getCell(cpIndex);
        ConstType type = cell.getType();
        if (type.oneOf(CONSTANT_UNKNOWN,
                CONSTANT_INTEGER, CONSTANT_FLOAT,
                CONSTANT_LONG, CONSTANT_DOUBLE,
                CONSTANT_UTF8)) {
            dataWriter = new ConstElemValue(type.getAnnotationElementTypeValue(), cell);
        } else {
            dataWriter = new ClassElemValue(parser.pool.getCell(cpIndex));
        }
        return dataWriter;
    }

    /**
     * scanAnnotationIdent
     * <p>
     * parses the identifier of an annotation.
     *
     * @param ident Basic Type identifier
     * @param name  Annotation Name
     * @return Basic Type Annotation data
     * @throws SyntaxError if a scanning error occurs
     */
    private DataWriter scanAnnotationIdent(String ident, String name) throws SyntaxError {
        // Handle JASM annotation Keyword Identifiers
        DataWriter dataWriter;
        BasicType type = getBasicType(ident);
        switch (type) {
            case T_BOOLEAN:
                // consume the keyword, get the value
                scanner.scan();
                switch (scanner.token) {
                    case INTVAL:
                        // Handle Boolean value in integer form
                        environment.traceln("Boolean Field: " + name + " = " + scanner.intValue);
                        int val = scanner.intValue;
                        if (val > 1 || val < 0) {
                            environment.traceln("Warning: Boolean Field: " + name + " value is not 0 or 1, value = " + scanner.intValue);
                        }
                        dataWriter = new ConstElemValue(AE_BOOLEAN.tag(), parser.pool.findIntegerCell(val));
                        scanner.scan();
                        break;
                    case IDENT:
                        // handle boolean value with true/false keywords
                        int val1;
                        switch (scanner.stringValue) {
                            case "true":
                                val1 = 1;
                                break;
                            case "false":
                                val1 = 0;
                                break;
                            default:
                                environment.error(scanner.pos, "err.incorrect.annotation",
                                        AE_BOOLEAN.printValue(),
                                        INTVAL.parseKey() + ", " +
                                                CPINDEX.parseKey() + ", " +
                                                TRUE.parseKey() + ", " +
                                                FALSE.parseKey(),
                                        scanner.stringValue);
                                throw new SyntaxError();
                        }
                        environment.traceln("Boolean Field: " + name + " = " + scanner.stringValue);
                        dataWriter = new ConstElemValue(AE_BOOLEAN.tag(), parser.pool.findIntegerCell(val1));
                        scanner.scan();
                        break;
                    case CPINDEX:
                        int cpIndex = Integer.parseInt(scanner.stringValue);
                        dataWriter = new ConstElemValue(AE_BOOLEAN.tag(), parser.pool.getCell(cpIndex));
                        scanner.scan();
                        break;
                    default:
                        environment.error(scanner.pos, "err.incorrect.annotation",
                                AE_BOOLEAN.printValue(),
                                INTVAL.parseKey() + ", " + CPINDEX.parseKey(),
                                scanner.stringValue);
                        throw new SyntaxError();
                }
                break;
            case T_BYTE:
                // consume the keyword, get the value
                scanner.scan();
                switch (scanner.token) {
                    case INTVAL:
                        environment.traceln("Byte Field: " + name + " = " + scanner.intValue);
                        int val = scanner.intValue;
                        if (val > 0xFF) {
                            environment.traceln("Warning: Byte Field: " + name + " value is greater than 0xFF, value = " + scanner.intValue);
                        }
                        dataWriter = new ConstElemValue(AE_BYTE.tag(), parser.pool.findIntegerCell(val));
                        scanner.scan();
                        break;
                    case CPINDEX:
                        int cpIndex = Integer.parseInt(scanner.stringValue);
                        dataWriter = new ConstElemValue(AE_BYTE.tag(), parser.pool.getCell(cpIndex));
                        scanner.scan();
                        break;
                    default:
                        environment.error(scanner.pos, "err.incorrect.annotation",
                                AE_BYTE.printValue(),
                                INTVAL.parseKey() + ", " + CPINDEX.parseKey(),
                                scanner.stringValue);
                        throw new SyntaxError();
                }
                break;
            case T_CHAR:
                // consume the keyword, get the value
                scanner.scan();
                switch (scanner.token) {
                    case INTVAL:
                        environment.traceln("Char Field: " + name + " = " + scanner.intValue);
                        Integer val = scanner.intValue;
                        // Bounds check?
                        dataWriter = new ConstElemValue(AE_CHAR.tag(), parser.pool.findIntegerCell(val));
                        scanner.scan();
                        break;
                    case CPINDEX:
                        int cpIndex = Integer.parseInt(scanner.stringValue);
                        dataWriter = new ConstElemValue(AE_CHAR.tag(), parser.pool.getCell(cpIndex));
                        scanner.scan();
                        break;
                    default:
                        environment.error(scanner.pos, "err.incorrect.annotation",
                                AE_CHAR.printValue(),
                                INTVAL.parseKey() + ", " + CPINDEX.parseKey(),
                                scanner.stringValue);
                        throw new SyntaxError();
                }
                break;
            case T_SHORT:
                // consume the keyword, get the value
                scanner.scan();
                switch (scanner.token) {
                    case INTVAL:
                        environment.traceln("Short Field: " + name + " = " + scanner.intValue);
                        int val = scanner.intValue;
                        if (val > 0xFFFF) {
                            environment.traceln("Warning: Short Field: " + name + " value is greater than 0xFFFF, value = " + scanner.intValue);
                        }
                        dataWriter = new ConstElemValue(AE_SHORT.tag(),
                                parser.pool.findIntegerCell(val));
                        scanner.scan();
                        break;
                    case CPINDEX:
                        int cpIndex = Integer.parseInt(scanner.stringValue);
                        dataWriter = new ConstElemValue(AE_SHORT.tag(),
                                parser.pool.getCell(cpIndex));
                        scanner.scan();
                        break;
                    default:
                        environment.error(scanner.pos, "err.incorrect.annotation",
                                AE_SHORT.printValue(),
                                INTVAL.parseKey() + ", " + CPINDEX.parseKey(),
                                scanner.stringValue);
                        throw new SyntaxError();
                }
                break;
            default:
                environment.error(scanner.pos, "err.incorrect.annot.keyword", ident);
                throw new SyntaxError();
        }
        return dataWriter;
    }

    /**
     * AnnotationElemValue - used to store Annotation values
     */
    static class AnnotationElemValue implements ConstantPoolDataVisitor {

        AnnotationData annotationData;

        AnnotationElemValue(AnnotationData annotationData) {
            this.annotationData = annotationData;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(AE_ANNOTATION.tag());
            annotationData.write(out);
        }

        @Override
        public int getLength() {
            return 1 + annotationData.getLength();
        }

        @Override
        public <T extends DataWriter> T visit(ConstantPool pool) {
            annotationData = visitData(annotationData, pool);
            return (T) this;
        }
    }

    /**
     * Annotation Element value referring to a class
     */
    static class ClassElemValue implements ConstantPoolDataVisitor {

        ConstCell constCell;

        ClassElemValue(ConstCell constCell) {
            this.constCell = constCell;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(AE_CLASS.tag());
            constCell.write(out);
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public <T extends DataWriter> T visit(ConstantPool pool) {
            constCell = visitConstCell(constCell, pool);
            return (T) this;
        }
    }

    /**
     * Annotation Element value referring to an Array
     */
    static class ArrayElemValue implements ConstantPoolDataVisitor {

        ArrayList<DataWriter> elemValues;

        ArrayElemValue() {
            this.elemValues = new ArrayList<>();
        }

        void add(DataWriter elemValue) {
            elemValues.add(elemValue);
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(AE_ARRAY.tag());
            out.writeShort(elemValues.size());

            for (DataWriter eval : elemValues) {
                eval.write(out);
            }
        }

        @Override
        public int getLength() {
            return 3 + elemValues.stream().flatMapToInt(elem -> IntStream.of(elem.getLength())).sum();
        }

        @Override
        public <T extends DataWriter> T visit(ConstantPool pool) {
            for (DataWriter element : elemValues) {
                visitData(element, pool);
            }
            return (T) this;
        }
    }

    /**
     * Annotation Element value referring to a Constant
     */
    static class ConstElemValue implements ConstantPoolDataVisitor {

        char tag;
        ConstCell constCell;

        ConstElemValue(char tag, ConstCell constCell) {
            this.tag = tag;
            this.constCell = constCell;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(tag);
            constCell.write(out);
        }

        @Override
        public int getLength() {
            return 3;
        }

        @Override
        public <T extends DataWriter> T visit(ConstantPool pool) {
            if (!this.constCell.isSet()) {
                this.constCell = visitConstCell(this.constCell, pool);
                if (!AnnotationElementType.isSet(this.tag)) {
                    tag = this.constCell.getAnnotationElementTypeValue();
                }
            }
            return (T) this;
        }
    }

    /**
     * Element Value for Enums
     */
    static class EnumElemValue implements ConstantPoolDataVisitor {

        ConstCell type;
        ConstCell value;

        EnumElemValue(ConstCell type, ConstCell value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public void write(CheckedDataOutputStream out) throws IOException {
            out.writeByte(AE_ENUM.tag());
            type.write(out);
            value.write(out);
        }

        @Override
        public int getLength() {
            return 5;
        }

        @Override
        public <T extends DataWriter> T visit(ConstantPool pool) {
            this.type = visitConstCell(this.type, pool);
            this.value = visitConstCell(this.value, pool);
            return (T) this;
        }
    }

    /**
     * Target Type visitor, used for constructing the target-info within a type
     * annotation. visitExcept() is the entry point. ti is the constructed target info.
     */
    private static class TargetTypeVisitor extends TypeAnnotationTypes.TypeAnnotationTargetVisitor {

        private TypeAnnotationTargetInfoData targetInfoData;
        private SyntaxError syntaxError;
        private Scanner scanner;
        private JasmEnvironment environment;

        public void init(Scanner scanner) {
            this.scanner = scanner;
            this.environment = scanner.environment;
            reset();
        }

        public final void reset() {
            targetInfoData = null;
            syntaxError = null;
        }

        //This is the entry point for a visitor that tunnels exceptions
        public void visitExcept(ETargetType targetType) throws SyntaxError {
            reset();
            visit(targetType);
            if (syntaxError != null) {
                throw syntaxError;
            }
        }

        public TypeAnnotationTargetInfoData getTargetInfo() {
            return targetInfoData;
        }

        // Gathers Int values, and tunnels any exceptions thrown by the scanner
        private int scanIntVal(ETargetType targetType) {
            int ret = -1;
            if (scanner.token == INTVAL) {
                ret = scanner.intValue;
                try {
                    scanner.scan();
                } catch (SyntaxError se) {
                    syntaxError = se;
                }
            } else {
                environment.error(scanner.pos, "err.incorrect.typeannot.targtype.int", targetType.parseKey(), scanner.token);
                syntaxError = new SyntaxError();
            }
            return ret;
        }

        // Gathers String values, and tunnels any exceptions thrown by the scanner
        private String scanStringVal(ETargetType targetType) {
            String ret = "";
            if (scanner.token == Token.STRINGVAL) {
                ret = scanner.stringValue;
                try {
                    scanner.scan();
                } catch (SyntaxError se) {
                    syntaxError = se;
                }
            } else {
                environment.error(scanner.pos, "err.incorrect.typeannot.targtype.string", targetType.parseKey(), scanner.token);
                syntaxError = new SyntaxError();
            }
            return ret;
        }

        // Gathers braces, and tunnels any exceptions thrown by the scanner
        private void scanBrace(boolean left) {
            try {
                scanner.expect(left ? Token.LBRACE : Token.RBRACE);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
        }

        private boolean errorFound() {
            return syntaxError != null;
        }

        @Override
        public void visit_type_param_target(ETargetType targetType) {
            environment.traceln("Type Param Target: ");
            int byteval = scanIntVal(targetType); // param index
            if (!errorFound()) {
                targetInfoData = new TypeAnnotationTargetInfoData.type_parameter_target(targetType, byteval);
            }
        }

        @Override
        public void visit_supertype_target(ETargetType targetType) {
            environment.traceln("SuperType Target: ");
            int shortval = scanIntVal(targetType); // type index
            if (!errorFound()) {
                targetInfoData = new TypeAnnotationTargetInfoData.supertype_target(targetType, shortval);
            }
        }

        @Override
        public void visit_typeparam_bound_target(ETargetType targetType) {
            environment.traceln("TypeParam Bound Target: ");
            int byteval1 = scanIntVal(targetType); // param index
            if (errorFound()) {
                return;
            }
            int byteval2 = scanIntVal(targetType); // bound index
            if (errorFound()) {
                return;
            }
            targetInfoData = new TypeAnnotationTargetInfoData.type_parameter_bound_target(targetType, byteval1, byteval2);
        }

        @Override
        public void visit_empty_target(ETargetType targetType) {
            environment.traceln("Empty Target: ");
            if (!errorFound()) {
                targetInfoData = new TypeAnnotationTargetInfoData.empty_target(targetType);
            }
        }

        @Override
        public void visit_methodformalparam_target(ETargetType targetType) {
            environment.traceln("MethodParam Target: ");
            int byteval = scanIntVal(targetType); // param index
            if (!errorFound()) {
                targetInfoData = new formal_parameter_target(targetType, byteval);
            }
        }

        @Override
        public void visit_throws_target(ETargetType targetType) {
            environment.traceln("Throws Target: ");
            int shortval = scanIntVal(targetType); // exception index
            if (!errorFound()) {
                targetInfoData = new throws_target(targetType, shortval);
            }
        }

        @Override
        public void visit_localvar_target(ETargetType targetType) {
            environment.traceln("LocalVar Target: ");
            localvar_target locvartab = new localvar_target(targetType, 0);
            targetInfoData = locvartab;

            while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
                // consume the left brace
                scanBrace(true);
                if (errorFound()) {
                    return;
                }
                // scan the local var triple
                int shortval1 = scanIntVal(targetType); // startPC
                if (errorFound()) {
                    return;
                }
                int shortval2 = scanIntVal(targetType); // length
                if (errorFound()) {
                    return;
                }
                int shortval3 = scanIntVal(targetType); // CPX
                locvartab.addEntry(shortval1, shortval2, shortval3);
                scanBrace(false);
                if (errorFound()) {
                    return;
                }
            }
        }

        @Override
        public void visit_catch_target(ETargetType targetType) {
            environment.traceln("Catch Target: ");
            int shortval = scanIntVal(targetType); // catch index

            targetInfoData = new catch_target(targetType, shortval);
        }

        @Override
        public void visit_offset_target(ETargetType targetType) {
            environment.traceln("Offset Target: ");
            int shortval = scanIntVal(targetType); // offset index
            if (!errorFound()) {
                targetInfoData = new offset_target(targetType, shortval);
            }
        }

        @Override
        public void visit_typearg_target(ETargetType targetType) {
            environment.traceln("TypeArg Target: ");
            int shortval = scanIntVal(targetType); // offset
            if (errorFound()) {
                return;
            }
            int byteval = scanIntVal(targetType); // type index
            if (errorFound()) {
                return;
            }
            targetInfoData = new type_argument_target(targetType, shortval, byteval);
        }
    }
}
