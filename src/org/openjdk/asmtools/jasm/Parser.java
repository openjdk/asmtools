/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.Constants.DEFAULT_MAJOR_VERSION;
import static org.openjdk.asmtools.jasm.Constants.DEFAULT_MINOR_VERSION;
import static org.openjdk.asmtools.jasm.ConstantPool.*;
import static org.openjdk.asmtools.jasm.RuntimeConstants.*;
import static org.openjdk.asmtools.jasm.Tables.*;
import static org.openjdk.asmtools.jasm.JasmTokens.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used to parse Jasm statements and expressions.
 * The result is a parse tree.<p>
 *
 * This class implements an operator precedence parser. Errors are
 * reported to the Environment object, if the error can't be
 * resolved immediately, a SyntaxError exception is thrown.<p>
 *
 * Error recovery is implemented by catching Scanner.SyntaxError exceptions
 * and discarding input scanner.tokens until an input token is reached that
 * is possibly a legal continuation.<p>
 *
 * The parse tree that is constructed represents the input
 * exactly (no rewrites to simpler forms). This is important
 * if the resulting tree is to be used for code formatting in
 * a programming environment. Currently only documentation comments
 * are retained.<p>
 *
 * A parser owns several components (scanner, constant-parser,
 * instruction-parser, annotations-parser) to which it delegates certain
 * parsing responsibilities.  This parser contains functions to parse the
 * overall form of a class, and any members (fields, methods, inner-classes).
 * <p>
 *
 * Syntax errors, should always be caught inside the
 * parser for error recovery.
 */
class Parser extends ParseBase {

  /*-------------------------------------------------------- */
  /* Annotation Inner Classes */
    /**
     * The main compile error for the parser
     */
    public static class CompilerError extends Error {

        CompilerError(String message) {
            super(message);
        }
    }
  /*-------------------------------------------------------- */
  /* Parser Fields */

    private ArrayList                   clsDataList = new ArrayList<>();
    protected ClassData                 cd = null;
    protected ConstantPool              pool = null;
    private MethodData                  curMethod;
    protected CodeAttr                  curCode;
    private String                      pkg = null;
    private String                      pkgPrefix = "";
    private ArrayList<AnnotationData>   pkgAnnttns = null;
    /* RemoveModules
    private String mdl = null;
    private ArrayList<AnnotationData> mdlAnnttns = null;
     */
    private ArrayList<AnnotationData>   clsAnnttns = null;
    private ArrayList<AnnotationData>   memberAnnttns = null;
    private short                       major_version = DEFAULT_MAJOR_VERSION;
    private short                       minor_version = DEFAULT_MINOR_VERSION;
    private boolean                     explicitcp = false;

    /** other parser components */
    private ParserAnnotation            annotParser = null;     // For parsing Annotations
    private ParserCP                    cpParser    = null;     // for parsing Constants
    private ParserInstr                 instrParser = null;     // for parsing Instructions


   /*-------------------------------------------------------- */


    /**
     * Create a parser
     */
    /*
    protected Parser(Environment sf) throws IOException {
        this.scanner = new Scanner(sf);
    }
    * */

    protected Parser(Environment sf, short major_version, short minor_version) throws IOException {
        super.init(new Scanner(sf), this, sf);
        this.major_version = major_version;
        this.minor_version = minor_version;
        this.annotParser = new ParserAnnotation(scanner, this, env);
        this.cpParser = new ParserCP(scanner, this, env);
        this.instrParser = new ParserInstr(scanner, this, cpParser, env);
    }


    protected void setDebugFlags(boolean debugScanner, boolean debugMembers,
            boolean debugCP, boolean debugAnnot, boolean debugInstr) {

        enableDebug(debugMembers);
        scanner.enableDebug(debugScanner);
        cpParser.enableDebug(debugCP);
        annotParser.enableDebug(debugAnnot);
        instrParser.enableDebug(debugInstr);
    }




    /*---------------------------------------------*/

    protected String encodeClassString(String classname) {
        return "L" + classname + ";";
    }

    /**
     * Parses version in package statements
     */

    protected final void parseVersionPkg() throws IOException  {
        if (scanner.token == Token.SEMICOLON) {
            return;
        }
        parse_ver: {
            if (scanner.token != Token.VERSION) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            major_version = (short)scanner.intValue;
            scanner.scan();
            if (scanner.token != Token.COLON) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            minor_version = (short)scanner.intValue;
            scanner.scan();
            debugScan("     [Parser.parseVersionPkg]: " + major_version + ":" + minor_version);
            return;
        }
        env.error(scanner.pos, "version.expected");
        throw new Scanner.SyntaxError();
    }

    protected final void parseVersion() throws IOException  {
        if (scanner.token == Token.LBRACE) {
            return;
        }
        parse_ver: {
            if (scanner.token != Token.VERSION) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            cd.major_version = (short)scanner.intValue;
            scanner.scan();
            if (scanner.token != Token.COLON) {
                break parse_ver;
            }
            scanner.scan();
            if (scanner.token != Token.INTVAL) {
                break parse_ver;
            }
            cd.minor_version = (short)scanner.intValue;
            scanner.scan();
            debugStr("parseVersion: " + cd.major_version + ":" + cd.minor_version);
            return;
        }
        env.error(scanner.pos, "version.expected");
        throw new Scanner.SyntaxError();
    }

    /**
     * Parse an internal name: identifier.
     */
    protected String parseIdent() throws Scanner.SyntaxError, IOException {
        String v = scanner.idValue;
        scanner.expect(Token.IDENT);
        return v;
    }

    /**
     * Parse a local variable
     */
    protected void parseLocVarDef() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            curCode.LocVarDataDef(v);
        } else {
            String name = scanner.stringValue, type;
            scanner.expect(Token.IDENT);
            if (scanner.token == Token.COLON) {
                scanner.scan();
                type = parseIdent();
            } else {
                type = "I";                  // TBD
            }
            curCode.LocVarDataDef(name, pool.FindCellAsciz(type));
        }
    }

    protected Argument parseLocVarRef() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            return new Argument(v);
        } else {
            String name = scanner.stringValue;
            scanner.expect(Token.IDENT);
            return curCode.LocVarDataRef(name);
        }
    }

    protected void parseLocVarEnd() throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.INTVAL) {
            int v = scanner.intValue;
            scanner.scan();
            curCode.LocVarDataEnd(v);
        } else {
            String name = scanner.stringValue;
            scanner.expect(Token.IDENT);
            curCode.LocVarDataEnd(name);
        }
    }

    protected void parseMapItem(DataVector map) throws Scanner.SyntaxError, IOException {
        StackMapType itemType = stackMapType(scanner.intValue);
        ConstType tag = null;
        Argument arg = null;
        Token ptoken = scanner.token;
        int iValue = scanner.intValue;
        String sValue = scanner.stringValue;
        scanner.scan();
        resolve: {
            switch (ptoken) {
                case INTVAL:
                    break resolve;
                case CLASS:
                    itemType = StackMapType.ITEM_Object;
                    tag = ConstType.CONSTANT_CLASS;
                    break resolve;
                case CPINDEX:
                    itemType = StackMapType.ITEM_Object;
                    arg = pool.getCell(iValue);
                    break resolve;
                case IDENT:
                    itemType = stackMapType(sValue);
                    ConstType tg = Tables.tag(sValue);
//                    tag = (tg == null) ? 0 : tg.value;
                    tag = tg;
                    if (itemType != null) { // itemType OK
                        if ((tag != null) // ambiguity: "int," or "int 77,"?
                                && (scanner.token != Token.SEMICOLON)
                                && (scanner.token != Token.COMMA)) {
                            itemType=StackMapType.ITEM_Object;
                        }
                        break resolve;
                    } else if (tag != null) { // tag OK
                        itemType=StackMapType.ITEM_Object;
                        break resolve;
                    }
            }
            // resolution failed:
            itemType = StackMapType.ITEM_Bogus;
            env.error("itemtype.expected", "<" + ptoken.printval() + ">");
        }
        switch (itemType) {
            case ITEM_Object:  // followed by CP index
                if (arg == null) {
                    arg = pool.FindCell(cpParser.parseConstValue(tag));
                }
                map.addElement(new StackMapData.StackMapItem2(itemType, arg));
                break;
            case ITEM_NewObject:  // followed by label
                arg = instrParser.parseLabelRef();
                map.addElement(new StackMapData.StackMapItem2(itemType, arg));
                break;
            default:
                map.addElement(new StackMapData.StackMapItem1(itemType));
        }
    }


    /**
     * Parse an external name: CPINDEX, string, or identifier.
     */
    protected ConstCell parseName() throws Scanner.SyntaxError, IOException {
        debugScan("------- [Parser.parseName]: ");
        String v;
        switch (scanner.token) {
            case CPINDEX: {
                int cpx = scanner.intValue;
                scanner.scan();
                return pool.getCell(cpx);
            }
            case STRINGVAL:
                v = scanner.stringValue;
                scanner.scan();
                return pool.FindCellAsciz(v);

                // In many cases, Identifiers can correctly have the same
                // names as keywords.  We need to allow these.
            case SYNTHETIC:
            case DEPRECATED:
            case VERSION:
            case BITS:
            case STACK:
            case LOCAL:
            case OF:
            case NAN:
            case INNERCLASS:
            case STRICT:
            case FIELDREF:
            case METHODREF:
            case IDENT:
            case BRIDGE:
                v = scanner.idValue;
                scanner.scan();
                return pool.FindCellAsciz(v);
            default:
                env.error(scanner.pos, "name.expected", scanner.token);
                throw new Scanner.SyntaxError();
        }
    }

    /**
     * Parses a field or method reference for method handle.
     */
    protected ConstCell parseMethodHandle(SubTag subtag) throws Scanner.SyntaxError, IOException {
        ConstCell refCell;
        switch (subtag) {
            case REF_GETFIELD: case REF_GETSTATIC:
            case REF_PUTFIELD: case REF_PUTSTATIC:
                refCell = pool.FindCell(cpParser.parseConstValue(ConstType.CONSTANT_FIELD));
                break;
            case REF_INVOKEVIRTUAL: case REF_INVOKESTATIC:
            case REF_INVOKESPECIAL: case REF_NEWINVOKESPECIAL:
                refCell = pool.FindCell(cpParser.parseConstValue(ConstType.CONSTANT_METHOD));
                break;
            case REF_INVOKEINTERFACE:
                refCell = pool.FindCell(cpParser.parseConstValue(ConstType.CONSTANT_INTERFACEMETHOD));
                break;
            default:
                // should not reach
                throw new Scanner.SyntaxError();
        }
        return refCell;
    }

    /**
     * Parses a sub-tag value in method handle.
     */
    protected SubTag parseSubtag() throws Scanner.SyntaxError, IOException {
        SubTag subtag = null;
        switch (scanner.token) {
            case IDENT:
                subtag = subtag(scanner.stringValue);
                break;
            case INTVAL:
                subtag  = subtag(scanner.intValue);
                break;
        }
        if (subtag == null) {
            env.error("subtag.expected");
            throw new Scanner.SyntaxError();
        }
        scanner.scan();
        return subtag;
    }

    protected ConstCell parseClassName(boolean uncond) throws Scanner.SyntaxError, IOException {
        String v;
        switch (scanner.token) {
            case CPINDEX: {
                int cpx = scanner.intValue;
                scanner.scan();
                return pool.getCell(cpx);
            }
            case STRINGVAL:
                v = scanner.stringValue;
                scanner.scan();
                return pool.FindCellAsciz(v);
                // Some identifiers might coincide with token names.
                // these should be OK to use as identifier names.
            case SYNTHETIC:
            case DEPRECATED:
            case VERSION:
            case BITS:
            case STACK:
            case LOCAL:
            case OF:
            case NAN:
            case INNERCLASS:
            case STRICT:
            case FIELDREF:
            case METHODREF:
            case BRIDGE:
            case IDENT:
                v = scanner.idValue;
                scanner.scan();
                if (uncond || (scanner.token == Token.FIELD)) {  //?????????????????????????????????????
                    if ((v.indexOf("/") == -1)           // class identifier doesn't contain "/"
                            && (v.indexOf("[")==-1)){    // class identifier doesn't contain "["
                        v = pkgPrefix + v; // add package
                    }
                }
                return pool.FindCellAsciz(v);
            default:
                ConstType key = Tables.tag(scanner.token.value());
                env.traceln("%%%%% Unrecognized token [" + scanner.token + "]: '" + (key == null? "null":key.parseKey()) + "'.");
                env.error(scanner.prevPos, "name.expected");
                throw new Scanner.SyntaxError();
        }
    }


    /**
     * Parse a signed integer of size bytes long.
     *  size = 1 or 2
     */
    protected Argument parseInt(int size) throws Scanner.SyntaxError, IOException {
        if (scanner.token == Token.BITS) {
            scanner.scan();
        }
        if (scanner.token != Token.INTVAL) {
            env.error(scanner.pos, "int.expected");
            throw new Scanner.SyntaxError();
        }
        int arg = scanner.intValue * scanner.sign;
        switch (size) {
            case 1:
//                if ((arg>127)||(arg<-128)) { // 0xFF not allowed
                if ((arg > 255) || (arg < -128)) { // to allow 0xFF
                    env.error(scanner.pos, "value.large", "1 byte");
                    throw new Scanner.SyntaxError();
                }
                break;
            case 2:
//                if ((arg > 32767) || (arg < -32768)) { //this seems
// natural but is not backward compatible. Some tests contain
// expressions like:
//                sipush    0x8765;

                if ((arg > 65535) || (arg < -32768)) {
                    env.error(scanner.pos, "value.large", "2 bytes");
                    throw new Scanner.SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseInt("+size+")");
        }
        scanner.scan();
        return new Argument(arg);
    }

    /**
     * Parse an unsigned integer of size bytes long.
     *  size = 1 or 2
     */
    protected Argument parseUInt(int size) throws Scanner.SyntaxError, IOException {
        if (scanner.token != Token.INTVAL) {
            env.error(scanner.pos, "int.expected");
            throw new Scanner.SyntaxError();
        }
        if (scanner.sign == -1) {
            env.error(scanner.pos, "neg.forbidden");
            throw new Scanner.SyntaxError();
        }
        int arg = scanner.intValue;
        switch (size) {
            case 1:
                if (arg > 255) {
                    env.error(scanner.pos, "value.large", "1 byte");
                    throw new Scanner.SyntaxError();
                }
                break;
            case 2:
                if (arg > 65535) {
                    env.error(scanner.pos, "value.large", "2 bytes");
                    throw new Scanner.SyntaxError();
                }
                break;
            default:
                throw new InternalError("parseUInt("+size+")");
        }
        scanner.scan();
        return new Argument(arg);
    }

    /**
     * Parse constant declaration
     */
    protected void parseConstDef() throws IOException {
        for (;;) {
            if (scanner.token == Token.CPINDEX) {
                int cpx = scanner.intValue;
                scanner.scan();
                scanner.expect(Token.ASSIGN);
                env.traceln("parseConstDef:"+cpx);
                pool.setCell(cpx, cpParser.parseConstRef(null));
            } else {
                env.error("const.def.expected");
                throw new Scanner.SyntaxError();
            }
            if (scanner.token != Token.COMMA) {
                scanner.expect(Token.SEMICOLON);
                return;
            }
            scanner.scan(); // COMMA
        }
    }

    /**
     * Parse the modifiers
     */
    private int scanModifier(int mod) throws IOException {
        int nextmod, prevpos;

        while (true) {
            nextmod = 0;
            switch (scanner.token) {
                case PUBLIC:       nextmod = ACC_PUBLIC;       break;
                case PRIVATE:      nextmod = ACC_PRIVATE;      break;
                case PROTECTED:    nextmod = ACC_PROTECTED;    break;
                case STATIC:       nextmod = ACC_STATIC;       break;
                case FINAL:        nextmod = ACC_FINAL;        break;
                case SYNCHRONIZED: nextmod = ACC_SYNCHRONIZED; break;
                case SUPER:        nextmod = ACC_SUPER;        break;
                case VOLATILE:     nextmod = ACC_VOLATILE;     break;
                case BRIDGE:       nextmod = ACC_BRIDGE;       break;
                case TRANSIENT:    nextmod = ACC_TRANSIENT;    break;
                case VARARGS:      nextmod = ACC_VARARGS;      break;
                case NATIVE:       nextmod = ACC_NATIVE;       break;
                case INTERFACE:    nextmod = ACC_INTERFACE;    break;
                case ABSTRACT:     nextmod = ACC_ABSTRACT;     break;
                case STRICT:       nextmod = ACC_STRICT;       break;
                case ENUM:         nextmod = ACC_ENUM;         break;
                case SYNTHETIC:    nextmod = ACC_SYNTHETIC;    break;
 //               case SYNTHETIC:    nextmod = SYNTHETIC_ATTRIBUTE;    break;

                case DEPRECATED:   nextmod = DEPRECATED_ATTRIBUTE;   break;
                case MANDATED:       nextmod = ACC_MANDATED;       break;
                default:
//                    env.traceln(" is not a mod");
                    return nextmod;
            }
            prevpos = scanner.pos;
            scanner.scan();
            if ((mod & nextmod) == 0) {
                return nextmod;
            }
            env.error(prevpos, "warn.repeated.modifier");
        }
    }

    protected int scanModifiers() throws IOException {
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
    protected void parseField(int mod) throws Scanner.SyntaxError, IOException {
        debugStr("  [Parser.parseField]: <<<Begin>>>");
        // check access modifiers:
        Modifiers.checkFieldModifiers(cd, mod, scanner.pos);

        while (true) {
            ConstCell nameCell = parseName();
            scanner.expect(Token.COLON);
            ConstCell typeCell = parseName();

            // Define the variable
            FieldData fld = cd.addField(mod, nameCell, typeCell);

            if (memberAnnttns != null) {
                fld.addAnnotations(memberAnnttns);
            }

            // Parse the optional initializer
            if (scanner.token == Token.ASSIGN) {
                scanner.scan();
                fld.SetValue(cpParser.parseConstRef(null));
            }

            // If the next scanner.token is a comma, then there is more
            debugScan("  [Parser.parseField]: Field: " + fld + " ");

            if (scanner.token != Token.COMMA) {
                scanner.expect(Token.SEMICOLON);
                return;
            }
            scanner.scan();
        }  // end while
    }  // end parseField

    /**
     * Scan method's signature to determine size of parameters.
     */
    protected int countParams(int pos, ConstCell sigCell) throws Scanner.SyntaxError, IOException {
        String sig;
        try {
            ConstValue_String strConst = (ConstValue_String) sigCell.ref;
            sig = strConst.value;
        } catch (NullPointerException | ClassCastException e) {
            return 0; // ??? TBD
        }
        int siglen = sig.length(), k = 0, loccnt = 0, errparam = 0;
        boolean arraytype = false;
        scan: {
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
                        }
                        else {
                            loccnt++;
                        }
                        break;
                    case 'L':
                        for (;;k++) {
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
        env.error(scanner.pos, "msig.malformed", new Integer(k).toString(),
                new Integer(errparam).toString());
        return loccnt;
    }

    /**
     * Parse a method.
     */
    protected void parseMethod(int mod) throws Scanner.SyntaxError, IOException {

        // The start of the method
        int posa = scanner.pos;
        debugStr("  [Parser.parseMethod]: <<<Begin>>>");

        ConstCell nameCell = parseName();
        ConstValue_String strConst = (ConstValue_String) nameCell.ref;
        String name = strConst.value;
        boolean is_clinit = name.equals("<clinit>");
        boolean is_init = name.equals("<init>");
        DefaultAnnotationAttr defAnnot = null;

        // check access modifiers:
        Modifiers.checkMethodModifiers(cd, mod, posa, is_init, is_clinit);

        scanner.expect(Token.COLON);
        ConstCell typeCell = parseName();
        int paramcnt = countParams(scanner.pos, typeCell);
        if ((! Modifiers.isStatic(mod)) && ! is_clinit) {
            paramcnt++;
        }
        if (paramcnt > 255) {
            env.error(scanner.pos, "warn.msig.more255", new Integer(paramcnt).toString());
        }
        // Parse throws clause
        ArrayList<ConstCell> exc_table = null;
        if (scanner.token == Token.THROWS) {
            scanner.scan();
            exc_table = new  ArrayList<>();
            for (;;) {
                posa = scanner.pos;
                ConstCell exc = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
                if (exc_table.contains(exc)) {
                    env.error(posa, "warn.exc.repeated");
                } else {
                    exc_table.add(exc);
                    env.traceln("THROWS:"+exc.arg);
                }
                if (scanner.token != Token.COMMA) {
                    break;
                }
                scanner.scan();
            }
        }
        if (scanner.token == Token.DEFAULT) {
            // need to scan the annotation value
            defAnnot = annotParser.parseDefaultAnnotation();
        }

        curMethod = cd.StartMethod(mod, nameCell, typeCell, exc_table);
        Argument max_stack = null, max_locals = null;

        if (scanner.token == Token.STACK) {
            scanner.scan();
            max_stack = parseUInt(2);
        }
        if (scanner.token == Token.LOCAL) {
            scanner.scan();
            max_locals = parseUInt(2);
        }
        if (scanner.token == Token.INTVAL) {
            annotParser.parseParamAnnots(paramcnt, curMethod);
        }

        if (scanner.token == Token.SEMICOLON) {
            if ((max_stack != null) || (max_locals != null)) {
                env.error("token.expected", "{");
            }
            scanner.scan();
        }  else {
            scanner.expect(Token.LBRACE);
            curCode = curMethod.startCode(posa, paramcnt, max_stack, max_locals);
            while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
                instrParser.parseInstr();
                if (scanner.token == Token.RBRACE) {
                    break;
                }
                scanner.expect(Token.SEMICOLON);
            }

            curCode.endCode();
            scanner.expect(Token.RBRACE);
        }

        if (defAnnot != null) {
            curMethod.addDefaultAnnotation(defAnnot);
        }
        if (memberAnnttns != null) {
            curMethod.addAnnotations(memberAnnttns);
        }
        cd.EndMethod();
        debugStr("  [Parser.parseMethod]: Method: " + curMethod );

    }  // end parseMethod


    /**
     * Parse a (CPX based) BootstrapMethod entry.
     */
    protected void parseCPXBootstrapMethod(int mod) throws Scanner.SyntaxError, IOException {
        // Parses in the form:
        // BOOTSTRAPMETHOD CPX_MethodHandle (CPX_Arg)* ;
        if (scanner.token == Token.CPINDEX) {
            // CPX can be a CPX to an MethodHandle constant,
            int cpx = scanner.intValue;
            ConstCell MHCell = pool.getCell(cpx);
            scanner.scan();
            ArrayList<ConstCell> bsm_args = new ArrayList<>(256);

            while (scanner.token != Token.SEMICOLON) {
                if (scanner.token == Token.CPINDEX) {
                    bsm_args.add(pool.getCell(scanner.intValue));

                } else {
                    // throw error, bootstrap method is not recognizable
                    env.error(scanner.pos, "invalid.bootstrapmethod");
                    throw new Scanner.SyntaxError();
                }
                scanner.scan();
            }
            BootstrapMethodData bsmData = new BootstrapMethodData(MHCell, bsm_args);
            cd.addBootstrapMethod(bsmData);
        } else {
            // throw error, bootstrap method is not recognizable
            env.error(scanner.pos, "invalid.bootstrapmethod");
            throw new Scanner.SyntaxError();
        }
     }



    /**
     * Parse an inner class.
     */
    protected void parseInnerClass(int mod) throws Scanner.SyntaxError, IOException {
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
        debugScan("[Parser.parseInnerClass]:  Begin ");
        Modifiers.checkInnerClassModifiers(cd, mod, scanner.pos);

        ConstCell nameCell;
        ConstCell innerClass = null;
        ConstCell outerClass = null;


        if (scanner.token == Token.CLASS) {
            nameCell = pool.getCell(0);  // no NameIndex
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            if ((scanner.token == Token.IDENT) || scanner.checkTokenIdent()) {
                // Got a Class Name
                nameCell = parseName();
                parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == Token.CPINDEX) {
                // CPX can be either a CPX to an InnerClassName,
                // or a CPX to an InnerClassInfo
                int cpx = scanner.intValue;
                nameCell = pool.getCell(cpx);
                ConstValue nameCellValue = nameCell.ref;

                if (nameCellValue instanceof ConstValue_String) {
                    // got a name cell
                   scanner.scan();
                   parseInnerClass_s1(mod, nameCell, innerClass, outerClass);
                } else {
                    // got a CPRef cell
                    nameCell = pool.getCell(0);  // no NameIndex
                    parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
                }
            } else {
                pic_error();
            }

        }
    }

    private void parseInnerClass_s1(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // next scanner.token must be '='
        if (scanner.token == Token.ASSIGN) {
            scanner.scan();
            parseInnerClass_s2(mod, nameCell, innerClass, outerClass);
        } else {
            pic_error();
        }

    }


    private void parseInnerClass_s2(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) throws IOException {
        // scanner.token is either "CLASS IDENT" or "CPX_Class"
        if ((scanner.token == Token.CPINDEX) || (scanner.token == Token.CLASS)) {
            if (scanner.token == Token.CPINDEX) {
                int cpx = scanner.intValue;
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == Token.CLASS) {
                // next symbol needs to be InnerClass
                scanner.scan();
                innerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            // See if declaration is terminated
            if (scanner.token == Token.SEMICOLON) {
                // InnerClass is complete, no OUTERINFO;
                outerClass = pool.getCell(0);
                pic_tracecreate(mod, nameCell, innerClass, outerClass);
                cd.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else if (scanner.token == Token.OF) {
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
        if ((scanner.token == Token.CLASS) || (scanner.token == Token.CPINDEX)) {
            if (scanner.token == Token.CLASS) {
                // next symbol needs to be InnerClass
               scanner.scan();
               outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }
            if (scanner.token == Token.CPINDEX) {
                int cpx = scanner.intValue;
                outerClass = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }

            if (scanner.token == Token.SEMICOLON) {
               pic_tracecreate(mod, nameCell, innerClass, outerClass);
               cd.addInnerClass(mod, nameCell, innerClass, outerClass);
            } else {
                pic_error();
            }
        } else {
            pic_error();
        }
    }

    private void pic_tracecreate(int mod, ConstCell nameCell, ConstCell innerClass, ConstCell outerClass) {
            // throw error, IC is not recognizable
            env.trace(" Creating InnerClass: [" + Modifiers.toString(mod, CF_Context.CTX_INNERCLASS) + "], ");

            if (nameCell != pool.getCell(0)) {
                ConstValue value = nameCell.ref;
                if (value != null) {
                    env.trace(value.toString() + " = ");
                }
            }

            ConstValue_Cell ici_val = (ConstValue_Cell) innerClass.ref;
            ConstCell ici_ascii = (ConstCell) ici_val.cell;
            // Constant pool may not be numberized yet.
            //
            // check values before dereference on a trace.
            if (ici_ascii.ref == null) {
                env.trace("<#cpx-unresolved> ");
            } else {
                ConstValue_String cval = ( ConstValue_String) ici_ascii.ref;
                if (cval.value == null){
                    env.trace("<#cpx-0> ");
                } else {
                    env.trace(cval.value + " ");
                }
            }

            if (outerClass != pool.getCell(0)) {
                if (outerClass.arg != 0) {
                    ConstValue_Cell oci_val = (ConstValue_Cell) outerClass.ref;
                    ConstCell  oci_ascii = (ConstCell) oci_val.cell;
                    if (oci_ascii.ref == null) {
                        env.trace(" of <#cpx-unresolved>  ");
                    } else {
                        ConstValue_String cval = ( ConstValue_String) oci_ascii.ref;
                        if (cval.value == null) {
                            env.trace(" of <#cpx-0>  ");
                        } else {
                            env.trace(" of " + cval.value);
                        }
                    }
                }
            }

            env.traceln("");
    }

    private void pic_error() {
            // throw error, IC is not recognizable
            env.error(scanner.pos, "invalid.innerclass");
            throw new Scanner.SyntaxError();
    }

    /**
     * The match() method is used to quickly match opening
     * brackets (ie: '(', '{', or '[') with their closing
     * counter part. This is useful during error recovery.<p>
     *
     * Scan to a matching '}', ']' or ')'. The current scanner.token must be
     * a '{', '[' or '(';
     */
    protected void match(Token open, Token close) throws IOException {
        int depth = 1;

        while (true) {
            scanner.scan();
            if (scanner.token == open) {
                depth++;
            } else if (scanner.token == close) {
                if (--depth == 0) {
                    return;
                }
            } else if (scanner.token == Token.EOF) {
                env.error(scanner.pos, "unbalanced.paren");
                return;
            }
        }
    }

    /**
     * Recover after a syntax error in a field. This involves
     * discarding scanner.tokens until an EOF or a possible legal
     * continuation is encountered.
     */
    protected void recoverField() throws Scanner.SyntaxError, IOException {
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

                // possible begin of a field, continue
                return;

                case LBRACE:
                    match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                    break;

                case RBRACE:
                case INTERFACE:
                case CLASS:
                case IMPORT:
                case PACKAGE:
                    // begin of something outside a class, panic more
                    endClass(scanner.pos);
                    scanner.debugStr("    [Parser.recoverField]: pos: [" + scanner.pos + "]: ");
                    throw new Scanner.SyntaxError();

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
    protected void parseClass(int mod) throws IOException {
        int posa = scanner.pos;
        debugStr("   [Parser.parseClass]:  Begin ");
        // check access modifiers:
        Modifiers.checkClassModifiers(env, mod, posa);

        if (cd == null) {
            cd = new ClassData(env, major_version, minor_version);
            pool = cd.pool;
        }

        if (clsAnnttns != null) {
            cd.addAnnotations(clsAnnttns);
        }

        // move the tokenizer to the identifier:
        if (scanner.token == Token.CLASS) {
            scanner.scan();
        }

        // Parse the class name
        ConstCell nm = cpParser.parseConstRef(ConstType.CONSTANT_CLASS, null, true);

        if (scanner.token == Token.FIELD) { // DOT
            String fileExtension;
            scanner.scan();
            switch (scanner.token) {
                case STRINGVAL:
                    fileExtension = scanner.stringValue;
                    break;
                case IDENT:
                    fileExtension = scanner.idValue;
                    break;
                default:
                    env.error(scanner.pos, "name.expected");
                    throw new Scanner.SyntaxError();
            }
            scanner.scan();
            cd.fileExtension="."+fileExtension;
        } else if (scanner.token == Token.SEMICOLON) {
            // drop the semi-colon following a name
            scanner.scan();
        }

        // Parse extends clause
        ConstCell sup = null;
        if (scanner.token == Token.EXTENDS) {
            scanner.scan();
            sup = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            while (scanner.token == Token.COMMA) {
                scanner.scan();
                env.error(posa, "multiple.inherit");
                cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
            }
        }

        // Parse implements clause
        ArrayList<Argument> impl = new ArrayList<>();
        if (scanner.token == Token.IMPLEMENTS) {
            do {
                scanner.scan();
                Argument intf = cpParser.parseConstRef(ConstType.CONSTANT_CLASS);
                if (impl.contains(intf)) {
                    env.error(posa, "warn.intf.repeated", intf);
                } else {
                    impl.add(intf);
                }
            } while (scanner.token == Token.COMMA);
        }
        parseVersion();
        scanner.expect(Token.LBRACE);

        // Begin a new class
        cd.init(mod, nm, sup, impl);

       // Parse constant declarations

        // Parse class members
        while ((scanner.token != Token.EOF) && (scanner.token != Token.RBRACE)) {
            switch (scanner.token) {
                case SEMICOLON:
                    // Empty fields are allowed
                    scanner.scan();
                    break;

                case CONST:
                    scanner.scan();
                    parseConstDef();
                    explicitcp = true;
                    break;


                default:   // scanner.token is some member.
                    parseClassMembers();
            }  // end switch
        } // while

        scanner.expect(Token.RBRACE);

        // End the class
        endClass(scanner.prevPos);
    } // end parseClass

    private void parseClassMembers() throws IOException {
        debugScan("[Parser.parseClassMembers]:  Begin ");
        // Parse annotations
        if (scanner.token == Token.ANNOTATION) {
            memberAnnttns = annotParser.scanAnnotations();
        }

        // Parse modifiers
        int mod = scanModifiers();
        try {
            switch (scanner.token) {
                case FIELDREF:
                    scanner.scan();
                    parseField(mod);
                    break;
                case METHODREF:
                    scanner.scan();
                    parseMethod(mod);
                    break;
                case INNERCLASS:
                    scanner.scan();
                    parseInnerClass(mod);
                    break;
                case BOOTSTRAPMETHOD:
                    scanner.scan();
                    parseCPXBootstrapMethod(mod);
                    break;
                default:
                    env.error(scanner.pos, "field.expected");
                    throw new Scanner.SyntaxError();
            }  // end switch
        } catch (Scanner.SyntaxError e) {
            recoverField();
        }
        memberAnnttns = null;
    }

    /**
     * Recover after a syntax error in the file.
     * This involves discarding scanner.tokens until an EOF
     * or a possible legal continuation is encountered.
     */
    protected void recoverFile() throws IOException {
        while (true) {
            env.traceln("recoverFile: scanner.token=" + scanner.token);
            switch (scanner.token) {
                case CLASS:
                case INTERFACE:
                    // Start of a new source file statement, continue
                    return;

                case LBRACE:
                    match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    match(Token.LSQBRACKET, Token.RSQBRACKET);
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
    protected void endClass(int where) {
        if (explicitcp) {
            // Fix references in the constant pool (for explicitly coded CPs)
            pool.fixRefsInPool();
            // Fix any bootstrap Method references too
            cd.relinkBootstrapMethods();
        }

        cd.endClass();
        clsDataList.add(cd);
        cd = null;
    }

    public final ClassData[] getClassesData() {
        return ((ClassData[]) clsDataList.toArray(new ClassData[0]));
    }

    /**
     * Determines whether the JASM file is for a package-info class
     * or a module-info class.
     *
     * creates the correct kind of ClassData accordingly.
     *
     * @throws IOException
     */
    private void parseJasmPackages() throws IOException {
        try {
            // starting annotations could either be
            // a package annotation, or a class annotation
            if (scanner.token == Token.ANNOTATION) {
                if (cd == null) {
                    cd = new ClassData(env, major_version, minor_version);
                    pool = cd.pool;
                }
                pkgAnnttns = annotParser.scanAnnotations();
            }
            if (scanner.token == Token.PACKAGE) {
                // Package statement
                scanner.scan();
                int where = scanner.pos;
                String id = parseIdent();
                parseVersionPkg();
                scanner.expect(Token.SEMICOLON);

                if (pkg == null) {
                    pkg = id;
                    pkgPrefix = id + "/";
                } else {
                    env.error(where, "package.repeated");
                }
                debugScan("[Parser.parseJasmPackages] {PARSED} package-prefix: " + pkgPrefix + " ");
            }
        } catch (Scanner.SyntaxError e) {
            recoverFile();
        }
        // skip bogus semi colons
        while (scanner.token == Token.SEMICOLON) {
            scanner.scan();
        }

        // checks that we compile module or package compilation unit
        if (scanner.token == Token.EOF) {
            env.traceln("Scanner:  EOF");
            String sourceName = env.getSourceName();
            int mod = ACC_INTERFACE | ACC_ABSTRACT;

            // package-info
            if (sourceName.endsWith("package-info.jasm")) {
                env.traceln("Creating \"package-info.jasm\": package: " + pkg + " " + major_version + ":" + minor_version);

                if (cd == null) {
                    cd = new ClassData(env, major_version, minor_version);
                    pool = cd.pool;
                } else {
                    cd.major_version = major_version;
                    cd.minor_version = minor_version;
                }
                ConstCell me = pool.FindCellClassByName(pkgPrefix + "package-info");

                if (major_version > 49) {
                    mod |= SYNTHETIC_ATTRIBUTE;
                }
                cd.init(mod, me, new ConstCell(0), null);

                if (pkgAnnttns != null) {
                    cd.addAnnotations(pkgAnnttns);
                }

                endClass(scanner.prevPos);
            }
            return;
        }

        if (pkg == null && pkgAnnttns != null) { // RemoveModules
            clsAnnttns = pkgAnnttns;
            pkgAnnttns = null;
        }
    }

    /**
     * Parse an Jasm file.
     */
    public void parseFile() {
        try{
            // First, parse any package identifiers (and associated package annotations)
            parseJasmPackages();

            while (scanner.token != Token.EOF) {
                // Second, parse any class identifiers (and associated class annotations)
                try {
                    // Parse annotations
                    if (scanner.token == Token.ANNOTATION) {
                        if (cd == null) {
                            cd = new ClassData(env, major_version, minor_version);
                            pool = cd.pool;
                        }
                        clsAnnttns = annotParser.scanAnnotations();
                    }

                    // Parse class modifiers
                    int mod = scanModifiers();
                    if (mod == 0) {
                        switch (scanner.token) {
                        case CLASS:
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
                          debugScan(" [Parser.parseFile]: ");
                          env.error(scanner.pos, "toplevel.expected");
                          throw new Scanner.SyntaxError();
                        }
                    } else if (Modifiers.isInterface(mod) && (scanner.token != Token.CLASS)) {
                        // rare syntactic sugar:
                        // interface <ident> == abstract interface class <ident>
                        mod |= ACC_ABSTRACT;
                    }
                    parseClass(mod);
                    clsAnnttns = null;
                } catch (Scanner.SyntaxError e) {
                    // KTL
                    env.traceln("^^^^^^^ Syntax Error ^^^^^^^^^^^^");
                    if (scanner.debugFlag)
                        e.printStackTrace();
                    recoverFile();
                }
            }
        } catch (IOException e) {
            env.error(scanner.pos, "io.exception", env.getSourceName());
        } catch (Error er) {
            er.printStackTrace();
        }
    } //end parseFile
}  //end Parser
