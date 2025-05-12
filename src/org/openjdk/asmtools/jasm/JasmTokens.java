/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JasmTokens
 * <p>
 * This class contains tokens specific to parsing JASM syntax.
 * <p>
 * The classes in JasmTokens are following a Singleton Pattern. These classes are Enums,
 * and they are contained in private hash maps (lookup tables and reverse lookup tables).
 * These hash maps all have public accessors, which clients use to look-up enums.
 * <p>
 * Tokens in this table carry no external state, and are typically treated as constants.
 * They do not need to be reset.
 */
public class JasmTokens {

    /*-------------------------------------------------------- */
    /* Marker: describes the type of Keyword */
    public enum KeywordType {
        TOKEN("TOKEN"),
        VALUE("VALUE"),
        JASMIDENTIFIER("JASM"),
        KEYWORD("KEYWORD");

        private final String printValue;

        KeywordType(String printValue) {
            this.printValue = printValue;
        }

        public String printValue() {
            return printValue;
        }
    }

    /*--------------------------------------------------------  */
    /* Marker - describes the type of token                     */
    /*    this is rather cosmetic, no function currently.       */
    public enum TokenType {
        MODIFIER("Modifier"),
        OPERATOR("Operator"),
        VALUE("Value"),
        TYPE("Type"),
        EXPRESSION("Expression"),
        STATEMENT("Statement"),
        DECLARATION("Declaration"),
        PUNCTUATION("Punctuation"),
        SPECIAL("Special"),
        JASM("Jasm"),
        MISC("Misc"),
        JASM_IDENT("Jasm identifier"),
        MODULE_NAME("Module Name"),             // The token can be used as Module Name
        TYPE_PATH_KIND("Type path kind"),       // Table 4.7.20.2-A Interpretation of type_path_kind values
        CLASS_NAME("Class Name");              // The token can be used as Class Name

        private final String printValue;

        TokenType(String printValue) {
            this.printValue = printValue;
        }

        public String printValue() {
            return printValue;
        }
    }

    public enum AnnotationType {
        Visible("@+"),
        Invisible("@-"),
        VisibleType("@T+"),
        InvisibleType("@T-");

        private final String jasmPrefix;

        AnnotationType(String jasmPrefix) {
            this.jasmPrefix = jasmPrefix;
        }

        /**
         * isAnnotationToken
         * <p>
         * examines the beginning of a string to see if it starts with an annotation
         * characters ('@+' = visible annotation, '@-' = invisible).
         *
         * @param str String to be analyzed
         * @return True if the string starts with an annotation char.
         */
        static public boolean isAnnotationToken(String str) {
            return (str.startsWith(AnnotationType.Invisible.jasmPrefix) ||
                    str.startsWith(AnnotationType.Visible.jasmPrefix));
        }

        /**
         * isTypeAnnotationToken
         * <p>
         * examines the beginning of a string to see if it starts with type annotation
         * characters ('@T+' = visible type annotation, '@T-' = invisible).
         *
         * @param str String to be analyzed
         * @return True if the string starts with an annotation char.
         */
        static public boolean isTypeAnnotationToken(String str) {
            return (str.startsWith(AnnotationType.InvisibleType.jasmPrefix) ||
                    str.startsWith(AnnotationType.VisibleType.jasmPrefix));
        }

        /**
         * isAnnotation
         * <p>
         * examines the beginning of a string to see if it starts with an annotation character
         *
         * @param str String to be analyzed
         * @return True if the string starts with an annotation char.
         */
        static public boolean isAnnotation(String str) {
            return (str.startsWith("@"));
        }

        /**
         * isInvisibleAnnotationToken
         * <p>
         * examines the end of an annotation token to determine visibility ('+' = visible
         * annotation, '-' = invisible).
         *
         * @param str String to be analyzed
         * @return True if the token implies invisible annotation.
         */
        static public boolean isInvisibleAnnotationToken(String str) {
            return (str.endsWith("-"));
        }
    }

    /**
     * Scanner Tokens (Definitive List)
     */
    public enum Token {
        EOF(-1, "EOF", "EOF", EnumSet.of(TokenType.MISC, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        COMMA(0, "COMMA", ",", EnumSet.of(TokenType.OPERATOR)),
        ASSIGN(1, "ASSIGN", "=", EnumSet.of(TokenType.OPERATOR)),

        ASGMUL(2, "ASGMUL", "*=", EnumSet.of(TokenType.OPERATOR)),
        ASGDIV(3, "ASGDIV", "/=", EnumSet.of(TokenType.OPERATOR)),
        ASGREM(4, "ASGREM", "%=", EnumSet.of(TokenType.OPERATOR)),
        ASGADD(5, "ASGADD", "+=", EnumSet.of(TokenType.OPERATOR)),
        ASGSUB(6, "ASGSUB", "-=", EnumSet.of(TokenType.OPERATOR)),
        ASGLSHIFT(7, "ASGLSHIFT", "<<=", EnumSet.of(TokenType.OPERATOR)),
        ASGRSHIFT(8, "ASGRSHIFT", ">>=", EnumSet.of(TokenType.OPERATOR)),
        ASGURSHIFT(9, "ASGURSHIFT", "<<<=", EnumSet.of(TokenType.OPERATOR)),
        ASGBITAND(10, "ASGBITAND", "&=", EnumSet.of(TokenType.OPERATOR)),
        ASGBITOR(11, "ASGBITOR", "|=", EnumSet.of(TokenType.OPERATOR)),
        ASGBITXOR(12, "ASGBITXOR", "^=", EnumSet.of(TokenType.OPERATOR)),

        COND(13, "COND", "?:", EnumSet.of(TokenType.OPERATOR)),
        OR(14, "OR", "||", EnumSet.of(TokenType.OPERATOR)),
        AND(15, "AND", "&&", EnumSet.of(TokenType.OPERATOR)),
        BITOR(16, "BITOR", "|", EnumSet.of(TokenType.OPERATOR)),
        BITXOR(17, "BITXOR", "^", EnumSet.of(TokenType.OPERATOR)),
        BITAND(18, "BITAND", "&", EnumSet.of(TokenType.OPERATOR)),
        NE(19, "NE", "!=", EnumSet.of(TokenType.OPERATOR)),
        EQ(20, "EQ", "==", EnumSet.of(TokenType.OPERATOR)),
        GE(21, "GE", ">=", EnumSet.of(TokenType.OPERATOR)),
        GT(22, "GT", ">", EnumSet.of(TokenType.OPERATOR)),
        LE(23, "LE", "<=", EnumSet.of(TokenType.OPERATOR)),
        LT(24, "LT", "<", EnumSet.of(TokenType.OPERATOR)),
        INSTANCEOF(25, "INSTANCEOF", "instanceof", EnumSet.of(TokenType.OPERATOR)),
        LSHIFT(26, "LSHIFT", "<<", EnumSet.of(TokenType.OPERATOR)),
        RSHIFT(27, "RSHIFT", ">>", EnumSet.of(TokenType.OPERATOR)),
        URSHIFT(28, "URSHIFT", "<<<", EnumSet.of(TokenType.OPERATOR)),
        ADD(29, "ADD", "+", EnumSet.of(TokenType.OPERATOR)),
        SUB(30, "SUB", "-", EnumSet.of(TokenType.OPERATOR)),
        DIV(31, "DIV", "/", EnumSet.of(TokenType.OPERATOR)),
        REM(32, "REM", "%", EnumSet.of(TokenType.OPERATOR)),
        MUL(33, "MUL", "*", EnumSet.of(TokenType.OPERATOR)),
        CAST(34, "CAST", "cast", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        POS(35, "POS", "+", EnumSet.of(TokenType.OPERATOR)),
        NEG(36, "NEG", "-", EnumSet.of(TokenType.OPERATOR)),
        NOT(37, "NOT", "!", EnumSet.of(TokenType.OPERATOR)),
        BITNOT(38, "BITNOT", "~", EnumSet.of(TokenType.OPERATOR)),
        PREINC(39, "PREINC", "++", EnumSet.of(TokenType.OPERATOR)),
        PREDEC(40, "PREDEC", "--", EnumSet.of(TokenType.OPERATOR)),
        NEWARRAY(41, "NEWARRAY", "new", EnumSet.of(TokenType.OPERATOR)),
        NEWINSTANCE(42, "NEWINSTANCE", "new", EnumSet.of(TokenType.OPERATOR)),
        NEWFROMNAME(43, "NEWFROMNAME", "new", EnumSet.of(TokenType.OPERATOR)),
        POSTINC(44, "POSTINC", "++", EnumSet.of(TokenType.OPERATOR)),
        POSTDEC(45, "POSTDEC", "--", EnumSet.of(TokenType.OPERATOR)),
        FIELD(46, "FIELD", "field", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        METHOD(47, "METHOD", "method", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        ARRAYACCESS(48, "ARRAYACCESS", "[]", EnumSet.of(TokenType.OPERATOR)),
        NEW(49, "NEW", "new", EnumSet.of(TokenType.OPERATOR)),
        INC(50, "INC", "++", EnumSet.of(TokenType.OPERATOR)),
        DEC(51, "DEC", "--", EnumSet.of(TokenType.OPERATOR)),
        // 52, 53 are reserved for FILE, CLASSFILE

        CONVERT(55, "CONVERT", "convert", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        EXPR(56, "EXPR", "expr", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        ARRAY(57, "ARRAY", "array", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        GOTO(58, "GOTO", "goto", EnumSet.of(TokenType.OPERATOR)),
        /*
         * Value tokens
         */
        IDENT(60, "IDENT", "Identifier", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME, TokenType.JASM_IDENT), KeywordType.VALUE),
        BOOLEANVAL(61, "BOOLEANVAL", "Boolean", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        BYTEVAL(62, "BYTEVAL", "Byte", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CHARVAL(63, "CHARVAL", "Char", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        SHORTVAL(64, "SHORTVAL", "Short", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        INTVAL(65, "INTVAL", "Integer", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        LONGVAL(66, "LONGVAL", "Long", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        FLOATVAL(67, "FLOATVAL", "Float", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        DOUBLEVAL(68, "DOUBLEVAL", "Double", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        STRINGVAL(69, "STRINGVAL", "String", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.VALUE),
        /*
         * Type keywords
         */
        BYTE(70, "BYTE", "byte", EnumSet.of(TokenType.TYPE)),
        CHAR(71, "CHAR", "char", EnumSet.of(TokenType.TYPE)),
        SHORT(72, "SHORT", "short", EnumSet.of(TokenType.TYPE)),
        INT(73, "INT", "int", EnumSet.of(TokenType.TYPE)),
        LONG(74, "LONG", "long", EnumSet.of(TokenType.TYPE)),
        FLOAT(75, "FLOAT", "float", EnumSet.of(TokenType.TYPE)),
        DOUBLE(76, "DOUBLE", "double", EnumSet.of(TokenType.TYPE)),
        VOID(77, "VOID", "void", EnumSet.of(TokenType.TYPE)),
        BOOLEAN(78, "BOOLEAN", "boolean", EnumSet.of(TokenType.TYPE)),
        /*
         * Expression keywords
         */
        TRUE(80, "TRUE", "true", EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        FALSE(81, "FALSE", "false", EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        THIS(82, "THIS", "this", EnumSet.of(TokenType.EXPRESSION)),
        SUPER(83, "SUPER", "super", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        NULL(84, "NULL", "null", EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),

        THIS_CLASS(85, "this_class", "this_class", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SUPER_CLASS(86, "super_class", "super_class", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        /*
         * Statement keywords
         */
        IF(90, "IF", "if", EnumSet.of(TokenType.STATEMENT)),
        ELSE(91, "ELSE", "else", EnumSet.of(TokenType.STATEMENT)),
        FOR(92, "FOR", "for", EnumSet.of(TokenType.STATEMENT)),
        WHILE(93, "WHILE", "while", EnumSet.of(TokenType.STATEMENT)),
        DO(94, "DO", "do", EnumSet.of(TokenType.STATEMENT)),
        SWITCH(95, "SWITCH", "switch", EnumSet.of(TokenType.STATEMENT)),
        CASE(96, "CASE", "case", EnumSet.of(TokenType.STATEMENT)),
        DEFAULT(97, "DEFAULT", "default", EnumSet.of(TokenType.STATEMENT), KeywordType.KEYWORD),
        BREAK(98, "BREAK", "break", EnumSet.of(TokenType.STATEMENT)),
        CONTINUE(99, "CONTINUE", "continue", EnumSet.of(TokenType.STATEMENT)),
        RETURN(100, "RETURN", "return", EnumSet.of(TokenType.STATEMENT)),
        TRY(101, "TRY", "try", EnumSet.of(TokenType.STATEMENT)),

        CATCH(102, "CATCH", "catch", EnumSet.of(TokenType.STATEMENT)),
        FINALLY(103, "FINALLY", "finally", EnumSet.of(TokenType.STATEMENT)),
        THROW(104, "THROW", "throw", EnumSet.of(TokenType.STATEMENT)),
        STAT(105, "STAT", "stat", EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        EXPRESSION(106, "EXPRESSION", "expression", EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        DECLARATION(107, "DECLARATION", "declaration", EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        VARDECLARATION(108, "VARDECLARATION", "vdeclaration", EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        /*
         * Declaration keywords
         */
        IMPORT(110, "IMPORT", "import", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CLASS(111, "CLASS", "class", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        FILE(52, "FILE", "file", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        CLASS_FILE(53, "CLASSFILE", "classfile", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        EXTENDS(112, "EXTENDS", "extends", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        IMPLEMENTS(113, "IMPLEMENTS", "implements", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        INTERFACE(114, "INTERFACE", "interface", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        PACKAGE(115, "PACKAGE", "package", EnumSet.of(TokenType.DECLARATION), KeywordType.KEYWORD),
        ENUM(116, "ENUM", "enum", EnumSet.of(TokenType.DECLARATION), KeywordType.KEYWORD),
        MANDATED(117, "MANDATED", "mandated", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        THROWS(118, "THROWS", "throws", EnumSet.of(TokenType.DECLARATION), KeywordType.KEYWORD),

        /*
         * Modifier keywords
         */
        ANNOTATION_ACCESS(119, "ANNOTATION_ACCESS", "annotation", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        PRIVATE(120, "PRIVATE", "private", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        PUBLIC(121, "PUBLIC", "public", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        PROTECTED(122, "PROTECTED", "protected", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        CONST(123, "CONST", "const", EnumSet.of(TokenType.DECLARATION), KeywordType.KEYWORD),
        STATIC(124, "STATIC", "static", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        TRANSIENT(125, "TRANSIENT", "transient", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SYNCHRONIZED(126, "SYNCHRONIZED", "synchronized", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        NATIVE(127, "NATIVE", "native", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        FINAL(128, "FINAL", "final", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        VOLATILE(129, "VOLATILE", "volatile", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        ABSTRACT(130, "ABSTRACT", "abstract", EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        TRANSITIVE(131, "TRANSITIVE", "transitive", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        OPEN(132, "OPEN", "open", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME), KeywordType.KEYWORD),

        /*
         * Punctuation
         */
        AT_SIGN(133, "AT", "@", EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        SEMICOLON(134, "SEMICOLON", ";", EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        COLON(135, "COLON", ":", EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        QUESTIONMARK(136, "QUESTIONMARK", "?", EnumSet.of(TokenType.PUNCTUATION)),
        LBRACE(137, "LBRACE", "{", EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        RBRACE(138, "RBRACE", "}", EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        LPAREN(139, "LPAREN", "(", EnumSet.of(TokenType.PUNCTUATION)),
        RPAREN(140, "RPAREN", ")", EnumSet.of(TokenType.PUNCTUATION)),
        LSQBRACKET(141, "LSQBRACKET", "[", EnumSet.of(TokenType.PUNCTUATION)),
        RSQBRACKET(142, "RSQBRACKET", "]", EnumSet.of(TokenType.PUNCTUATION)),

        ESCAPED_COLON(201, "ESCCOLON", "\\:", EnumSet.of(TokenType.PUNCTUATION)),
        ESCAPED_ATSIGH(202, "ESCATSIGH", "\\@", EnumSet.of(TokenType.PUNCTUATION)),
        ESCAPED_BACKSLASH(203, "ESCBACKSLASH", "\\\\", EnumSet.of(TokenType.PUNCTUATION)),
        /*
         * Special tokens
         */
        ERROR(145, "ERROR", "error", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        COMMENT(146, "COMMENT", "comment", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        TYPE(147, "TYPE", "type", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        LENGTH(148, "LENGTH", "Length", "length", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        INLINERETURN(149, "INLINERETURN", "inline-return", EnumSet.of(TokenType.MODIFIER)),
        INLINEMETHOD(150, "INLINEMETHOD", "inline-method", EnumSet.of(TokenType.MODIFIER)),
        INLINENEWINSTANCE(151, "INLINENEWINSTANCE", "inline-new", EnumSet.of(TokenType.MODIFIER)),

        /*
         * Added for jasm
         */
        METHODREF(152, "METHODREF", "Method", "Methodref", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        FIELDREF(153, "FIELD", "Field", "Fieldref", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        STACK(154, "STACK", "stack", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LOCAL(155, "LOCAL", "locals", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        CPINDEX(156, "CPINDEX", "CPINDEX", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CPNAME(157, "CPNAME", "CPName", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        SIGN(158, "SIGN", "SIGN", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        BITS(159, "BITS", "bits", EnumSet.of(TokenType.MISC, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        INF(160, "INF", "Inf", "Infinity", EnumSet.of(TokenType.MISC, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NAN(161, "NAN", "NaN", EnumSet.of(TokenType.MISC, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        INNERCLASS(162, "INNERCLASS", "InnerClass", "InnerClasses", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        OF(163, "OF", "of", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SYNTHETIC(164, "SYNTHETIC", "synthetic", EnumSet.of(TokenType.MODIFIER, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        STRICT(165, "STRICT", "strict", EnumSet.of(TokenType.MODIFIER, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        DEPRECATED(166, "DEPRECATED", "deprecated", EnumSet.of(TokenType.MODIFIER, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        VERSION(167, "VERSION", "version", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        MODULE(168, "MODULE", "module", EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        ANNOTATION(169, "ANNOTATION", "@", EnumSet.of(TokenType.MISC)),
        PARAM_NAME(170, "PARAM_NAME", "#", EnumSet.of(TokenType.MISC)),

        VARARGS(171, "VARARGS", "varargs", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        BRIDGE(172, "BRIDGE", "bridge", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        // Declaration keywords
        BOOTSTRAPMETHOD(173, "BOOTSTRAPMETHOD", "BootstrapMethod", "BootstrapMethods", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NESTHOST(174, "NESTHOST", "NestHost", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SIGNATURE(175, "SIGNATURE", "Signature", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NESTMEMBERS(176, "NESTMEMBERS", "NestMembers", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //
        RECORD(177, "RECORD", "Record", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        COMPONENT(178, "COMPONENT", "Component", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //
        PERMITTEDSUBCLASSES(179, "PERMITTEDSUBCLASSES", "PermittedSubclasses", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SOURCEFILE(180, "SOURCEFILE", "SourceFile", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        ENCLOSINGMETHOD(181, "ENCLOSINGMETHOD", "EnclosingMethod", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SOURCEDEBUGEXTENSION(182, "SOURCEDEBUGEXTENSION", "SourceDebugExtension", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //Module statements
        REQUIRES(183, "REQUIRES", "requires", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        EXPORTS(184, "EXPORTS", "exports", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        TO(185, "TO", "to", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        USES(186, "USES", "uses", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        PROVIDES(187, "PROVIDES", "provides", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        WITH(188, "WITH", "with", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        OPENS(189, "OPENS", "opens", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        //

        // Table 4.7.20.2-1 type_path_kind
        ARRAY_TYPEPATH(190, TypeAnnotationTypes.EPathKind.ARRAY.parseKey(), TypeAnnotationTypes.EPathKind.ARRAY.parseKey(),
                EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        INNER_TYPE_TYPEPATH(191, TypeAnnotationTypes.EPathKind.INNER_TYPE.parseKey(), TypeAnnotationTypes.EPathKind.INNER_TYPE.parseKey(),
                EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        WILDCARD_TYPEPATH(192, TypeAnnotationTypes.EPathKind.WILDCARD.parseKey(), TypeAnnotationTypes.EPathKind.WILDCARD.parseKey(),
                EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        TYPE_ARGUMENT_TYPEPATH(193, TypeAnnotationTypes.EPathKind.TYPE_ARGUMENT.parseKey(), TypeAnnotationTypes.EPathKind.TYPE_ARGUMENT.parseKey(),
                EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        // Valhalla Declaration keyword(s) attribute
        LOADABLEDESCRIPTORS(203, "LOADABLEDESCRIPTORS", "LoadableDescriptors", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //
        STACKMAPTABLE_HEADER(208, "STACKMAPTABLE", "StackMapTable", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        STACKMAP_HEADER(209, "STACKMAP", "StackMap", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LINETABLE_HEADER(210, "LINETABLE", "LineTable", "LineNumberTable", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LOCALVARIABLES_HEADER(211, "LOCALVARIABLES", "LocalVariables", "LocalVariableTable", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LOCALVARIABLETYPES_HEADER(212, "LOCALVARIABLETYPES", "LocalVariableTypes", "LocalVariableTypeTable", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        /*
         * Special tokens
         */
        LINE(214, "LINE", "line", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        START(215, "START", "Start", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SLOT(216, "SLOT", "Slot", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NAME(217, "NAME", "Name", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        DESCRIPTOR(218, "DESCRIPTOR", "Descriptor", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NUMBEROFENTRIES(219, "NUMBEROFENTRIES", "number_of_entries", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        FRAMETYPE(220, "FRAMETYPE", "frame_type", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        ENTRYTYPE(221, "ENTRYTYPE", "entry_type", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        OFFSETDELTA(222, "OFFSETDELTA", "offset_delta", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        BYTECODEOFFSET(223, "BYTECODEOFFSET", "offset", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        STACKMAP(224, "STACKMAP", "stack_map", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LOCALSMAP(225, "LOCALSMAP", "locals_map", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        UNSETFIELDS(226, "UNSETFIELDS", "unset_fields", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        ARGUMENTS(227, "ARGUMENTS", "Arguments", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        // new keyword(s)
        IDENTITY(228, "IDENTITY", "identity", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        VALUE(229, "VALUE", "value", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD);

        final static EnumSet<Token> ALL_TOKENS = EnumSet.allOf(Token.class);
        // Misc Keywords
        final private Integer value;                        // 160
        final private String printValue;                    // INF
        final private String parseKey;                      // inf
        final private String alias;                         // Infinity
        final private EnumSet<TokenType> tokenTypes;        // TokenType.MISC, TokenType.MODULE_NAME
        final private KeywordType keywordType;              // KeywordType.KEYWORD

        public static Optional<Token> get(String parseKey, KeywordType keywordType) {
            return ALL_TOKENS.stream().
                    filter(t -> t.keywordType == keywordType).
                    filter(t -> t.parseKey.equals(parseKey) || (t.alias != null && t.alias.equals(parseKey))).
                    findFirst();
        }

        public static Set<Token> getTokenByType(TokenType type) {
            return ALL_TOKENS.stream().filter(t -> t.tokenTypes.contains(type)).
                    collect(Collectors.toSet());
        }

        public static Set<Token> getTokenByKeywordType(KeywordType keywordType) {
            return ALL_TOKENS.stream().filter(t -> t.keywordType.equals(keywordType)).
                    collect(Collectors.toSet());
        }

        /**
         * Checks that this enum element is in an enum list
         *
         * @param tokens the list of enum elements for checking
         * @return true if a tokens list contains this enum element
         */
        public boolean in(Token... tokens) {
            return tokens != null && Arrays.asList(tokens).contains(this);
        }

        // By default, if a KeywordType is not specified, it has the value 'TOKEN'
        Token(Integer val, String print, String parseKey, EnumSet<TokenType> tokenTypes) {
            this(val, print, parseKey, null, tokenTypes, KeywordType.TOKEN);
        }

        Token(Integer val, String print, String parseKey, EnumSet<TokenType> tokenTypes, KeywordType ktype) {
            this(val, print, parseKey, null, tokenTypes, ktype);
        }

        Token(Integer val, String print, String parseKey, String alias, EnumSet<TokenType> tokenTypes, KeywordType ktype) {
            this.value = val;
            this.printValue = print;
            this.parseKey = parseKey;
            this.tokenTypes = tokenTypes;
            this.keywordType = ktype;
            this.alias = alias;
        }

        public String printValue() {
            return printValue;
        }

        public String parseKey() {
            return parseKey;
        }

        public String alias() {
            return this.alias;
        }

        public boolean hasType(TokenType type) {
            return tokenTypes.contains(type);
        }

        public int value() {
            return value;
        }

        public boolean isPossibleJasmIdentifier() {
            return tokenTypes.contains(TokenType.JASM_IDENT);
        }

        public boolean isPossibleModuleName() {
            return tokenTypes.contains(TokenType.MODULE_NAME);
        }

        public boolean isPossibleClassName() {
            return tokenTypes.contains(TokenType.CLASS_NAME);
        }

        /**
         * Checks a token belonging to the table: Table 4.7.20.2-A. Interpretation of type_path_kind values
         *
         * @return true if token is ARRAY, INNER_TYPE, WILDCARD or TYPE_ARGUMENT
         */
        public boolean possibleTypePathKind() {
            return tokenTypes.contains(TokenType.TYPE_PATH_KIND);
        }

        @Override
        public String toString() {
            return "<" + printValue + "> [" + value + "]";
        }
    }

    public static Token keyword_token_ident(String idValue) {
        return Token.get(idValue, KeywordType.KEYWORD).orElse(Token.IDENT);
    }
}
