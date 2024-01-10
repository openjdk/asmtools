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

import org.openjdk.asmtools.asmutils.StringUtils;
import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.*;
import org.openjdk.asmtools.jasm.ConstantPool.ConstValue_Cell;
import org.openjdk.asmtools.jdis.ModuleContent;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.structure.CFVersion.copyOf;
import static org.openjdk.asmtools.common.structure.EModifier.*;
import static org.openjdk.asmtools.jasm.ClassData.CoreClasses.PLACE.CLASSFILE;
import static org.openjdk.asmtools.jasm.ClassFileConst.*;
import static org.openjdk.asmtools.jasm.ConstantPool.ConstValue_UTF8;
import static org.openjdk.asmtools.jasm.Indexer.NotSet;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jdis.ConstantPool.TAG.*;

/**
 * This class is used to parse Jasm statements and expressions.
 * The result is a parse tree.<p>
 * <p>
 * This class implements an operator precedence parser. Errors are
 * reported to the Environment object, if the error can't be
 * resolved immediately, a SyntaxError exception is thrown.<p>
 * <p>
 * Error recovery is implemented by catching Scanner.SyntaxError exceptions
 * and discarding input scanner.tokens until an input token is reached that
 * is possibly a legal continuation.<p>
 * <p>
 * The parse tree that is constructed represents the input
 * exactly (no rewrites to simpler forms). This is important
 * if the resulting tree is to be used for code formatting in
 * a programming environment. Currently, only documentation comments
 * are retained.<p>
 * <p>
 * A parser owns several components (scanner, constant-parser,
 * instruction-parser, annotations-parser) to which it delegates certain
 * parsing responsibilities.  This parser contains functions to parse the
 * overall form of a class, and any members (fields, methods, inner-classes).
 * <p>
 * Syntax errors, should always be caught inside the
 * parser for error recovery.
 */
class Parser extends ParseBase {

    private final ArrayList<ClassData> clsDataList = new ArrayList<>();
    /**
     * other parser components
     */
    private final ParserAnnotation annotParser;       // For parsing Annotations
    private final ParserCP cpParser;                  // for parsing Constants
    private final ParserInstr instrParser;            // for parsing Instructions
    /* Parser Fields */
    protected ConstantPool pool = null;
    ClassData classData = null;
    CFVersion currentCFV;
    CodeAttr curCodeAttr;
    private String pkg = null;
    private String pkgPrefix = "";
    private ArrayList<AnnotationData> pkgAnnttns = null;
    private ArrayList<AnnotationData> clsAnnttns = null;
    private ArrayList<AnnotationData> memberAnnttns = null;
    private boolean explicitCP = false;
    private ModuleAttr moduleAttribute;

    /**
     * Create a parser
     */
    protected Parser(JasmEnvironment environment, CFVersion cfVersion) throws IOException {
        super.init(environment, this);
        this.cpParser = new ParserCP(this);
        this.annotParser = new ParserAnnotation(this);
        this.instrParser = new ParserInstr(this, cpParser);
        this.currentCFV = copyOf(cfVersion);
    }

    void setDebugFlags(boolean debugScanner, boolean debugMembers,
                       boolean debugCP, boolean debugAnnot, boolean debugInstr) {
        setDebugFlag(debugMembers);
        scanner.setDebugFlag(debugScanner);
        cpParser.setDebugFlag(debugCP);
        annotParser.setDebugFlag(debugAnnot);
        instrParser.setDebugFlag(debugInstr);
    }

    String encodeClassString(String classname) {
        return "L" + classname + ";";
    }

    public int getPosition() {
        return environment.getPosition();
    }

    private void parseVersion() {
        int majorVersion, minorVersion;
        if (scanner.token == Token.VERSION) {
            scanner.scan();
            if (scanner.token == Token.INTVAL) {
                majorVersion = scanner.intValue;
                scanner.scan();
                if (scanner.token == Token.COLON) {
                    scanner.scan();
                    if (scanner.token == Token.INTVAL) {
                        minorVersion = scanner.intValue;
                        classData.cfv.setFileVersion(majorVersion, minorVersion);
                        scanner.scan();
                        traceMethodInfoLn("parseVersion: " + classData.cfv.asString());
                        return;
                    }
                }
            }
        }
        environment.error(scanner.pos, "err.version.expected");
        throw new SyntaxError();
    }

    // Parse an internal name: identifier.
    String parseIdent() throws SyntaxError {
        String v = scanner.idValue;
        scanner.expect(Token.IDENT);
        return v;
    }

    /**
     * Parse a local variable presented in the form
     * (var) index  #name_index:#descriptor_index; [ (var) index name:descriptor; ]
     * <p>
     * index - a valid index into the local variable array of the current frame.
     * name -  valid unqualified name denoting a local variable
     * descriptor - a field descriptor which encodes the type of a local variable in the source program
     */
    void parseLocVarDef() throws SyntaxError {
        int index = NotSet;
        ConstCell<?> nameCell, descriptorCell;
        // The form is (var) index  #name_index:#descriptor_index; [ (var) index name:descriptor; ]
        int indexPosition = scanner.pos;
        if (scanner.token != Token.INTVAL) {
            environment.error(indexPosition, "err.locvar.expected", index, curCodeAttr.max_locals.cpIndex);
            throw new SyntaxError();
        }
        index = scanner.intValue;
        if (!curCodeAttr.max_locals.inRange(index)) {
            environment.error(indexPosition, "err.locvar.wrong.index", index, curCodeAttr.max_locals.cpIndex - 1);
            throw new SyntaxError();
        }
        scanner.scan();
        // scan pair #name_index:#descriptor_index or  name:descriptor
        // 1. scan (#)name(_index)
        nameCell = parseName();
        scanner.expect(Token.COLON);
        // scan pair #name_index:#descriptor_index or  name:descriptor
        // 1. scan (#)descriptor(_index)
        int descriptorPosition = scanner.pos;
        descriptorCell = parseName();
        // check field type of the local var
        FieldType fieldType = FieldType.getFieldType(((String) descriptorCell.ref.value).charAt(0));
        if (fieldType == null) {
            environment.error(descriptorPosition, "err.locvar.unknown.field.descriptor", index, descriptorCell.ref.value.toString());
            throw new SyntaxError();
        }
        curCodeAttr.LocVarDataDef(indexPosition, index, nameCell, descriptorCell);
    }

    /**
     * Parse The index (LOCAL_VARIABLE) into the local variable array of the instructions:
     * [wide]aload, astore, fload, fstore, iload, istore, lload, lstore, dload, dstore LOCAL_VARIABLE;
     * [wide]iinc LOCAL_VARIABLE, NUMBER;
     */
    Indexer parseLocVarRef() throws SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int index = scanner.intValue;
            scanner.scan();
            return new Indexer(index);
        } else {
            //IMPROVEMENT: add parsing form where LOCAL_VARIABLE is a name of the LocalVariable
            // like aload count[| #12]; where count was defined above: var 2 count:I;
            environment.error(scanner.pos, "err.locvar.expected");
            throw new SyntaxError();
        }
    }

    /**
     * Parse The index (LOCAL_VARIABLE) into the local variable array of the instructions:
     * endvar LOCAL_VARIABLE;
     */
    void parseLocVarEnd() throws SyntaxError, IOException {
        final int position = scanner.pos;
        if (scanner.token == Token.INTVAL) {
            int index = scanner.intValue;
            curCodeAttr.LocVarDataEnd((short) index, position);
            scanner.scan();
        } else {
            //IMPROVEMENT: add parsing form where LOCAL_VARIABLE is a name of the LocalVariable
            // like varend count[| #12]; where count was defined above: var 2 count:I;
            environment.error(scanner.pos, "err.locvar.expected");
            throw new SyntaxError();
        }
    }

    void parseMapItem(DataVector map) throws SyntaxError, IOException {
        StackMap.VerificationType itemVerificationType =
                StackMap.getVerificationType(scanner.intValue, Optional.empty());
        ConstType tag = null;
        Indexer arg = null;
        Token token = scanner.token;
        int iValue = scanner.intValue;
        String sValue = scanner.stringValue;
        scanner.scan();
        resolve:
        {
            switch (token) {
                case INTVAL:
                    break resolve;
                case CLASS:
                    itemVerificationType = StackMap.VerificationType.ITEM_Object;
                    tag = ConstType.CONSTANT_CLASS;
                    break resolve;
                case CPINDEX:
                    itemVerificationType = StackMap.VerificationType.ITEM_Object;
                    arg = pool.getCell(iValue);
                    break resolve;
                case IDENT:
                    itemVerificationType = StackMap.getVerificationType(sValue);
                    tag = ClassFileConst.tag(sValue);
                    if (itemVerificationType != null) { // itemType OK
                        if ((tag != null) // ambiguity: "int," or "int 77,"?
                                && (scanner.token != SEMICOLON)
                                && (scanner.token != COMMA)) {
                            itemVerificationType = StackMap.VerificationType.ITEM_Object;
                        }
                        break resolve;
                    } else if (tag != null) { // tag OK
                        itemVerificationType = StackMap.VerificationType.ITEM_Object;
                        break resolve;
                    }
            }
            // resolution failed:
            itemVerificationType = StackMap.VerificationType.ITEM_Bogus;
            environment.error("err.itemtype.expected", "<" + token.printValue() + ">");
        }
        switch (itemVerificationType) {
            case ITEM_Object -> {  // followed by CP index
                if (arg == null) {
                    arg = pool.findCell(cpParser.parseConstValue(tag));
                }
                map.addElement(new StackMapData.StackMapItemTaggedPointer(itemVerificationType, arg));
            }
            case ITEM_NewObject -> {  // followed by label
                arg = instrParser.parseLabelRef();
                map.addElement(new StackMapData.StackMapItemTaggedPointer(itemVerificationType, arg));
            }
            default -> map.addElement(new StackMapData.StackMapItemTagged(itemVerificationType));
        }
    }

    /**
     * Parse an external name: CPINDEX, string, or identifier.
     */
    ConstCell parseName() throws SyntaxError {
        traceMethodInfoLn();
        String v;
        if (scanner.token == CPINDEX) {
            int cpx = scanner.intValue;
            scanner.scan();
            return pool.getCell(cpx);
        } else if (scanner.token == STRINGVAL) {
            v = scanner.stringValue;
            scanner.scan();
            return pool.findUTF8Cell(v);
        } else if (scanner.token.isPossibleClassName()) {
            // In many cases, Identifiers can correctly have the same
            // names as keywords.  Let's allow Class Names.
            v = scanner.idValue;
            scanner.scan();
            return pool.findUTF8Cell(v);
        }
        environment.error(scanner.pos, "err.name.expected", "\"" + scanner.token + "\"");
        throw new SyntaxError();
    }

    /**
     * Parses a field or method reference for method handle.
     */
    ConstCell parseMethodHandle(SubTag subtag) throws SyntaxError {
        ConstCell refCell;
        switch (subtag) {
            // If the value of the reference_kind item is
            // 1 (REF_getField), 2 (REF_getStatic), 3 (REF_putField)  or 4 (REF_putStatic),
            // then the constant_pool entry at that index must be a CONSTANT_Fieldref_info structure (4.4.2)
            // representing a field for which a method handle is to be created. jvms-4.4.8-200-C-A
            case REF_GETFIELD, REF_GETSTATIC, REF_PUTFIELD, REF_PUTSTATIC ->
                    refCell = pool.findCell(cpParser.parseConstValue(ConstType.CONSTANT_FIELDREF));

            //  If the value of the reference_kind item is
            //  5 (REF_invokeVirtual) or 8 (REF_newInvokeSpecial),
            //  then the constant_pool entry at that index must be a CONSTANT_Methodref_info structure (4.4.2)
            //  representing a class's method or constructor (2.9.1) for which a method handle is to be created.
            //  jvms-4.4.8-200-C-B
            case REF_INVOKEVIRTUAL, REF_NEWINVOKESPECIAL -> {
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ConstType.CONSTANT_METHODREF, ConstType.CONSTANT_INTERFACEMETHODREF);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(getPosition(), ConstType.CONSTANT_METHODREF, null);
            }
            case REF_INVOKESTATIC, REF_INVOKESPECIAL -> {
                // CODETOOLS-7902333
                // 4.4.8. The CONSTANT_MethodHandle_info Structure
                // reference_index
                // The value of the reference_index item must be a valid index into the constant_pool table.
                // The constant_pool entry at that index must be as follows:
                // If the value of the reference_kind item is 6 (REF_invokeStatic) or 7 (REF_invokeSpecial),
                // then if the class file version number is less than 52.0, the constant_pool entry at that index must be
                // a CONSTANT_Methodref_info structure representing a class's method for which a method handle is to be created;
                // if the class file version number is 52.0 or above, the constant_pool entry at that index must be
                // either a CONSTANT_Methodref_info structure or a CONSTANT_InterfaceMethodref_info structure (4.4.2)
                // representing a class's or interface's method for which a method handle is to be created.
                ConstType ctype01 = ConstType.CONSTANT_METHODREF;
                ConstType ctype02 = ConstType.CONSTANT_INTERFACEMETHODREF;
                if (this.classData.cfv.major_version() >= 52 && EModifier.isInterface(this.classData.access)) {
                    ctype01 = ConstType.CONSTANT_INTERFACEMETHODREF;
                    ctype02 = ConstType.CONSTANT_METHODREF;
                }
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ctype01, ctype02);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(getPosition(), ctype01, ctype02);
            }
            case REF_INVOKEINTERFACE -> {
                cpParser.setExitImmediately(true);
                refCell = cpParser.parseConstRef(ConstType.CONSTANT_INTERFACEMETHODREF, ConstType.CONSTANT_METHODREF);
                cpParser.setExitImmediately(false);
                checkReferenceIndex(getPosition(), ConstType.CONSTANT_INTERFACEMETHODREF, null);
            }
            default ->
                // should not reach
                    throw new SyntaxError();
        }
        return refCell;
    }

    /**
     * Check the pair reference_kind:reference_index where reference_kind is any from:
     * REF_invokeVirtual, REF_newInvokeSpecial, REF_invokeStatic, REF_invokeSpecial, REF_invokeInterface
     * and reference_index is one of [Empty], Method or InterfaceMethod
     * There are possible entries:
     * ldc Dynamic REF_newInvokeSpecial:InterfaceMethod  LdcConDyTwice."<init>":
     * ldc Dynamic REF_invokeInterface:LdcConDyTwice."<init>":
     * ldc Dynamic REF_newInvokeSpecial:Method LdcConDyTwice."<init>":
     * ldc MethodHandle REF_newInvokeSpecial:InterfaceMethod  LdcConDyTwice."<init>":
     * ldc MethodHandle REF_invokeInterface:LdcConDyTwice."<init>":
     * ldc MethodHandle REF_newInvokeSpecial:Method LdcConDyTwice."<init>":
     * invokedynamic MethodHandle REF_invokeStatic:Method java/lang/invoke/StringConcatFactory.makeConcatWithConstants:
     * invokedynamic MethodHandle REF_invokeStatic:java/lang/invoke/StringConcatFactory.makeConcatWithConstants
     * ....
     *
     * @param position    the position in a source file
     * @param defaultTag  expected reference_index tag (Method or InterfaceMethod)
     * @param defaultTag2 2nd expected reference_index tag (Method or InterfaceMethod)
     */
    private void checkReferenceIndex(int position, ConstType defaultTag, ConstType defaultTag2) {
        if (!scanner.token.in(COLON, SEMICOLON, COMMA)) {
            if (defaultTag2 != null) {
                environment.error(position, "err.wrong.tag2", defaultTag.parseKey(), defaultTag2.parseKey());
            } else {
                environment.error(position, "err.wrong.tag", defaultTag.parseKey());
            }
            throw new SyntaxError().setFatal();
        }
    }

    /**
     * Parses a sub-tag value in method handle.
     */
    SubTag parseSubtag() throws SyntaxError {
        SubTag subtag = switch (scanner.token) {
            case IDENT -> subTag(scanner.stringValue);
            case INTVAL -> subTag(scanner.intValue);
            default -> null;
        };
        if (subtag == null) {
            environment.error("err.subtag.expected");
            throw new SyntaxError();
        }
        scanner.scan();
        return subtag;
    }

    ConstCell parseConstantPackageInfo() throws SyntaxError {
        if (scanner.token == CPINDEX) {
            int cpx = scanner.intValue;
            scanner.scan();
            return pool.getCell(cpx);
        } else if (scanner.token == STRINGVAL ||
                // Some identifiers might coincide with token names.
                // these should be OK to use as identifier names.
                scanner.token.isPossibleClassName()) {
            String packageName = scanner.stringValue;
            scanner.scan();
            return pool.findPackageCell(packageName);
        } else {
            throwSyntaxError("err.package.name.expected");
            return null;
        }
    }

    ConstCell parseConstantModuleInfo() throws SyntaxError {
        if (scanner.token == CPINDEX) {
            int cpx = scanner.intValue;
            scanner.scan();
            return pool.getCell(cpx);
        } else if (scanner.token == STRINGVAL ||
                // Some identifiers might coincide with token names.
                // these should be OK to use as identifier names.
                scanner.token.isPossibleModuleName()) {
            String moduleName = scanner.stringValue;
            scanner.scan();
            return pool.findModuleCell(moduleName);
        } else {
            throwSyntaxError("err.module.name.expected");
            return null;
        }
    }

    ConstCell parseConstantClassInfo(boolean uncond) throws SyntaxError {
        if (scanner.token == CPINDEX) {
            int cpx = scanner.intValue;
            scanner.scan();
            return pool.getCell(cpx);
        } else if (scanner.token == STRINGVAL ||
                // Some identifiers might coincide with token names.
                // these should be OK to use as identifier names.
                scanner.token.isPossibleClassName()) {
            String value = scanner.stringValue;
            scanner.scan();
            value = prependPackage(value, uncond);
            return pool.findUTF8Cell(value);
        } else {
            throwSyntaxError("err.class.name.expected");
            return null;
        }
    }

    private void throwSyntaxError(String msgId) throws SyntaxError {
        ConstType key = ClassFileConst.tag(scanner.token.value());
        environment.traceln("Unrecognized token %s: %s", scanner.token.toString(), key == null ? "null" : key.parseKey());
        environment.error(scanner.prevPos, msgId, "\"" + scanner.token.parseKey() + "\"");
        throw new SyntaxError();
    }

    private String prependPackage(String className, boolean uncond) {
        if (uncond || (scanner.token == Token.FIELD)) {
            if ((!className.contains("/"))             // class identifier doesn't contain "/"
                    && (!className.contains("["))) {    // class identifier doesn't contain "["
                className = pkgPrefix + className; // add package
            }
        }
        return className;
    }

    /**
     * Parse a signed integer of size bytes long.
     * size = 1 or 2
     */
    Indexer parseInt(String opCode, int size) throws SyntaxError, IOException {
        if (scanner.token == Token.BITS) {
            scanner.scan();
        }
        if (scanner.token != Token.INTVAL) {
            environment.error(scanner.pos, "err.int.expected");
            throw new SyntaxError();
        }
        int arg = scanner.intValue * scanner.sign;
        switch (size) {
            case 1:
//              if ((arg>127)||(arg<-128)) { // 0xFF not allowed
                if ((arg > 255) || (arg < -128)) { // to allow 0xFF
                    environment.error(scanner.pos, "err.value.large", opCode, arg, "1 byte");
                    throw new SyntaxError();
                }
                break;
            case 2:
//              if ((arg > 32767) || (arg < -32768)) { //this seems
//              natural but is not backward compatible. Some tests contain
//              expressions like:  sipush    0x8765;
                if ((arg > 65535) || (arg < -32768)) {
                    environment.error(scanner.pos, "err.value.large", opCode, arg, "2 bytes");
                    throw new SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseInt(" + size + ")");
        }
        scanner.scan();
        return new Indexer(arg);
    }

    /**
     * Parse an unsigned integer of size bytes long.
     * size = 1 or 2
     */
    Indexer parseUInt(int size) throws SyntaxError, IOException {
        if (scanner.token != Token.INTVAL) {
            environment.error(scanner.pos, "err.int.expected");
            throw new SyntaxError();
        }
        if (scanner.sign == -1) {
            environment.error(scanner.pos, "err.neg.forbidden");
            throw new SyntaxError();
        }
        int arg = scanner.intValue;
        switch (size) {
            case 1:
                if (arg > 255) {
                    environment.error(scanner.pos, "err.value.large", "ubyte", arg, "1 byte");
                    throw new SyntaxError();
                }
                break;
            case 2:
                if (arg > 65535) {
                    environment.error(scanner.pos, "err.value.large", "ushort", arg, "2 bytes");
                    throw new SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseUInt(" + size + ")");
        }
        scanner.scan();
        return new Indexer(arg);
    }

    /**
     * Parse constant declaration
     */
    private void parseConstDef() {
        for (; ; ) {
            if (scanner.token == Token.CPINDEX) {
                int cpx = scanner.intValue;
                scanner.scan();
                scanner.expect(Token.ASSIGN);
                traceMethodInfoLn("\ncpIndex: " + cpx);
                pool.setCell(cpx, cpParser.parseConstRef(null));
            } else {
                environment.error("err.const.def.expected");
                throw new SyntaxError();
            }
            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                return;
            }
            scanner.scan(); // COMMA
        }
    }

    /**
     * Parse the modifiers
     */
    private int scanModifier(int mod) throws SyntaxError {
        int nextmod, prevpos;

        while (true) {
            nextmod = 0;
            switch (scanner.token) {
                case PUBLIC:
                    nextmod = EModifier.ACC_PUBLIC.getFlag();
                    break;
                case PRIVATE:
                    nextmod = EModifier.ACC_PRIVATE.getFlag();
                    break;
                case PROTECTED:
                    nextmod = EModifier.ACC_PROTECTED.getFlag();
                    break;
                case STATIC:
                    nextmod = EModifier.ACC_STATIC.getFlag();
                    break;
                case FINAL:
                    nextmod = EModifier.ACC_FINAL.getFlag();
                    break;
                case SYNCHRONIZED:
                    nextmod = EModifier.ACC_SYNCHRONIZED.getFlag();
                    break;
                case SUPER:
                    nextmod = EModifier.ACC_SUPER.getFlag();
                    break;
                case VOLATILE:
                    nextmod = EModifier.ACC_VOLATILE.getFlag();
                    break;
                case BRIDGE:
                    nextmod = EModifier.ACC_BRIDGE.getFlag();
                    break;
                case TRANSIENT:
                    nextmod = EModifier.ACC_TRANSIENT.getFlag();
                    break;
                case VARARGS:
                    nextmod = EModifier.ACC_VARARGS.getFlag();
                    break;
                case NATIVE:
                    nextmod = EModifier.ACC_NATIVE.getFlag();
                    break;
                case INTERFACE:
                    nextmod = ACC_INTERFACE.getFlag();
                    break;
                case ABSTRACT:
                    nextmod = ACC_ABSTRACT.getFlag();
                    break;
                case STRICT:
                    nextmod = EModifier.ACC_STRICT.getFlag();
                    break;
                case ENUM:
                    nextmod = EModifier.ACC_ENUM.getFlag();
                    break;
                case SYNTHETIC:
                    nextmod = EModifier.ACC_SYNTHETIC.getFlag();
                    break;
                case ANNOTATION_ACCESS:
                    nextmod = EModifier.ACC_ANNOTATION.getFlag();
                    break;
                case DEPRECATED:
                    nextmod = EModifier.DEPRECATED_ATTRIBUTE.getFlag();
                    break;
                case MANDATED:
                    nextmod = EModifier.ACC_MANDATED.getFlag();
                    break;
                case VALUE:
                    nextmod = EModifier.ACC_VALUE.getFlag();
                    break;
                case PERMITS_VALUE:
                    nextmod = EModifier.ACC_PERMITS_VALUE.getFlag();
                    break;
                case PRIMITIVE:
                    nextmod = EModifier.ACC_PRIMITIVE.getFlag();
                    break;
                default:
                    return nextmod;
            }
            prevpos = scanner.pos;
            scanner.scan();
            if ((mod & nextmod) == 0) {
                return nextmod;
            }
            environment.warning(prevpos, "warn.repeated.modifier");
        }
    }

    int scanModifiers() throws SyntaxError {
        int mod = 0, nextmod;
        while (true) {
            nextmod = scanModifier(mod);
            if (nextmod == 0) {
                return mod;
            }
            mod = mod | nextmod;
        }
    }

    /**
     * Parse a field.
     */
    private void parseField(int mod) throws SyntaxError {
        // Parses in the form:
        // FIELD (, FIELD)*;
        // where
        // FIELD = NAME:DESCRIPTOR(:SIGNATURE)? CONST_VALUE?
        // NAME = (CPINDEX | IDENT)
        // DESCRIPTOR = (CPINDEX | STRING)
        // SIGNATURE  = (CPINDEX | STRING)
        // CONST_VALUE = ASSIGN CONSTREF
        traceMethodInfoLn("Begin");
        // check access modifiers:
        Checker.checkFieldModifiers(classData, mod, scanner.prevPos);
        while (true) {
            ConstCell nameCell = parseName();
            scanner.expect(COLON);
            ConstCell typeCell = parseName();

            // Define the variable
            FieldData fld = classData.addField(mod, nameCell, typeCell);

            if (memberAnnttns != null) {
                fld.addAnnotations(memberAnnttns);
            }

            // Parse the optional attribute: signature
            if (scanner.token == COLON) {
                scanner.scan();
                ConstCell signatureCell = parseName();
                fld.setSignatureAttr(signatureCell);
            }

            // Parse the optional initializer
            if (scanner.token == ASSIGN) {
                scanner.scan();
                fld.SetInitialValue(cpParser.parseConstRef(null));
            }

            // If the next scanner.token is a comma, then there is more
            traceMethodInfoLn("Field: " + fld);

            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                return;
            }
            scanner.scan();
        }  // end while
    }  // end parseField

    /**
     * Scan method's signature to determine size of parameters.
     */
    private int countParams(ConstCell sigCell) throws SyntaxError {
        String sig;
        try {
            ConstValue_UTF8 strConst = (ConstValue_UTF8) sigCell.ref;
            sig = strConst.value;
        } catch (NullPointerException | ClassCastException e) {
            return 0; // ??? TBD
        }
        int siglen = sig.length(), k = 0, loccnt = 0, errparam = 0;
        boolean arraytype = false;
        scan:
        {
            if (k >= siglen) {
                break scan;
            }
            if (sig.charAt(k) != '(') {
                errparam = 1;
                break scan;
            }
            for (k = 1; k < siglen; k++) {
                switch (sig.charAt(k)) {
                    case ')':
                        if (arraytype) {
                            errparam = 2;
                            break scan;
                        }
                        return loccnt;
                    case '[':
                        arraytype = true;
                        break;
                    case 'B':
                    case 'C':
                    case 'F':
                    case 'I':
                    case 'S':
                    case 'Z':
                        loccnt++;
                        arraytype = false;
                        break;
                    case 'D':
                    case 'J':
                        loccnt++;
                        if (arraytype) {
                            arraytype = false;
                        } else {
                            loccnt++;
                        }
                        break;
                    case 'L':
                    case 'Q':
                        for (; ; k++) {
                            if (k >= siglen) {
                                errparam = 3;
                                break scan;
                            }
                            if (sig.charAt(k) == ';') {
                                break;
                            }
                        }
                        loccnt++;
                        arraytype = false;
                        break;
                    default:
                        errparam = 4;
                        break scan;
                }
            }
        }
        environment.error(scanner.pos, "err.msig.malformed", Integer.toString(k), Integer.toString(errparam));
        return loccnt;
    }

    /**
     * Parse a method.
     */
    private void parseMethod(int mod) throws SyntaxError, IOException {
        traceMethodInfoLn("Begin");
        int scannerPosition = scanner.prevPos;
        // The start of the method
        ConstCell nameCell = parseName();
        ConstValue_UTF8 strConst = (ConstValue_UTF8) nameCell.ref;
        String name = strConst.value;
        boolean is_clinit = name.equals("<clinit>");
        // TODO: not a good way to detect factories...
        boolean is_init = name.equals("<init>") && !EModifier.isStatic(mod);
        DefaultAnnotationAttr defAnnot = null;

        // check access modifiers:
        Checker.checkMethodModifiers(classData, mod, scannerPosition, is_init, is_clinit);

        scanner.expect(Token.COLON);
        ConstCell typeCell = parseName();
        int paramCount = countParams(typeCell);
        if ((!EModifier.isStatic(mod)) && !is_clinit) {
            paramCount++;
        }
        if (paramCount > 255) {
            environment.warning(scanner.pos, "warn.msig.more255", Integer.toString(paramCount));
        }


        // Parse the optional attribute: signature
        ConstCell signatureCell = null;
        if (scanner.token == COLON) {
            scanner.scan();
            signatureCell = parseName();
        }

        // Parse throws clause
        ArrayList<ConstCell<?>> exc_table = null;
        if (scanner.token == Token.THROWS) {
            scanner.scan();
            exc_table = new ArrayList<>();
            for (; ; ) {
                scannerPosition = scanner.pos;
                ConstCell<?> exc = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
                if (exc_table.contains(exc)) {
                    environment.warning(scannerPosition, "warn.exc.repeated");
                } else {
                    exc_table.add(exc);
                    environment.traceln("THROWS:" + exc.cpIndex);
                }
                if (scanner.token != COMMA) {
                    break;
                }
                scanner.scan();
            }
        }
        if (scanner.token == Token.DEFAULT) {
            // need to scan the annotation value
            defAnnot = annotParser.parseDefaultAnnotation();
        }

        MethodData curMethod = classData.StartMethod(mod, nameCell, typeCell, exc_table);
        if (signatureCell != null) {
            curMethod.setSignatureAttr(signatureCell);
        }

        Indexer max_stack = null, max_locals = null;

        if (scanner.token == STACK) {
            scanner.scan();
            max_stack = parseUInt(2);
        }
        if (scanner.token == LOCAL) {
            scanner.scan();
            max_locals = parseUInt(2);
        }
        if (scanner.token == INTVAL) {
            annotParser.parseParamAnnots(paramCount, curMethod);
        }

        if (scanner.token == SEMICOLON) {
            if ((max_stack != null) || (max_locals != null)) {
                environment.error("err.token.expected", LBRACE.parseKey());
            }
            scanner.scan();
        } else {
            scanner.expect(LBRACE);
            curCodeAttr = curMethod.startCode(paramCount, max_stack, max_locals);
            while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
                instrParser.parseInstr();
                if (scanner.token == RBRACE) {
                    break;
                }
                // code's type annotation(s)
                if (scanner.token == ANNOTATION) {
                    curCodeAttr.addAnnotations(annotParser.scanAnnotations());
                    break;
                }
                scanner.expect(SEMICOLON);
            }
            curCodeAttr.endCode();
            scanner.expect(RBRACE);
        }

        if (defAnnot != null) {
            curMethod.addDefaultAnnotation(defAnnot);
        }
        if (memberAnnttns != null) {
            curMethod.addAnnotations(memberAnnttns);
        }
        classData.EndMethod();
        traceMethodInfoLn("End of the method " + curMethod);

    }  // end parseMethod

    /**
     * Parse a (CPX based) BootstrapMethod entry.
     */
    private void parseCPXBootstrapMethod() throws SyntaxError {
        // Parses in the form:
        // BOOTSTRAPMETHOD CPX_MethodHandle (CPX_Arg)* ;
        if (scanner.token == CPINDEX) {
            // CPX can be a CPX to an MethodHandle constant,
            int cpx = scanner.intValue;
            ConstCell<?> MHCell = pool.getCell(cpx);
            scanner.scan();
            ArrayList<ConstCell<?>> bsm_args = new ArrayList<>(256);

            while (scanner.token != SEMICOLON) {
                if (scanner.token == CPINDEX) {
                    bsm_args.add(pool.getCell(scanner.intValue));

                } else {
                    // throw error, bootstrap method is not recognizable
                    environment.error(scanner.pos, "err.invalid.bootstrapmethod");
                    throw new SyntaxError();
                }
                scanner.scan();
            }
            BootstrapMethodData bsmData = new BootstrapMethodData(MHCell, bsm_args);
            classData.addBootstrapMethod(bsmData);
        } else {
            // throw error, bootstrap method is not recognizable
            environment.error(scanner.pos, "err.invalid.bootstrapmethod");
            throw new SyntaxError();
        }
    }

    /**
     * Parse the class Signature entry.
     */
    private void parseClassSignature() throws SyntaxError {
        // Parses in the form:
        // SIGNATURE;
        // where
        // SIGNATURE = (CPINDEX | STRING)
        traceMethodInfoLn("Begin");
        ConstCell signatureCell = parseName();
        traceMethodInfoLn("Signature: " + signatureCell);
        classData.setSignatureAttr(signatureCell);
    }

    /**
     * Parse class reference used by statements:
     * this_class[:]    (CPINDEX | STRING);
     * super_class[:]   (CPINDEX | STRING);
     */
    private void parseClassRef(Consumer<ConstCell<?>> consumer) {
        traceMethodInfoLn("Begin");
        if (scanner.token == COLON) {
            scanner.scan();
        }
        ConstCell nm = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);
        consumer.accept(nm);
    }

    private void parseSourceFile() throws SyntaxError {
        // Parses in the form:
        // SOURCEFILE (CPINDEX | IDENT);
        traceMethodInfoLn("Begin");
        String cpSourceFile = null;
        if (pool.findUTF8Cell(EAttribute.ATT_SourceFile.parseKey()) != null) {
            String sourceName = environment.getSourceName();
            ConstCell<?> cell = pool.lookupUTF8Cell(name -> name.contains(sourceName) &&
                    StringUtils.contains.apply(name, List.of(".java", ".jcod", ".jasm", ".class")));
            if (cell != null) {
                cpSourceFile = (String) cell.ref.value;
            }
        }
        ConstCell<?> sourceFileCell = parseName();
        if (sourceFileCell.ref == null && sourceFileCell.cpIndex != NotSet) {
            environment.error(scanner.prevPos, "err.wrong.sourcefile.ref");
            throw new SyntaxError();
        }
        traceMethodInfoLn("Source File: " + sourceFileCell);
        if (cpSourceFile != null && !cpSourceFile.equals(sourceFileCell.ref.value)) {
            // new file name that overwrites CP value.
            environment.warning(scanner.prevPos, "warn.extra.attribute",
                    EAttribute.ATT_SourceFile.parseKey(), sourceFileCell.ref.value);
        }
        classData.setSourceFileAttr(sourceFileCell);
    }

    /**
     * Parse a SourceDebugExtension attribute
     */
    private void parseSourceDebugExtension() throws SyntaxError {
        // Parses in the form:
        // SOURCEDEBUGEXTENSION { ("STRING"; | (0x?? )+; )+ }
        traceMethodInfoLn("Begin");
        SourceDebugExtensionAttr sourceDebugExtensionAttr = classData.setSourceDebugExtensionAttr();
        boolean prevSemicolonParsed = true;

        scanner.expect(LBRACE);
        while (true) {
            switch (scanner.token) {
                case INTVAL, BYTEVAL, SHORT -> {
                    try {
                        sourceDebugExtensionAttr.append(scanner.intValue);
                    } catch (IllegalArgumentException ex) {
                        environment.error(scanner.pos, "err.token.expected", STRINGVAL.parseKey());
                        throw new SyntaxError();
                    }
                    // SEMICOLON ignored if list of bytes
                    prevSemicolonParsed = false;
                }
                case STRINGVAL -> {
                    if (!prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                        throw new SyntaxError();
                    }
                    try {
                        sourceDebugExtensionAttr.append(scanner.stringValue);
                    } catch (IllegalArgumentException ex) {
                        environment.error(scanner.pos, "err.token.expected", BYTEVAL.parseKey());
                        throw new SyntaxError();
                    }
                    prevSemicolonParsed = false;
                }
                case SEMICOLON -> {
                    if (prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected",
                                (sourceDebugExtensionAttr.type == SourceDebugExtensionAttr.Type.BYTE) ?
                                        BYTEVAL.parseKey() : STRINGVAL.parseKey());
                        throw new SyntaxError();
                    }
                    prevSemicolonParsed = true;
                }
                case RBRACE -> {
                    if (!prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                        throw new SyntaxError();
                    }
                    if (sourceDebugExtensionAttr.isEmpty()) {
                        environment.warning(scanner.pos, "warn.empty.debug.extension");
                    }
                    scanner.scan();
                    return;
                }
                default -> {
                    if (prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.one.of.two.token.expected",
                                STRINGVAL.parseKey(), RBRACE.parseKey());
                    } else {
                        environment.error(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                    }

                    throw new SyntaxError();

                }
            }
            // next line
            scanner.scan();
        }
    }

    /**
     * Parse a NestHost entry
     */
    private void parseNestHost() throws SyntaxError {
        // Parses in the form:
        // NESTHOST IDENT;
        traceMethodInfoLn("Begin");
        ConstCell<?> cell = parseConstantClassInfo(true);
        if (!cell.getType().oneOf(ConstType.CONSTANT_UTF8, ConstType.CONSTANT_CLASS)) {
            throwSyntaxError("err.class.name.expected");
        }
        classData.addNestHost(cell);
        traceMethodInfoLn("NestHost: class " + cell);
        scanner.expect(SEMICOLON);
    }

    /**
     * Parse a list of classes belonging to the [NestMembers | PermittedSubclasses | Preload]  entry
     */
    private void parseClasses(Consumer<ArrayList<ConstCell>> classesConsumer) throws SyntaxError {
        ArrayList<ConstCell> classes = new ArrayList<>();
        // Parses in the form:
        // (NESTMEMBERS|PERMITTEDSUBCLASSES|PRELOAD)? IDENT(, IDENT)*;
        traceMethodInfoLn("Begin");
        while (true) {
            ConstCell<?> cell = parseConstantClassInfo(true);
            if (!cell.getType().oneOf(ConstType.CONSTANT_UTF8, ConstType.CONSTANT_CLASS)) {
                throwSyntaxError("err.class.name.expected");
            }
            classes.add(cell);
            traceMethodInfoLn("Added cell: " + cell);
            if (scanner.token != COMMA) {
                scanner.expect(SEMICOLON);
                classesConsumer.accept(classes);
                return;
            }
            scanner.scan();
        }
    }

    /**
     * Parse the Record entry
     */
    private void parseRecord() throws SyntaxError {
        // Parses in the form:
        // RECORD { (COMPONENT)+ }
        // where
        // COMPONENT Component (ANNOTATION)* NAME:DESCRIPTOR(:SIGNATURE)? (,|;)
        // NAME = (CPINDEX | IDENT)
        // DESCRIPTOR = (CPINDEX | STRING)
        // SIGNATURE  = (CPINDEX | STRING)
        traceMethodInfoLn("Begin");
        scanner.expect(LBRACE);

        ArrayList<AnnotationData> componentAnntts = null;
        boolean grouped = false;
        RecordData rd = classData.setRecord(scanner.pos);

        while (true) {
            if (scanner.token == RBRACE) {
                if (rd.isEmpty()) {
                    environment.warning(scanner.pos, "warn.no.components.in.record.attribute");
                    classData.rejectRecord();
                } else if (grouped) {
                    environment.error(scanner.pos, "err.grouped.component.expected");
                }
                scanner.scan();
                break;
            }

            ConstCell nameCell, descCell, signatureCell = null;
            if (scanner.token == ANNOTATION) {
                componentAnntts = annotParser.scanAnnotations();
            }

            scanner.expect(COMPONENT);

            nameCell = parseName();
            scanner.expect(COLON);
            descCell = parseName();
            // Parse the optional attribute: signature
            if (scanner.token == COLON) {
                scanner.scan();
                signatureCell = parseName();
            }

            rd.addComponent(nameCell, descCell, signatureCell, componentAnntts);

            switch (scanner.token) {
                case COMMA -> grouped = true;
                case SEMICOLON -> {
                    grouped = false;
                    componentAnntts = null;
                }
                default -> environment.error(scanner.pos, "err.one.of.two.token.expected",
                        "<" + SEMICOLON.printValue() + ">",
                        "<" + COMMA.printValue() + ">");
            }
            // next component
            scanner.scan();
        }  // end while
        traceMethodInfoLn("End");
    }

    /**
     * Parse an inner class.
     */
    private void parseInnerClass(int mod) throws SyntaxError, IOException {
        // Parses in the form:
        // MODIFIERS (INNERCLASSNAME =)? (INNERCLASS) (OF OUTERCLASS)? ;
        //
        // where
        //    INNERCLASSNAME = (IDENT | CPX_IN-CL-NM)
        //    INNERCLASS = (CLASS IDENT | CPX_IN-CL) (S2)
        //    OUTERCLASS = (CLASS IDENT | CPX_OT-CL) (S3)
        //
        // Note:
        //    If a class reference cannot be identified using IDENT, CPX indexes must be used.

        // check access modifiers:
        traceMethodInfoLn("Begin");
        Checker.checkInnerClassModifiers(classData, mod, scanner.pos);

        ConstCell nameCell;
        ConstCell innerClass = null;
        ConstCell outerClass = null;

        if (scanner.token == CLASS) {
            nameCell = pool.getCell(0);  // no NameIndex
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            if (scanner.token == IDENT && scanner.checkTokenIdent()) {
                // Got a Class Name
                nameCell = parseName();
                parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == CPINDEX) {
                // CPX can be either a CPX to an InnerClassName,
                // or a CPX to an InnerClassInfo
                int cpx = scanner.intValue;
                nameCell = pool.getCell(cpx);
                ConstValue nameCellValue = nameCell.ref;

                if (nameCellValue instanceof ConstValue_UTF8) {
                    // got a name cell
                    scanner.scan();
                    parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
                } else {
                    // got a CPRef cell
                    nameCell = pool.getCell(0);  // no NameIndex
                    parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
                }
            } else if (scanner.token.isPossibleJasmIdentifier()) {
                // The name InnerClass of the inner class is allowed.
                nameCell = pool.findUTF8Cell(scanner.token.parseKey());
                scanner.scan();
                parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }
        }
        traceMethodInfoLn("End");
    }

    private void parseInnerClass_s1(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // next scanner.token must be '='
        if (scanner.token == ASSIGN) {
            scanner.scan();
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            pic_error();
        }
    }

    private void parseInnerClass_s2(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // scanner.token is either "CLASS IDENT" or "CPX_Class"
        if ((scanner.token == CPINDEX) || (scanner.token == CLASS)) {
            if (scanner.token == CPINDEX) {
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == CLASS) {
                // next symbol needs to be InnerClass
                scanner.scan();
                // innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS); ignore keywords as much as possible:
                // private static InnerClass Module = class NormalModule$Module of class NormalModule;
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);
            }

            // See if declaration is terminated
            if (scanner.token == SEMICOLON) {
                // InnerClass is complete, no OUTERINFO;
                outerClass = pool.getCell(0);
                pic_tracecreate(mod, nameCell, innerClass, outerClass);
                classData.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == OF) {
                // got an outer class reference
                parseInnerClass_s3(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }

        } else {
            pic_error();
        }

    }

    private void parseInnerClass_s3(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        scanner.scan();
        if ((scanner.token == CLASS) || (scanner.token == CPINDEX)) {
            if (scanner.token == CLASS) {
                // next symbol needs to be InnerClass
                scanner.scan();
                // outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);; ignore keywords as much as possible:
                // private static InnerClass NormalModule = class Module$NormalModule of class Module;
                outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);

            }
            if (scanner.token == CPINDEX) {
                outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == SEMICOLON) {
                pic_tracecreate(mod, nameCell, innerClass, outerClass);
                classData.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }
        } else {
            pic_error();
        }
    }

    private void pic_tracecreate(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) {
        // throw error, IC is not recognizable
        traceMethodInfoLn("Creating InnerClass:");
        environment.trace("[" + EModifier.asNames(mod, ClassFileContext.INNER_CLASS) + "], ");

        if (nameCell != pool.getCell(0)) {
            ConstValue value = nameCell.ref;
            if (value != null) {
                environment.trace(value + " =\n\t\t");
            }
        }

        ConstValue_Cell<?> ici_val = (ConstValue_Cell) innerClass.ref;
        ConstCell ici_ascii = ici_val.value;
        // Constant pool may not be numberized yet.
        //
        // check values before dereference on a trace.
        if (ici_ascii.ref == null) {
            environment.trace("<#cpx-unresolved> ");
        } else {
            if (ici_ascii.ref.value == null) {
                environment.trace("<#cpx-0> ");
            } else {
                environment.trace(ici_ascii.ref.value + " ");
            }
        }

        if (outerClass != pool.getCell(0)) {
            if (outerClass.cpIndex != 0) {
                ConstValue_Cell<?> oci_val = (ConstValue_Cell) outerClass.ref;
                ConstCell oci_ascii = oci_val.value;
                if (oci_ascii.ref == null) {
                    environment.trace("\n\t\tof <#cpx-unresolved>  ");
                } else {
                    ConstValue_UTF8 cval = (ConstValue_UTF8) oci_ascii.ref;
                    if (cval.value == null) {
                        environment.trace("\n\t\tof <#cpx-0>  ");
                    } else {
                        environment.trace("\n\t\tof " + cval.value);
                    }
                }
            }
        }
        environment.trace("\n");
    }

    private void pic_error() {
        // throw error, IC is not recognizable
        environment.error(scanner.pos, "err.invalid.innerclass");
        throw new SyntaxError();
    }

    /**
     * The match() method is used to quickly match opening
     * brackets (ie: '(', '{', or '[') with their closing
     * counterpart. This is useful during error recovery.<p>
     * <p>
     * Scan to a matching '}', ']' or ')'. The current scanner.token must be
     * a '{', '[' or '(';
     */
    private void match(Token open, Token close) throws IOException {
        int depth = 1;

        while (true) {
            scanner.scan();
            if (scanner.token == open) {
                depth++;
            } else if (scanner.token == close) {
                if (--depth == 0) {
                    return;
                }
            } else if (scanner.token == EOF) {
                environment.error(scanner.pos, "err.unbalanced.paren");
                return;
            }
        }
    }

    /**
     * Recover after a syntax error in a field. This involves
     * discarding scanner.tokens until an EOF or a possible legal
     * continuation is encountered.
     */
    private void recoverField() throws SyntaxError, IOException {
        while (true) {
            switch (scanner.token) {
                case EOF:
                case STATIC:
                case FINAL:
                case PUBLIC:
                case PRIVATE:
                case SYNCHRONIZED:
                case TRANSIENT:
                case PROTECTED:
                case VOLATILE:
                case NATIVE:
//                case INTERFACE: see below
                case ABSTRACT:
                case ANNOTATION_ACCESS:
                    // possible begin of a field, continue
                    return;

                case LBRACE:
                    match(LBRACE, RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(LPAREN, RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(LSQBRACKET, RSQBRACKET);
                    scanner.scan();
                    break;

                case RBRACE:
                case INTERFACE:
                case CLASS:
                case IMPORT:
                case PACKAGE:
                    // begin of something outside a class, panic more
                    endClass();
                    traceMethodInfoLn(format("scanner position %d", scanner.pos));
                    throw new SyntaxError().setFatal();
                default:
                    // don't know what to do, skip
                    scanner.scan();
                    break;
            }
        }
    }

    /**
     * Parse a class or interface declaration.
     */
    private void parseClass(int mod) throws IOException {
        int posa = scanner.pos;
        traceMethodInfoLn("Begin");
        // check access modifiers:
        Checker.checkClassModifiers(mod, scanner);
        if (clsAnnttns != null) {
            classData.addAnnotations(clsAnnttns);
        }

        // move the tokenizer to the identifier:
        if (scanner.token == CLASS) {
            scanner.scan();
        } else if (scanner.token == ANNOTATION) {
            scanner.scan();
            if (scanner.token == INTERFACE) {
                mod |= ACC_ANNOTATION.getFlag() | ACC_INTERFACE.getFlag();
                scanner.scan();
            } else {
                environment.error(scanner.prevPos, "err.one.of.two.token.expected",
                        ANNOTATION.parseKey(), INTERFACE.parseKey());
                throw new SyntaxError();
            }
        }

        // Parse the class name
        ConstCell nm = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);

        if (scanner.token == FIELD) { // DOT
            String fileExtension;
            scanner.scan();
            switch (scanner.token) {
                // CLASS token added to allow:
                // class ClassName.class version 45:0 {..}
                case STRINGVAL, CLASS -> fileExtension = scanner.stringValue;
                case IDENT -> fileExtension = scanner.idValue;
                default -> {
                    environment.error(scanner.pos, "err.name.expected", "\"" + scanner.token.parseKey() + "\"");
                    throw new SyntaxError();
                }
            }
            scanner.scan();
            classData.fileExtension = "." + fileExtension;
        } else if (scanner.token == MODULE) {
            environment.error(scanner.prevPos, "err.token.expected", "\"" + OPEN.parseKey() + "\"");
            throw new SyntaxError();
        } else if (scanner.token == SEMICOLON) {
            // drop the semicolon following a name
            scanner.scan();
        } else if (scanner.token == COLON) {
            // parse optional attribute Signature
            scanner.scan();
            parseClassSignature();
        }

        // Parse extends clause
        ConstCell sup = null;
        if (scanner.token == EXTENDS) {
            scanner.scan();
            // sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS); ignore keywords as much as possible:
            // class Module$NormalModule extends Module version 63:0
            sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);
            while (scanner.token == COMMA) {
                scanner.scan();
                environment.warning(posa, "warn.multiple.inherit");
                // sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS); ignore keywords as much as possible:
                // class Module$NormalModule extends Module version 63:0
                sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);
            }
        }

        // Parse implements clause
        ArrayList<Indexer> impl = new ArrayList<>();
        if (scanner.token == IMPLEMENTS) {
            do {
                scanner.scan();
                // Indexer intf = cpParser.parseConstRef(ConstType.CONSTANT_CLASS); ignore keywords as much as possible:
                // public interface AddressField implements Field version 63:0
                Indexer intf = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);
                if (impl.contains(intf)) {
                    environment.warning(posa, "warn.intf.repeated", intf);
                } else {
                    impl.add(intf);
                }
            } while (scanner.token == COMMA);
        }


        // parse version
        if (scanner.token == LBRACE) {
            // no version info in the source
            if (!classData.cfv.isSet() && !classData.cfv.isSetByParameter()) {
                // the version isn't set by params
                classData.cfv.initClassDefaultVersion();
                environment.warning(scanner.prevPos, "warn.default.cfv", classData.cfv.asString());
            }
        } else {
            if (classData.cfv.isSet() && classData.cfv.isSetByParameter() && classData.cfv.isFrozen()) {
                int minor = classData.cfv.minor_version();
                int major = classData.cfv.major_version();
                String version = classData.cfv.asString();
                String option = classData.cfv.isThresholdSet() ? classData.cfv.asThresholdString() : classData.cfv.asString();
                parseVersion();
                if (classData.cfv.isSetByParameter() &&
                        (major == classData.cfv.major_version() || minor == classData.cfv.minor_version()))
                    environment.warning(scanner.prevPos, "warn.isset.cfv", version, option);
            } else {
                parseVersion();
            }
        }
        scanner.expect(LBRACE);

        // Begin a new class
        classData.init(mod, nm, sup, impl);

        // Parse class members
        while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
            switch (scanner.token) {
                case SEMICOLON ->
                    // Empty fields are allowed
                        scanner.scan();
                case CONST -> {
                    scanner.scan();
                    parseConstDef();
                    explicitCP = true;
                }
                default ->   // scanner.token is some member.
                        parseClassMembers();
            }
        }
        scanner.expect(RBRACE);
        endClass();
    } // end parseClass

    /**
     * Parses a package or type name in a module statement(s)
     *
     * @return Pair  Either Package/Type name or CP Index for this Package/Type name.
     */
    private NameInfo parseTypeName() throws IOException {
        String name = "", field = "";
        int cpIndex = 0;
        if (scanner.token == IDENT) {
            while (true) {
                if (scanner.token.isPossibleClassName()) {
                    name = name + field + scanner.idValue;
                    scanner.scan();
                } else {
                    environment.error(scanner.pos, "err.name.expected", "\"" + scanner.token.parseKey() + "\"");
                    throw new SyntaxError();
                }
                if (scanner.token == FIELD) {
                    environment.warning(scanner.pos, "warn.dot.will.be.converted");
                    field = "/";
                    scanner.scan();
                } else {
                    break;
                }
            }
        } else if (scanner.token == CPINDEX) {
            cpIndex = scanner.intValue;
            scanner.scan();
        }
        return new NameInfo(cpIndex, name);
    }

    /**
     * Parses a module name in a module statement(s)
     *
     * @return Pair  Either Module name or CP Index for the module name.
     */
    private NameInfo parseModuleName() throws IOException {
        String field = "";
        NameInfo nameInfo = new NameInfo();
        if (scanner.token == IDENT) {
            while (true) {
                if (scanner.token.isPossibleModuleName()) {
                    nameInfo.setName(nameInfo.name() + field + scanner.idValue);
                    scanner.scanModuleStatement();
                } else {
                    environment.error(scanner.pos, "err.module.name.expected", "\"" + scanner.token.parseKey() + "\"");
                    throw new SyntaxError().setFatal();
                }
                if (scanner.token == FIELD) {
                    field = Character.toString((char) scanner.token.value());
                    scanner.scanModuleStatement();
                } else {
                    break;
                }
            }
        } else if (scanner.token == CPINDEX) {
            nameInfo.setCpIndex(scanner.intValue);
            scanner.scan();
        } else {
            environment.error(scanner.pos, "err.module.name.expected", "\"" + scanner.token.parseKey() + "\"");
            throw new SyntaxError().setFatal();
        }
        return nameInfo;
    }

    /**
     * Parse a module declaration.
     */
    private void parseModule(int mod) throws IOException {
        traceMethodInfoLn("Begin");
        if (mod != 0) {
            environment.warning(scanner.pos, "warn.modifiers.ignored", EModifier.asNames(mod, ClassFileContext.MODULE));
        }
        if (clsAnnttns != null) {
            classData.addAnnotations(clsAnnttns);
        }
        moduleAttribute = new ModuleAttr(classData);

        if (scanner.token == OPEN) {
            moduleAttribute.openModule();
            scanner.scan();
        }

        // move the tokenizer to the identifier:
        if (scanner.token == MODULE) {
            scanner.scanModuleStatement();
            // scanner.scan();
        } else {
            environment.error(scanner.pos, "err.token.expected", MODULE.parseKey());
            throw new SyntaxError().setFatal();
        }
        // Parse the module name
        NameInfo moduleNameInfo = parseModuleName();
        if (moduleNameInfo.isEmpty()) {
            environment.error(scanner.pos, "err.name.expected", "\"" + scanner.token + "\"");
            throw new SyntaxError().setFatal();
        }
        if (moduleNameInfo.cpIndex() != 0) {
            moduleAttribute.setModuleNameCpIndex(moduleNameInfo.cpIndex());
        } else {
            moduleAttribute.setModuleName(moduleNameInfo.name());
        }
        // parse version
        if (scanner.token == LBRACE) {
            classData.cfv.initModuleDefaultVersion();
            environment.warning(scanner.pos, "warn.default.cfv", classData.cfv.asString());
        } else {
            parseVersion();
        }
        scanner.expect(LBRACE);

        // Begin a new class as module
        classData.initAsModule();

        // Parse module statement(s)
        while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
            switch (scanner.token) {
                case CONST -> {
                    // Parse constant declarations
                    scanner.scan();
                    parseConstDef();
                    explicitCP = true;
                }
                case REQUIRES -> scanRequires(moduleAttribute.requires);
                case EXPORTS -> scanStatement(moduleAttribute.exports,
                        this::parseTypeName,
                        this::parseModuleName,
                        TO,
                        true,
                        "err.exports.expected");
                case OPENS -> scanStatement(moduleAttribute.opens,
                        this::parseTypeName,
                        this::parseModuleName,
                        TO, true, "err.opens.expected");
                case PROVIDES -> scanStatement(moduleAttribute.provides,
                        this::parseTypeName,
                        this::parseTypeName,
                        WITH,
                        false,
                        "err.provides.expected");
                case USES -> scanStatement(moduleAttribute.uses, "err.uses.expected");
                case SEMICOLON ->
                    // Empty fields are allowed
                        scanner.scan();
                default -> {
                    environment.error(scanner.pos, "err.module.statement.expected");
                    throw new SyntaxError().setFatal();
                }
            }  // end switch
        } // while
        scanner.expect(RBRACE);
        // End of the module
        endModule();
    } // end parseModule

    /**
     * Scans  ModuleStatement: requires [transitive|static|mandated|synthetic] ModuleName ;
     * Scans  ModuleStatement: requires [transitive|static|mandated|synthetic] #ref ;
     */
    private void scanRequires(Consumer<ModuleContent.Dependence> action) throws IOException {
        int flags = 0;
        NameInfo moduleNameInfo = new NameInfo();
        scanner.scanModuleStatement();
        while (scanner.token != SEMICOLON) {
            switch (scanner.token) {
                case STATIC -> {
                    if (EModifier.isStaticPhase(flags) || !moduleNameInfo.isEmpty()) {
                        environment.error(scanner.pos, "err.requires.expected");
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_STATIC_PHASE.getFlag();
                }
                case TRANSITIVE -> {
                    if (EModifier.isTransitive(flags) || !moduleNameInfo.isEmpty()) {
                        environment.error(scanner.pos, "err.requires.expected");
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_TRANSITIVE.getFlag();
                }
                case SYNTHETIC -> {
                    if (EModifier.isSynthetic(flags) || !moduleNameInfo.isEmpty()) {
                        environment.error(scanner.pos, "err.requires.expected");
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_SYNTHETIC.getFlag();
                }
                case MANDATED -> {
                    if (EModifier.isMandated(flags) || !moduleNameInfo.isEmpty()) {
                        environment.error(scanner.pos, "err.requires.expected");
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_MANDATED.getFlag();
                }
                case IDENT, CPINDEX -> {
                    if (!moduleNameInfo.isEmpty()) {
                        environment.error(scanner.pos, "err.requires.expected");
                        throw new SyntaxError().setFatal();
                    }
                    moduleNameInfo = parseModuleName();
                    continue;
                }
                default -> {
                    environment.error(scanner.pos, "err.requires.expected");
                    throw new SyntaxError().setFatal();
                }
            }
            scanner.scanModuleStatement();
        }
        // SEMICOLON
        if (moduleNameInfo.isEmpty()) {
            environment.error(scanner.pos, "err.requires.expected");
            throw new SyntaxError().setFatal();
        }
        action.accept(new ModuleContent.Dependence(moduleNameInfo.cpIndex(), moduleNameInfo.name(), flags, null));
        scanner.scanModuleStatement();
    }

    /**
     * Scans  Module Statement(s):
     * exports  [mandated|synthetic] packageName [to ModuleName {, ModuleName}*] ;
     * opens    [mandated|synthetic] packageName [to ModuleName {, ModuleName}*] ;
     * provides TypeName with TypeName {,typeName} ;
     */
    private <T extends ModuleContent.TargetType> void scanStatement(
            BiConsumer<T, Set<ModuleContent.TargetType>> action,
            NameSupplier source,
            NameSupplier target,
            Token startList,
            boolean emptyListAllowed,
            String err) throws IOException {
        boolean isProvidesStatement = (startList == WITH);
        int flags = 0; // [mandated|synthetic]
        NameInfo typeNameInfo = new NameInfo();
        HashSet<NameInfo> nameInfos = new HashSet<>(); // to ModuleName {, ModuleName}* Or with TypeName {,typeName}*
        scanner.scan();
        while (scanner.token != SEMICOLON) {
            switch (scanner.token) {
                case SYNTHETIC -> {
                    if (EModifier.isSynthetic(flags) || !typeNameInfo.isEmpty() || isProvidesStatement) {
                        environment.error(scanner.pos, err);
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_SYNTHETIC.getFlag();
                }
                case MANDATED -> {
                    if (EModifier.isMandated(flags) || !typeNameInfo.isEmpty() || isProvidesStatement) {
                        environment.error(scanner.pos, err);
                        throw new SyntaxError().setFatal();
                    }
                    flags |= EModifier.ACC_MANDATED.getFlag();
                }
                case IDENT, CPINDEX -> {
                    if (!typeNameInfo.isEmpty()) {
                        environment.error(scanner.pos, err);
                        throw new SyntaxError().setFatal();
                    }
                    typeNameInfo = source.get();
                    continue;
                }
                case TO, WITH -> {        // to[with]
                    nameInfos = scanList(isProvidesStatement ? () -> scanner.scan() : () -> scanner.scanModuleStatement(),
                            target, err, false);
                    continue;
                }
                default -> {
                    environment.error(scanner.pos, err);
                    throw new SyntaxError().setFatal();
                }
            }
            if (isProvidesStatement)
                scanner.scan();
            else
                scanner.scanModuleStatement();
        }
        // SEMICOLON
        if (typeNameInfo.isEmpty() || (nameInfos.isEmpty() && !emptyListAllowed)) {
            environment.error(scanner.pos, err);
            throw new SyntaxError().setFatal();
        }
        if (isProvidesStatement) {
            // provides <Class> with  Class(es)
            Set<ModuleContent.TargetType> classes = nameInfos.stream().
                    map(nameInfo -> new ModuleContent.TargetType(CONSTANT_CLASS, nameInfo.cpIndex(), nameInfo.name())).
                    collect(Collectors.toSet());
            action.accept(
                    (T) (new ModuleContent.FlaggedTargetType(CONSTANT_CLASS, typeNameInfo.cpIndex(), typeNameInfo.name(), flags, ClassFileContext.MODULE)),
                    classes);
        } else {
            // <export, open> Package to Module(s)
            Set<ModuleContent.TargetType> modules = nameInfos.stream().
                    map(nameInfo -> new ModuleContent.TargetType(CONSTANT_MODULE, nameInfo.cpIndex(), nameInfo.name())).
                    collect(Collectors.toSet());
            action.accept(
                    (T) (new ModuleContent.FlaggedTargetType(CONSTANT_PACKAGE, typeNameInfo.cpIndex(), typeNameInfo.name(), flags, ClassFileContext.MODULE)),
                    modules);
        }
        scanner.scan();
    }

    /**
     * Scans  ModuleStatement: uses TypeName;
     */
    private void scanStatement(Consumer<ModuleContent.TargetType> action, String err) throws IOException {
        HashSet<NameInfo> nameInfos = scanList(() -> scanner.scan(), this::parseTypeName, err, true);
        // SEMICOLON
        if (nameInfos.size() != 1) {
            environment.error(scanner.pos, err);
            throw new SyntaxError().setFatal();
        }
        nameInfos.
                forEach(nameInfo -> action.accept(new ModuleContent.TargetType(CONSTANT_CLASS, nameInfo.cpIndex(), nameInfo.name())));
        scanner.scan();
    }

    /**
     * Scans the "to" or "with" part of the following ModuleStatement:
     * exports  PackageName   [to  ModuleName {, ModuleName}] ;,
     * opens    PackageName   [to  ModuleName {, ModuleName}] ;
     * provides TypeName with TypeName [,typeName] ;
     * uses     TypeName;
     * <p>
     * : [ModuleName {, ModuleName}]; , [TypeName [,typeName]]; or TypeName;
     */
    private HashSet<NameInfo> scanList(Method scanMethod, NameSupplier target,
                                       String err, boolean onlyOneElement) throws IOException {
        HashSet<NameInfo> nameInfos = new HashSet<>();
        boolean comma = false, first = true;
        scanMethod.call();
        while (scanner.token != SEMICOLON) {
            switch (scanner.token) {
                case COMMA -> {
                    if (comma || first || onlyOneElement) {
                        environment.error(scanner.pos, err);
                        throw new SyntaxError().setFatal();
                    }
                    comma = true;
                }
                case IDENT, CPINDEX -> {
                    if (!first && !comma) {
                        environment.error(scanner.pos, err);
                        throw new SyntaxError().setFatal();
                    }
                    nameInfos.add(target.get());
                    comma = false;
                    first = false;
                    continue;
                }
                default -> {
                    environment.error(scanner.pos, err);
                    throw new SyntaxError().setFatal();
                }
            }
            scanner.scan();
        }
        // SEMICOLON
        if (nameInfos.isEmpty() || comma) {
            environment.error(scanner.pos, err);
            throw new SyntaxError().setFatal();
        }
        return nameInfos;
    }

    private void parseClassMembers() throws IOException {
        traceMethodInfoLn("Begin");
        // Parse annotations
        if (scanner.token == ANNOTATION) {
            memberAnnttns = annotParser.scanAnnotations();
        }
        // Parse modifiers
        int mod = scanModifiers();
        try {
            switch (scanner.token) {
                case FIELDREF -> {
                    scanner.scan();
                    parseField(mod);
                }
                case METHODREF -> {
                    scanner.scan();
                    parseMethod(mod);
                }
                case INNERCLASS -> {
                    scanner.scan();
                    parseInnerClass(mod);
                }
                case BOOTSTRAPMETHOD -> {
                    scanner.scan();
                    parseCPXBootstrapMethod();
                }
                case SIGNATURE -> {
                    if (classData.signatureAttr != null) {
                        environment.error(scanner.pos, "err.extra.attribute", SIGNATURE.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseClassSignature();
                    scanner.expect(SEMICOLON);
                }
                //
                case THIS_CLASS -> {
                    scanner.scan();
                    parseClassRef(constCell -> classData.coreClasses.this_class(CLASSFILE, constCell));
                    scanner.expect(SEMICOLON);
                }
                case SUPER_CLASS -> {
                    scanner.scan();
                    parseClassRef(constCell -> classData.coreClasses.super_class(CLASSFILE, constCell));
                    scanner.expect(SEMICOLON);
                }
                //
                case SOURCEFILE -> {
                    if (classData.sourceFileAttr != null) {
                        environment.error(scanner.pos, "err.extra.attribute", SOURCEFILE.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseSourceFile();
                    scanner.expect(SEMICOLON);
                }
                case SOURCEDEBUGEXTENSION -> {
                    if (classData.sourceDebugExtensionAttr != null) {
                        environment.error(scanner.pos, "err.extra.attribute", SOURCEDEBUGEXTENSION.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseSourceDebugExtension();
                }
                //
                case NESTHOST -> {
                    if (classData.nestHostAttributeExists()) {
                        environment.error(scanner.pos, "err.extra.attribute", NESTHOST.parseKey(), "ClassData");
                        throw new SyntaxError();
                    } else if (classData.nestMembersAttributesExist()) {
                        environment.error(scanner.pos, "err.both.nesthost.nestmembers.found");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseNestHost();
                }
                case NESTMEMBERS -> {
                    if (classData.nestMembersAttributesExist()) {
                        environment.error(scanner.pos, "err.extra.attribute", NESTMEMBERS.parseKey(), "ClassData");
                        throw new SyntaxError();
                    } else if (classData.nestHostAttributeExists()) {
                        environment.error(scanner.pos, "err.both.nesthost.nestmembers.found");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> classData.addNestMembers(list));
                }
                case PERMITTEDSUBCLASSES -> {         // JEP 360
                    if (classData.nestMembersAttributesExist()) {
                        environment.error(scanner.pos, "err.extra.attribute", PERMITTEDSUBCLASSES.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> classData.addPermittedSubclasses(list));
                }
                case RECORD -> {                    // JEP 359
                    if (classData.recordAttributeExists()) {
                        environment.error(scanner.pos, "err.extra.attribute", RECORD.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseRecord();
                }
                case PRELOAD -> {
                    if (classData.preloadAttributeExists()) {
                        environment.error(scanner.pos, "err.extra.attribute", PRELOAD.parseKey(), "ClassData");
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    parseClasses(list -> classData.addPreloads(list));
                }
                default -> {
                    environment.error(scanner.pos, "err.field.expected");
                    throw new SyntaxError().setFatal();
                }
            }  // end switch
        } catch (SyntaxError e) {
            if (!e.isFatal()) {
                recoverField();
            } else {
                throw new SyntaxError().setFatal();
            }
        }
        traceMethodInfoLn("End");
        memberAnnttns = null;
    }

    /**
     * Recover after a syntax error in the file.
     * This involves discarding scanner.tokens until an EOF
     * or a possible legal continuation is encountered.
     */
    private void recoverFile() throws IOException {
        while (true) {
            environment.traceln("recoverFile: scanner.token=" + scanner.token);
            switch (scanner.token) {
                case CLASS:
                case INTERFACE:
                    // Start of a new source file statement, continue
                    return;

                case LBRACE:
                    match(LBRACE, RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(LPAREN, RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(LSQBRACKET, RSQBRACKET);
                    scanner.scan();
                    break;

                case EOF:
                    return;

                default:
                    // Don't know what to do, skip
                    scanner.scan();
                    break;
            }
        }
    }

    /**
     * End class
     */
    private void endClass() {
        if (explicitCP) {
            // Fix references in the constant pool (for explicitly coded CPs)
            // TODO Synthetic in package info
            pool.fixRefsInPool();
            // Fix any bootstrap Method references too
            classData.relinkBootstrapMethods();

            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                String sourceFileName = environment.getSimpleInputFileName();
                String sourceName = environment.getSourceName();
                classData.sourceFileAttr = new SourceFileAttr(this.classData.pool, sourceFileName).
                        updateIfFound(this.classData.pool,
                                name -> name.contains(sourceName) &&
                                        StringUtils.contains.apply(name, List.of(".java", ".jcod", ".jasm", ".class"))
                        );
            }
        } else {
            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                classData.sourceFileAttr = new CPXAttr(pool,
                        EAttribute.ATT_SourceFile,
                        pool.findUTF8Cell(environment.getSimpleInputFileName()));
            }
        }
        classData.endClass();
        clsDataList.add(classData);
        initializeClassData();
    }

    /**
     * End package-info
     */
    private void endPackageInfo() {
        if (explicitCP) {
            // Fix references in the constant pool (for explicitly coded CPs)
            pool.fixRefsInPool();
            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                String sourceName = environment.getSimpleInputFileName();
                classData.sourceFileAttr = new SourceFileAttr(this.classData.pool, sourceName).
                        updateIfFound(this.classData.pool, name -> name.contains("package-info."));
            }
        } else {
            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                classData.sourceFileAttr = new CPXAttr(pool,
                        EAttribute.ATT_SourceFile,
                        pool.findUTF8Cell(environment.getSimpleInputFileName()));
            }
        }
        classData.endPackageInfo();
        clsDataList.add(classData);
        classData = null;
    }

    /**
     * End module
     */
    private void endModule() {
        if (explicitCP) {
            // Fix references in the constant pool (for explicitly coded CPs)
            pool.fixRefsInPool();
            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                String sourceName = environment.getSimpleInputFileName();
                classData.sourceFileAttr = new SourceFileAttr(this.classData.pool, sourceName).
                        updateIfFound(this.classData.pool, name -> name.contains("module-info."));
            }
        } else {
            // Fix Source file if it isn't defined.
            if (classData.sourceFileAttr == null) {
                classData.sourceFileAttr = new CPXAttr(pool,
                        EAttribute.ATT_SourceFile,
                        pool.findUTF8Cell(environment.getSimpleInputFileName()));
            }
        }
        classData.endModule(moduleAttribute);
        clsDataList.add(classData);
        classData = null;
    }

    final ClassData[] getClassesData() {
        return clsDataList.toArray(new ClassData[0]);
    }

    /**
     * Determines whether the JASM file is for a package-info class
     * or for a module-info class.
     * <p>
     * creates the correct kind of ClassData accordingly.
     *
     * @throws IOException if any parse exception is met
     */
    private void parseJasmPackages() throws IOException {
        if (scanner.token.in(CONST, ANNOTATION, PACKAGE)) {
            boolean scanNext = true;
            try {
                while ((scanner.token != EOF) && scanNext) {
                    switch (scanner.token) {
                        case CONST:
                            // Parse constant declarations
                            scanner.scan();
                            parseConstDef();
                            explicitCP = true;
                            break;
                        case SEMICOLON:
                            // Empty fields are allowed
                            scanner.scan();
                            break;
                        case ANNOTATION:
                            pkgAnnttns = annotParser.scanAnnotations();
                            break;
                        case PACKAGE:
                            // Package statement
                            scanner.scan();
                            int where = scanner.pos;
                            String id = parseIdent();
                            if (scanner.token != SEMICOLON) {
                                parseVersion();
                                scanner.expect(SEMICOLON);
                            }
                            if (pkg == null) {
                                pkg = id;
                                pkgPrefix = id + "/";
                            } else {
                                environment.error(where, "err.package.repeated");
                            }
                            traceMethodInfoLn("{PARSED} package-prefix: \"" + pkgPrefix + "\"");
                        default:
                            scanNext = false;
                    }  // end switch
                } // while
            } catch (SyntaxError e) {
                recoverFile();
            }

            // skip bogus semi colons
            while (scanner.token == SEMICOLON) {
                scanner.scan();
            }

            // checks that we compile module or package compilation unit
            if (scanner.token == EOF) {
                environment.traceln("Scanner:  EOF");
                String sourceName = environment.getSimpleInputFileName();
                // package-info
                if (sourceName.contains("package-info")) {
                    environment.traceln("Creating \"package-info.jasm\": package: " + pkg + " " + classData.cfv.asString());
                    // Interface package-info should be marked ACC_INTERFACE and ACC_ABSTRACT flags
                    int mod = ACC_INTERFACE.getFlag() | ACC_ABSTRACT.getFlag();
                    // If the class file version number is less than 50.0, then the ACC_SYNTHETIC flag is unset;
                    // if the class file version number is 50.0 or above, then the ACC_SYNTHETIC flag is set.
                    if (classData.cfv.major_version() > 49) {
                        mod |= SYNTHETIC_ATTRIBUTE.getFlag();
                    }
                    classData.initAsPackageInfo(mod, pkgPrefix + "package-info");
                    if (pkgAnnttns != null) {
                        classData.addAnnotations(pkgAnnttns);
                    }
                    endPackageInfo();
                }
                return;
            }
            if (pkg == null && pkgAnnttns != null) { // RemoveModules
                clsAnnttns = pkgAnnttns;
                pkgAnnttns = null;
            }
        }
    }

    /**
     * Parse an Jasm file.
     */
    void parseFile() {
        try {
            initializeClassData();
            // First, parse any package identifiers (and associated package annotations)
            parseJasmPackages();

            while (scanner.token != EOF) {
                // Second, parse any class identifiers (and associated class annotations)
                try {
                    // Parse annotations
                    if (scanner.token == ANNOTATION) {
                        clsAnnttns = annotParser.scanAnnotations();
                    }

                    // Parse class modifiers
                    int mod = scanModifiers();
                    if (mod == 0) {
                        switch (scanner.token) {
                            case OPEN:
                            case MODULE:
                            case CLASS:
                            case INTERFACE:
                            case CPINDEX:
                            case STRINGVAL:
                            case IDENT:
                                // this is a class declaration anyway
                                break;
                            case SEMICOLON:
                                // Bogus semi colon
                                scanner.scan();
                                continue;
                            default:
                                // no class declaration found
                                environment.error(scanner.pos, "err.toplevel.expected");
                                throw new SyntaxError();
                        }
                    } else if (EModifier.isInterface(mod) && (scanner.token != CLASS)) {
                        // rare syntactic sugar:
                        // interface <ident> == abstract interface class <ident>
                        mod |= ACC_ABSTRACT.getFlag();
                    }
                    if (scanner.token == MODULE || scanner.token == OPEN)
                        parseModule(mod);
                    else
                        parseClass(mod);
                    clsAnnttns = null;

                } catch (SyntaxError e) {
                    environment.traceln("^^^^^^^ Syntax Error ^^^^^^^^^^^^");
                    if (scanner.environment.getVerboseFlag())
                        e.printStackTrace();
                    if (!e.isFatal()) {
                        recoverFile();
                    }
                    break;
                }
            }
        } catch (IOException e) {
            environment.error(scanner.pos, "io.exception", environment.getSimpleInputFileName());
        } catch (Error er) {
            er.printStackTrace();
        }
    } //end parseFile

    private void initializeClassData() {
        classData = new ClassData(this.environment, copyOf(this.currentCFV));
        pool = classData.pool;
    }

    @FunctionalInterface
    interface NameSupplier {
        NameInfo get() throws IOException;
    }

    @FunctionalInterface
    interface Method {
        void call() throws IOException;
    }

    /**
     * The main compile error for the parser
     */
    static class CompilerError extends Error {
        CompilerError(String message) {
            super(message);
        }
    }
}  //end Parser
