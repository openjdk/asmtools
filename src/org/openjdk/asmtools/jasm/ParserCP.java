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

import org.openjdk.asmtools.common.SyntaxError;

import java.util.ArrayList;
import java.util.function.BiFunction;

import static java.lang.String.format;
import static org.openjdk.asmtools.jasm.ClassFileConst.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.*;
import static org.openjdk.asmtools.jasm.ConstantPool.*;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;

/**
 * ParserCP
 * <p>
 * ParseCP is a parser class owned by Parser.java. It is primarily responsible for parsing
 * the constant pool and constant declarations.
 */
public class ParserCP extends ParseBase {

    // Visitor object
    private final ParserCPVisitor pConstVstr;
    // Stop parsing a source file immediately and interpret any issue as an error
    private boolean exitImmediately = false;
    // counter of left braces
    private int lbrace = 0;

    /**
     * main constructor
     */
    protected ParserCP(Parser parentParser) {
        super.init(parentParser);
        pConstVstr = new ParserCPVisitor();
    }

    /**
     * In particular cases it's necessary to interpret a warning issue as an error and
     * stop parsing a source file immediately
     * cpParser.setExitImmediately(true);
     * parseConstRef(...);
     * cpParser.setExitImmediately(false);
     */
    public void setExitImmediately(boolean exitImmediately) {
        this.exitImmediately = exitImmediately;
    }

    /**
     * Parse CONSTVALUE
     */
    protected ConstValue<?> parseConstValue(ConstType tag) throws SyntaxError {
        return pConstVstr.visitExcept(tag);
    }

    /**
     * Parse [TAG] CONSTVALUE
     */
    protected ConstValue<?> parseTagConstValue(ConstType defaultTag) throws SyntaxError {
        return parseTagConstValue(defaultTag, null, false);
    }

    private ConstType scanConstByID(boolean ignoreKeywords) {
        ConstType tag = null;
        if (!ignoreKeywords) {
            tag = ClassFileConst.tag(scanner.idValue);
        }
        traceMethodInfoLn(format("\t\tTag: %s ", tag == null ? "<not found>" : tag));
        return tag;
    }

    private ConstType scanConstPrimVal() throws SyntaxError {
        ConstType tag;
        switch (scanner.token) {
            case BYTE -> tag = CONSTANT_INTEGER_BYTE;
            case CHAR -> tag = CONSTANT_INTEGER_CHAR;
            case DOUBLEVAL -> tag = ConstType.CONSTANT_DOUBLE;
            case FLOATVAL -> tag = ConstType.CONSTANT_FLOAT;
            case LONGVAL -> tag = ConstType.CONSTANT_LONG;
            case INTVAL -> tag = ConstType.CONSTANT_INTEGER;
            case SHORT -> tag = CONSTANT_INTEGER_SHORT;
            case BOOLEAN -> tag = CONSTANT_INTEGER_BOOLEAN;
            case STRINGVAL, BITS, IDENT -> tag = ConstType.CONSTANT_STRING;
            default -> {
                // problem - no constant value
                environment.error(scanner.pos, "err.value.expected", scanner.token.printValue());
                throw new SyntaxError();
            }
        }
        return tag;
    }

    private void checkWrongTag(ConstType tag, ConstType defaultTag, ConstType default2Tag) throws SyntaxError {
        if (defaultTag != null) {
            if (tag != defaultTag) {
                if (default2Tag == null) {
                    if (exitImmediately) {
                        environment.error(scanner.pos, "err.wrong.tag", defaultTag.parseKey());
                        throw new SyntaxError().setFatal();
                    }
                    environment.warning(scanner.pos, "warn.wrong.tag", defaultTag.parseKey());
                } else if (tag != default2Tag) {
                    if (exitImmediately) {
                        environment.error(scanner.pos, "err.wrong.tag2", defaultTag.parseKey(), default2Tag.parseKey());
                        throw new SyntaxError().setFatal();
                    }
                    environment.warning(scanner.pos, "warn.wrong.tag2", defaultTag.parseKey(), default2Tag.parseKey());
                }
            }
        }
    }

    protected ConstValue<?> parseTagConstValue(ConstType defaultTag, ConstType default2Tag, boolean ignoreKeywords)
            throws SyntaxError {
        traceMethodInfoLn(format("\t<< DefaultTag: %s 2nd DefaultTag: %s IgnoreKeyword?: %b",
                defaultTag == null ? "<none>" : defaultTag,
                default2Tag == null ? "<none>" : default2Tag, ignoreKeywords));
        // Lookup the Tag from the scanner
        ConstType tag = scanConstByID(ignoreKeywords);
        traceMethodInfoLn(format("\tResult Tag: %s >>", tag));
        // If the scanned tag is null
        if (tag == null) {
            // and, if the expected tag is null
            if (defaultTag == null) {
                // return some other type of constant as the tag
                tag = scanConstPrimVal();
            } else {
                // otherwise, make the scanned-tag the same constant-type
                // as the expected tag.
                tag = defaultTag;
            }
        } else {
            // If the scanned tag is some constant type
            // and the scanned type does not equal the expected type
            checkWrongTag(tag, defaultTag, default2Tag);
            scanner.scan();
        }
        return parseConstValue(tag);
    } // end parseTagConstValue

    protected ConstCell<?> parseConstRef(ConstType defaultTag) throws SyntaxError {
        return parseConstRef(defaultTag, null, false);
    }

    protected ConstCell<?> parseConstRef(ConstType defaultTag, ConstType default2Tag) throws SyntaxError {
        return parseConstRef(defaultTag, default2Tag, false);
    }

    /**
     * Parse an instruction argument, one of: * #NUMBER, #NAME, [TAG] CONSTVALUE
     */
    protected ConstCell<?> parseConstRef(ConstType defaultTag,
                                         ConstType default2Tag,
                                         boolean ignoreKeywords) throws SyntaxError {
        if (scanner.token == Token.CPINDEX) {
            int cpx = scanner.intValue;
            scanner.scan();
            return parser.pool.getCell(cpx);
        } else {
            ConstValue<?> ref = parseTagConstValue(defaultTag, default2Tag, ignoreKeywords);
            return parser.pool.findCell(ref);
        }
    } // end parseConstRef

    /**
     * ParserCPVisitor
     * <p>
     * This inner class overrides a constant pool visitor to provide specific parsing
     * instructions (per method) for each type of Constant.
     * <p>
     * Note: since the generic visitor throws no exceptions, this derived class tunnels
     * the exceptions, rethrown in the visitExcept method.
     */
    class ParserCPVisitor extends CPTagVisitor<ConstValue<?>> {

        private SyntaxError syntaxError;

        //This is the entry point for a visitor that tunnels exceptions
        public ConstValue<?> visitExcept(ConstType tag) throws SyntaxError {
            syntaxError = null;
            traceMethodInfoLn();
            ConstValue<?> ret = visit(tag);
            if (syntaxError != null) {
                throw syntaxError;
            }
            return ret;
        }

        @Override
        public ConstValue<?> visitUTF8() {
            traceMethodInfoLn();
            try {
                scanner.expect(Token.STRINGVAL);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return new ConstValue_UTF8(scanner.stringValue);
        }

        @Override
        public ConstValue<?> visitInteger(ClassFileConst.ConstType tag) {
            traceMethodInfoLn();
            int v = 0;
            try {
                if (scanner.token == Token.BITS) {
                    scanner.scan();
                    scanner.inBits = true;
                }
                v = scanner.intValue * scanner.sign;
                scanner.expect(Token.INTVAL);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return new ConstValue_Integer(tag, v);
        }

        @Override
        public ConstValue<?> visitLong() {
            traceMethodInfoLn();
            ConstValue_Long valueLong = null;
            try {
                long v;
                if (scanner.token == Token.BITS) {
                    scanner.scan();
                    scanner.inBits = true;
                }
                switch (scanner.token) {
                    case INTVAL -> v = scanner.intValue;
                    case LONGVAL -> v = scanner.longValue;
                    default -> {
                        environment.error(scanner.prevPos, "err.token.expected", "Integer");
                        throw new SyntaxError();
                    }
                }
                valueLong = new ConstValue_Long(v * scanner.sign);
                scanner.scan();
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return valueLong;
        }

        @Override
        public ConstValue<?> visitFloat() {
            traceMethodInfoLn();
            ConstValue_Float valueFloat = null;
            try {
                int v;
                float f;
                scanner.inBits = false;  // this needs to be initialized for each float!
                if (scanner.token == Token.BITS) {
                    scanner.scan();
                    scanner.inBits = true;
                }
                i2f:
                {
                    switch (scanner.token) {
                        case INTVAL -> {
                            if (scanner.inBits) {
                                v = scanner.intValue;
                                break i2f;
                            } else {
                                f = (float) scanner.intValue;
                            }
                        }
                        case FLOATVAL -> f = scanner.floatValue;
                        case DOUBLEVAL -> f = (float) scanner.doubleValue; // to be excluded?
                        case INF -> f = Float.POSITIVE_INFINITY;
                        case NAN -> f = Float.NaN;
                        default -> {
                            environment.traceln("token=" + scanner.token);
                            environment.error(scanner.pos, "err.token.expected", "<Float>");
                            throw new SyntaxError();
                        }
                    }
                    v = Float.floatToIntBits(f);
                }
                if (scanner.sign == -1) {
                    v = v ^ 0x80000000;
                }
                valueFloat = new ConstValue_Float(v);
                scanner.scan();
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return valueFloat;
        }

        @Override
        public ConstValue<?> visitDouble() {
            traceMethodInfoLn();
            ConstValue_Double valueDouble = null;
            try {
                long v;
                double d;
                if (scanner.token == Token.BITS) {
                    scanner.scan();
                    scanner.inBits = true;
                }
                d2l:
                {
                    switch (scanner.token) {
                        case INTVAL -> {
                            if (scanner.inBits) {
                                v = scanner.intValue;
                                break d2l;
                            } else {
                                d = scanner.intValue;
                            }
                        }
                        case LONGVAL -> {
                            if (scanner.inBits) {
                                v = scanner.longValue;
                                break d2l;
                            } else {
                                d = (double) scanner.longValue;
                            }
                        }
                        case FLOATVAL -> d = scanner.floatValue;
                        case DOUBLEVAL -> d = scanner.doubleValue;
                        case INF -> d = Double.POSITIVE_INFINITY;
                        case NAN -> d = Double.NaN;
                        default -> {
                            environment.error(scanner.pos, "err.token.expected", "Double");
                            throw new SyntaxError();
                        }
                    }
                    v = Double.doubleToLongBits(d);
                }
                if (scanner.sign == -1) {
                    v = v ^ 0x8000000000000000L;
                }
                valueDouble = new ConstValue_Double(v);
                scanner.scan();
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return valueDouble;
        }

        private ConstCell<?> visitName() {
            traceMethodInfoLn();
            ConstCell<?> obj = null;
            try {
                // Parse an external name: CPINDEX, string, or identifier.
                obj = parser.parseName();
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }

        @Override
        public ConstValue<?> visitMethodType() {
            traceMethodInfoLn();
            ConstValue_MethodType obj = null;
            ConstCell<ConstValue_UTF8> cell = (ConstCell<ConstValue_UTF8>) visitName();
            if (syntaxError == null) {
                obj = new ConstValue_MethodType(cell);
            }
            return obj;
        }

        @Override
        public ConstValue<?> visitString() {
            traceMethodInfoLn();
            ConstValue_String obj = null;
            ConstCell cell = visitName();
            if (syntaxError == null) {
                obj = new ConstValue_String(cell);
            }
            return obj;
        }

        @Override
        public ConstValue<?> visitClass() {
            traceMethodInfoLn();
            ConstValue_Class obj = null;
            try {
                ConstCell cell = parser.parseConstantClassInfo(true);
                obj = new ConstValue_Class(cell);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }

        @Override
        public ConstValue<?> visitPackage() {
            traceMethodInfoLn();
            ConstValue_Package obj = null;
            try {
                ConstCell cell = parser.parseConstantPackageInfo();
                obj = new ConstValue_Package(cell);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }

        @Override
        public ConstValue<?> visitModule() {
            traceMethodInfoLn();
            ConstValue_Module obj = null;
            try {
                ConstCell cell = parser.parseConstantModuleInfo();
                obj = new ConstValue_Module(cell);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }


        @Override
        public ConstValue<?> visitMethodHandle() {
            traceMethodInfoLn();
            ConstValue_MethodHandle obj = null;
            try {
                ConstCell refCell;
                SubTag subTag;
                // MethodHandle    [INVOKESUBTAG|INVOKESUBTAG_INDEX] :    CONSTANT_FIELD | [FIELDREF|METHODREF|INTERFACEMETHODREF]
                if (scanner.token == Token.INTVAL) {
                    // INVOKESUBTAG_INDEX
                    // Handle explicit constant pool form
                    subTag = subTag(scanner.intValue);
                    scanner.scan();
                    scanner.expect(Token.COLON);
                    if (scanner.token == Token.CPINDEX) {
                        // CONSTANT_FIELD
                        int cpx = scanner.intValue;
                        refCell = parser.pool.getCell(cpx);
                        scanner.scan();
                    } else {
                        // [FIELDREF|METHODREF|INTERFACEMETHODREF]
                        refCell = parser.parseMethodHandle(subTag);
                    }
                } else {
                    // INVOKESUBTAG : REF_INVOKEINTERFACE, REF_NEWINVOKESPECIAL, ...
                    // normal JASM
                    subTag = parser.parseSubtag();
//                    subtagCell = new ConstCell(subtag.value());
                    scanner.expect(Token.COLON);
                    if (scanner.token == Token.CPINDEX) {
                        // CODETOOLS-7901522: Jasm doesn't allow creating REF_invoke* referring an InterfaceMethod
                        // Parsing the case when refCell is CP index (#1)
                        // const #1 = InterfaceMethod m:"()V";
                        // const #2 = MethodHandle REF_invokeSpecial:#1;
                        int cpx = scanner.intValue;
                        refCell = parser.pool.getCell(cpx);
                        scanner.scan();
                    } else {
                        refCell = parser.parseMethodHandle(subTag);
                    }
                }
                obj = new ConstValue_MethodHandle(subTag, refCell);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }

        private <T extends ConstValue_Pair<ConstValue_Class, ConstValue_NameAndType>> T visitMember(ConstType tag) {
            traceMethodInfoLn();
            T constValue = null;
            try {
                Token prevToken = scanner.token;
                ConstCell firstName;
                ConstCell<ConstValue_Class> ClassCell;
                ConstCell<ConstValue_NameAndType> NameCell, NapeCell;
                firstName = parser.parseConstantClassInfo(false);
                if (scanner.token == Token.FIELD) { // DOT
                    scanner.scan();
                    if (prevToken == Token.CPINDEX) {
                        ClassCell = firstName;
                    } else {
                        ClassCell = parser.pool.findCell(ConstType.CONSTANT_CLASS, firstName);
                    }
                    NameCell = parser.parseName();
                } else {
                    // no class provided - assume current class
                    if (parser.classData.coreClasses.this_class().isSet() ||
                            parser.classData.coreClasses.this_class().ref == null) {
                        ClassCell = (ConstCell<ConstValue_Class>) parser.classData.coreClasses.this_class();
                    } else {
                        ClassCell = parser.pool.findCell((ConstValue_Class) parser.classData.coreClasses.this_class().ref);
                    }
                    NameCell = firstName;
                }
                if (scanner.token == Token.COLON) {
                    // name and type separately
                    scanner.scan();
                    NapeCell = parser.pool.findCell(ConstType.CONSTANT_NAMEANDTYPE, NameCell, parser.parseName());
                } else {
                    // name and type as single name
                    NapeCell = NameCell;
                }
                switch (tag) {
                    case CONSTANT_INTERFACEMETHODREF -> constValue = (T) new ConstValue_InterfaceMethodRef(ClassCell, NapeCell);
                    case CONSTANT_METHODREF -> constValue = (T) new ConstValue_MethodRef(ClassCell, NapeCell);
                    case CONSTANT_FIELDREF -> constValue = (T) new ConstValue_FieldRef(ClassCell, NapeCell);
                }
                if (constValue == null) {
                    environment.error("err.invalid.type", tag.printVal());
                    throw new SyntaxError().setFatal();
                }
            } catch (SyntaxError se) {
                syntaxError = se;
            }

            return constValue;
        }

        @Override
        public ConstValue<?> visitField() {
            traceMethodInfoLn();
            return visitMember(ConstType.CONSTANT_FIELDREF);
        }

        @Override
        public ConstValue<?> visitMethod() {
            traceMethodInfoLn();
            return visitMember(ConstType.CONSTANT_METHODREF);
        }

        @Override
        public ConstValue<?> visitInterfaceMethod() {
            traceMethodInfoLn();
            return visitMember(ConstType.CONSTANT_INTERFACEMETHODREF);
        }

        @Override
        public ConstValue<?> visitNameAndType() {
            traceMethodInfoLn();
            ConstValue_NameAndType obj = null;
            try {
                ConstCell<?> NameCell = parser.parseName(), TypeCell;
                scanner.expect(Token.COLON);
                TypeCell = parser.parseName();
                obj = new ConstValue_NameAndType((ConstCell<ConstValue_UTF8>) NameCell, (ConstCell<ConstValue_UTF8>) TypeCell);
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }

        @Override
        public ConstValue_InvokeDynamic visitInvokeDynamic() {
            traceMethodInfoLn();
            final BiFunction<BootstrapMethodData, ConstCell<?>, ConstValue_InvokeDynamic> ctor =
                    ConstValue_InvokeDynamic::new;
            return visitBsm(ctor);
        }

        @Override
        public ConstValue_Dynamic visitDynamic() {
            traceMethodInfoLn();
            final BiFunction<BootstrapMethodData, ConstCell<?>, ConstValue_Dynamic> ctor =
                    ConstValue_Dynamic::new;
            return visitBsm(ctor);
        }

        private <E extends ConstValue_BootstrapMethod> E visitBsm(BiFunction<BootstrapMethodData, ConstCell<?>, E> ctor) {
            E obj = null;
            try {
                if (scanner.token == Token.INTVAL) {
                    // Handle explicit constant pool form
                    int bsmIndex = scanner.intValue;
                    scanner.scan();

                    scanner.expect(Token.COLON);

                    if (scanner.token != Token.CPINDEX) {
                        environment.traceln("token=" + scanner.token);
                        environment.error(scanner.pos, "err.token.expected", "<CPINDEX>");
                        throw new SyntaxError();
                    }
                    int cpx = scanner.intValue;
                    scanner.scan();
                    // Put a placeholder in place of BSM.
                    // resolve placeholder after the attributes are scanned.
                    BootstrapMethodData bsmData = new BootstrapMethodData(bsmIndex);
                    obj = ctor.apply(bsmData, parser.pool.getCell(cpx));
                } else {
                    // Handle full form
                    ConstCell<?> MHCell = parser.pool.findCell(parseConstValue(ConstType.CONSTANT_METHODHANDLE));
                    scanner.expect(Token.COLON);
                    ConstCell<?> NapeCell = parser.pool.findCell(parseConstValue(ConstType.CONSTANT_NAMEANDTYPE));
                    if (scanner.token == Token.LBRACE) {
                        ParserCP.this.lbrace++;
                        scanner.scan();
                    }
                    ArrayList<ConstCell<?>> bsm_args = new ArrayList<>(256);
                    while (true) {
                        if (ParserCP.this.lbrace > 0) {
                            if (scanner.token == Token.RBRACE) {
                                ParserCP.this.lbrace--;
                                scanner.scan();
                                break;
                            } else if (scanner.token == Token.SEMICOLON) {
                                scanner.expect(Token.RBRACE);
                            }
                        } else if (scanner.token == Token.SEMICOLON) {
                            break;
                        }
                        if (scanner.token == Token.COMMA) {
                            scanner.scan();
                        }
                        bsm_args.add(parseConstRef(null));
                    }
                    if (ParserCP.this.lbrace == 0) {
                        scanner.check(Token.SEMICOLON);
                    }
                    BootstrapMethodData bsmData = new BootstrapMethodData(MHCell, bsm_args);
                    parser.classData.addBootstrapMethod(bsmData);
                    obj = ctor.apply(bsmData, NapeCell);
                }
            } catch (SyntaxError se) {
                syntaxError = se;
            }
            return obj;
        }
    } // End Visitor
}
