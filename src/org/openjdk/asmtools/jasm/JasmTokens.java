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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

/**
 *
 * JasmTokens
 *
 * This class contains tokens specific to parsing JASM syntax.
 *
 * The classes in JasmTokens are following a Singleton Pattern. These classes are Enums,
 * and they are contained in private hash maps (lookup tables and reverse lookup tables).
 * These hash maps all have public accessors, which clients use to look-up enums.
 *
 * Tokens in this table carry no external state, and are typically treated as constants.
 * They do not need to be reset.
 */
public class JasmTokens {

    /*-------------------------------------------------------- */
    /* Marker: describes the type of Keyword */
    public enum KeywordType {
        TOKEN            (0, "TOKEN"),
        VALUE            (1, "VALUE"),
        JASMIDENTIFIER   (2, "JASM"),
        KEYWORD          (3, "KEYWORD");

        private final Integer value;
        private final String printValue;

        KeywordType(Integer val, String printValue) {
            this.value = val;
            this.printValue = printValue;
        }

        public String printValue() {
            return printValue;
        }
    }

    /*--------------------------------------------------------  */
    /* Marker - describes the type of token                     */
    /*    this is rather cosmetic, no function currently.       */
    static public enum TokenType {
        MODIFIER            (1, "Modifier"),
        OPERATOR            (2, "Operator"),
        VALUE               (3, "Value"),
        TYPE                (4, "Type"),
        EXPRESSION          (5, "Expression"),
        STATEMENT           (6, "Statement"),
        DECLARATION         (7, "Declaration"),
        PUNCTUATION         (8, "Punctuation"),
        SPECIAL             (9, "Special"),
        JASM                (10, "Jasm"),
        MISC                (11, "Misc"),
        JASM_IDENT          (12, "Jasm identifier"),
        MODULE_NAME         (13, "Module Name"),             // The token can be used as Module Name
        TYPE_PATH_KIND      (14, "Type path kind"),          // Table 4.7.20.2-A Interpretation of type_path_kind values
        CLASS_NAME          (15, "Class Name");              // The token can be used as Class Name

        private final Integer value;
        private final String printValue;

        TokenType(Integer value, String printValue) {
            this.value = value;
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
         *
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
         *
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
         *
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
         *
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
        EOF                 (-1, "EOF",         "EOF",  EnumSet.of(TokenType.MISC, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        COMMA               (0, "COMMA",        ",",    EnumSet.of(TokenType.OPERATOR)),
        ASSIGN              (1, "ASSIGN",       "=",    EnumSet.of(TokenType.OPERATOR)),

        ASGMUL              (2, "ASGMUL",       "*=",   EnumSet.of(TokenType.OPERATOR)),
        ASGDIV              (3, "ASGDIV",       "/=",   EnumSet.of(TokenType.OPERATOR)),
        ASGREM              (4, "ASGREM",       "%=",   EnumSet.of(TokenType.OPERATOR)),
        ASGADD              (5, "ASGADD",       "+=",   EnumSet.of(TokenType.OPERATOR)),
        ASGSUB              (6, "ASGSUB",       "-=",   EnumSet.of(TokenType.OPERATOR)),
        ASGLSHIFT           (7, "ASGLSHIFT",    "<<=",  EnumSet.of(TokenType.OPERATOR)),
        ASGRSHIFT           (8, "ASGRSHIFT",    ">>=",  EnumSet.of(TokenType.OPERATOR)),
        ASGURSHIFT          (9, "ASGURSHIFT",   "<<<=", EnumSet.of(TokenType.OPERATOR)),
        ASGBITAND           (10, "ASGBITAND",   "&=",   EnumSet.of(TokenType.OPERATOR)),
        ASGBITOR            (11, "ASGBITOR",    "|=",   EnumSet.of(TokenType.OPERATOR)),
        ASGBITXOR           (12, "ASGBITXOR",   "^=",   EnumSet.of(TokenType.OPERATOR)),

        COND                (13, "COND",        "?:",   EnumSet.of(TokenType.OPERATOR)),
        OR                  (14, "OR",          "||",   EnumSet.of(TokenType.OPERATOR)),
        AND                 (15, "AND",         "&&",   EnumSet.of(TokenType.OPERATOR)),
        BITOR               (16, "BITOR",       "|",    EnumSet.of(TokenType.OPERATOR)),
        BITXOR              (17, "BITXOR",      "^",    EnumSet.of(TokenType.OPERATOR)),
        BITAND              (18, "BITAND",      "&",    EnumSet.of(TokenType.OPERATOR)),
        NE                  (19, "NE",          "!=",   EnumSet.of(TokenType.OPERATOR)),
        EQ                  (20, "EQ",          "==",   EnumSet.of(TokenType.OPERATOR)),
        GE                  (21, "GE",          ">=",   EnumSet.of(TokenType.OPERATOR)),
        GT                  (22, "GT",          ">",    EnumSet.of(TokenType.OPERATOR)),
        LE                  (23, "LE",          "<=",   EnumSet.of(TokenType.OPERATOR)),
        LT                  (24, "LT",          "<",    EnumSet.of(TokenType.OPERATOR)),
        INSTANCEOF          (25, "INSTANCEOF",  "instanceof",  EnumSet.of(TokenType.OPERATOR)),
        LSHIFT              (26, "LSHIFT",      "<<",   EnumSet.of(TokenType.OPERATOR)),
        RSHIFT              (27, "RSHIFT",      ">>",   EnumSet.of(TokenType.OPERATOR)),
        URSHIFT             (28, "URSHIFT",     "<<<",  EnumSet.of(TokenType.OPERATOR)),
        ADD                 (29, "ADD",         "+",    EnumSet.of(TokenType.OPERATOR)),
        SUB                 (30, "SUB",         "-",    EnumSet.of(TokenType.OPERATOR)),
        DIV                 (31, "DIV",         "/",    EnumSet.of(TokenType.OPERATOR)),
        REM                 (32, "REM",         "%",    EnumSet.of(TokenType.OPERATOR)),
        MUL                 (33, "MUL",         "*",    EnumSet.of(TokenType.OPERATOR)),
        CAST                (34, "CAST",        "cast", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        POS                 (35, "POS",         "+",    EnumSet.of(TokenType.OPERATOR)),
        NEG                 (36, "NEG",         "-",    EnumSet.of(TokenType.OPERATOR)),
        NOT                 (37, "NOT",         "!",    EnumSet.of(TokenType.OPERATOR)),
        BITNOT              (38, "BITNOT",      "~",    EnumSet.of(TokenType.OPERATOR)),
        PREINC              (39, "PREINC",      "++",   EnumSet.of(TokenType.OPERATOR)),
        PREDEC              (40, "PREDEC",      "--",   EnumSet.of(TokenType.OPERATOR)),
        NEWARRAY            (41, "NEWARRAY",    "new",  EnumSet.of(TokenType.OPERATOR)),
        NEWINSTANCE         (42, "NEWINSTANCE", "new",  EnumSet.of(TokenType.OPERATOR)),
        NEWFROMNAME         (43, "NEWFROMNAME", "new",  EnumSet.of(TokenType.OPERATOR)),
        POSTINC             (44, "POSTINC",     "++",   EnumSet.of(TokenType.OPERATOR)),
        POSTDEC             (45, "POSTDEC",     "--",   EnumSet.of(TokenType.OPERATOR)),
        FIELD               (46, "FIELD",       "field", EnumSet.of(TokenType.OPERATOR,   TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        METHOD              (47, "METHOD",      "method",  EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        ARRAYACCESS         (48, "ARRAYACCESS", "[]",   EnumSet.of(TokenType.OPERATOR)),
        NEW                 (49, "NEW",         "new",  EnumSet.of(TokenType.OPERATOR)),
        INC                 (50, "INC",         "++",   EnumSet.of(TokenType.OPERATOR)),
        DEC                 (51, "DEC",         "--",   EnumSet.of(TokenType.OPERATOR)),

        CONVERT             (55, "CONVERT",     "convert", EnumSet.of(TokenType.OPERATOR, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        EXPR                (56, "EXPR",        "expr", EnumSet.of(TokenType.OPERATOR,    TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        ARRAY               (57, "ARRAY",       "array", EnumSet.of(TokenType.OPERATOR,   TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        GOTO                (58, "GOTO",        "goto", EnumSet.of(TokenType.OPERATOR)),
    /*
     * Value tokens
     */
        IDENT               (60, "IDENT",       "Identifier", EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME, TokenType.JASM_IDENT), KeywordType.VALUE),
        BOOLEANVAL          (61, "BOOLEANVAL",  "Boolean",    EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
        BYTEVAL             (62, "BYTEVAL",     "Byte",       EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CHARVAL             (63, "CHARVAL",     "Char",       EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        SHORTVAL            (64, "SHORTVAL",    "Short",      EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        INTVAL              (65, "INTVAL",      "Integer",    EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
        LONGVAL             (66, "LONGVAL",     "Long",       EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
        FLOATVAL            (67, "FLOATVAL",    "Float",      EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
        DOUBLEVAL           (68, "DOUBLEVAL",   "Double",     EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
        STRINGVAL           (69, "STRINGVAL",   "String",     EnumSet.of(TokenType.VALUE, TokenType.CLASS_NAME, TokenType.MODULE_NAME),   KeywordType.VALUE),
    /*
     * Type keywords
     */
        BYTE                (70, "BYTE",        "byte",     EnumSet.of(TokenType.TYPE)),
        CHAR                (71, "CHAR",        "char",     EnumSet.of(TokenType.TYPE)),
        SHORT               (72, "SHORT",       "short",    EnumSet.of(TokenType.TYPE)),
        INT                 (73, "INT",         "int",      EnumSet.of(TokenType.TYPE)),
        LONG                (74, "LONG",        "long",     EnumSet.of(TokenType.TYPE)),
        FLOAT               (75, "FLOAT",       "float",    EnumSet.of(TokenType.TYPE)),
        DOUBLE              (76, "DOUBLE",      "double",   EnumSet.of(TokenType.TYPE)),
        VOID                (77, "VOID",        "void",     EnumSet.of(TokenType.TYPE)),
        BOOLEAN             (78, "BOOLEAN",     "boolean",  EnumSet.of(TokenType.TYPE)),
    /*
     * Expression keywords
     */
        TRUE                (80, "TRUE",        "true",     EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        FALSE               (81, "FALSE",       "false",    EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        THIS                (82, "THIS",        "this",     EnumSet.of(TokenType.EXPRESSION)),
        SUPER               (83, "SUPER",       "super",    EnumSet.of(TokenType.MODIFIER),  KeywordType.KEYWORD),
        NULL                (84, "NULL",        "null",     EnumSet.of(TokenType.EXPRESSION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),

        THIS_CLASS          (85, "this_class",  "this_class",      EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SUPER_CLASS         (86, "super_class", "super_class",     EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
    /*
     * Statement keywords
     */
        IF                  (90, "IF",          "if",       EnumSet.of(TokenType.STATEMENT)),
        ELSE                (91, "ELSE",        "else",     EnumSet.of(TokenType.STATEMENT)),
        FOR                 (92, "FOR",         "for",      EnumSet.of(TokenType.STATEMENT)),
        WHILE               (93, "WHILE",       "while",    EnumSet.of(TokenType.STATEMENT)),
        DO                  (94, "DO",          "do",       EnumSet.of(TokenType.STATEMENT)),
        SWITCH              (95, "SWITCH",      "switch",   EnumSet.of(TokenType.STATEMENT)),
        CASE                (96, "CASE",        "case",     EnumSet.of(TokenType.STATEMENT)),
        DEFAULT             (97,  "DEFAULT",    "default",  EnumSet.of(TokenType.STATEMENT), KeywordType.KEYWORD),
        BREAK               (98, "BREAK",       "break",    EnumSet.of(TokenType.STATEMENT)),
        CONTINUE            (99, "CONTINUE",    "continue", EnumSet.of(TokenType.STATEMENT)),
        RETURN              (100, "RETURN",     "return",   EnumSet.of(TokenType.STATEMENT)),
        TRY                 (101, "TRY",        "try",      EnumSet.of(TokenType.STATEMENT)),

        CATCH               (102, "CATCH",      "catch",    EnumSet.of(TokenType.STATEMENT)),
        FINALLY             (103, "FINALLY",    "finally",  EnumSet.of(TokenType.STATEMENT)),
        THROW               (104, "THROW",      "throw",            EnumSet.of(TokenType.STATEMENT)),
        STAT                (105, "STAT",       "stat",             EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME )),
        EXPRESSION          (106, "EXPRESSION", "expression",       EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME )),
        DECLARATION         (107, "DECLARATION", "declaration",     EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME )),
        VARDECLARATION      (108, "VARDECLARATION", "vdeclaration", EnumSet.of(TokenType.STATEMENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
    /*
     * Declaration keywords
     */
        IMPORT              (110, "IMPORT",     "import",   EnumSet.of(TokenType.DECLARATION,     TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CLASS               (111, "CLASS",      "class",    EnumSet.of(TokenType.DECLARATION,     TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        EXTENDS             (112, "EXTENDS",    "extends",  EnumSet.of(TokenType.DECLARATION,     TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        IMPLEMENTS          (113, "IMPLEMENTS", "implements",   EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        INTERFACE           (114, "INTERFACE",  "interface",    EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        PACKAGE             (115, "PACKAGE",    "package",  EnumSet.of(TokenType.DECLARATION),    KeywordType.KEYWORD),
        ENUM                (116, "ENUM",       "enum",     EnumSet.of(TokenType.DECLARATION),    KeywordType.KEYWORD),
        MANDATED            (117, "MANDATED",   "mandated", EnumSet.of(TokenType.DECLARATION,     TokenType.CLASS_NAME), KeywordType.KEYWORD),
        THROWS              (118, "THROWS",     "throws",   EnumSet.of(TokenType.DECLARATION),    KeywordType.KEYWORD),

    /*
     * Modifier keywords
     */
        ANNOTATION_ACCESS   (119, "ANNOTATION_ACCESS",  "annotation",       EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        PRIVATE             (120, "PRIVATE",            "private",          EnumSet.of(TokenType.MODIFIER),    KeywordType.KEYWORD),
        PUBLIC              (121, "PUBLIC",             "public",           EnumSet.of(TokenType.MODIFIER),    KeywordType.KEYWORD),
        PROTECTED           (122, "PROTECTED",          "protected",        EnumSet.of(TokenType.MODIFIER),    KeywordType.KEYWORD),
        CONST               (123, "CONST",              "const",            EnumSet.of(TokenType.DECLARATION), KeywordType.KEYWORD),
        STATIC              (124, "STATIC",             "static",           EnumSet.of(TokenType.MODIFIER),    KeywordType.KEYWORD),
        TRANSIENT           (125, "TRANSIENT",          "transient",        EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SYNCHRONIZED        (126, "SYNCHRONIZED",       "synchronized",     EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        NATIVE              (127, "NATIVE",             "native",           EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        FINAL               (128, "FINAL",              "final",            EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        VOLATILE            (129, "VOLATILE",           "volatile",         EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        ABSTRACT            (130, "ABSTRACT",           "abstract",         EnumSet.of(TokenType.MODIFIER), KeywordType.KEYWORD),
        TRANSITIVE          (131, "TRANSITIVE",         "transitive",       EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME ), KeywordType.KEYWORD),
        OPEN                (132, "OPEN",               "open",             EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME ), KeywordType.KEYWORD),

    /*
     * Punctuation
     */
        AT_SIGN             (133, "AT",         "@",       EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        SEMICOLON           (134, "SEMICOLON",  ";",       EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        COLON               (135, "COLON",      ":",       EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        QUESTIONMARK        (136, "QUESTIONMARK", "?",     EnumSet.of(TokenType.PUNCTUATION)),
        LBRACE              (137, "LBRACE",     "{",       EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        RBRACE              (138, "RBRACE",     "}",       EnumSet.of(TokenType.PUNCTUATION), KeywordType.VALUE),
        LPAREN              (139, "LPAREN",     "(",       EnumSet.of(TokenType.PUNCTUATION)),
        RPAREN              (140, "RPAREN",     ")",       EnumSet.of(TokenType.PUNCTUATION)),
        LSQBRACKET          (141, "LSQBRACKET", "[",       EnumSet.of(TokenType.PUNCTUATION)),
        RSQBRACKET          (142, "RSQBRACKET", "]",       EnumSet.of(TokenType.PUNCTUATION)),

        ESCAPED_COLON       (201, "ESCCOLON",     "\\:",     EnumSet.of(TokenType.PUNCTUATION)),
        ESCAPED_ATSIGH      (202, "ESCATSIGH",    "\\@",     EnumSet.of(TokenType.PUNCTUATION)),
        ESCAPED_BACKSLASH   (203, "ESCBACKSLASH", "\\\\",    EnumSet.of(TokenType.PUNCTUATION)),
    /*
     * Special tokens
     */
        ERROR               (145, "ERROR",      "error",    EnumSet.of(TokenType.MODIFIER,    TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        COMMENT             (146, "COMMENT",    "comment",  EnumSet.of(TokenType.MODIFIER,    TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        TYPE                (147, "TYPE",       "type",     EnumSet.of(TokenType.MODIFIER,    TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        LENGTH              (148, "LENGTH",     "length",   EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        INLINERETURN        (149, "INLINERETURN", "inline-return",  EnumSet.of(TokenType.MODIFIER)),
        INLINEMETHOD        (150, "INLINEMETHOD", "inline-method",  EnumSet.of(TokenType.MODIFIER)),
        INLINENEWINSTANCE   (151, "INLINENEWINSTANCE", "inline-new",EnumSet.of(TokenType.MODIFIER)),

    /*
     * Added for jasm
     */
        METHODREF           (152, "METHODREF",  "Method",   EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        FIELDREF            (153, "FIELD",      "Field",    EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        STACK               (154, "STACK",      "stack",    EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        LOCAL               (155, "LOCAL",      "locals",   EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        CPINDEX             (156, "CPINDEX",    "CPINDEX",  EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        CPNAME              (157, "CPNAME",     "CPName",   EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        SIGN                (158, "SIGN",       "SIGN",     EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME)),
        BITS                (159, "BITS",       "bits",     EnumSet.of(TokenType.MISC,        TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        INF                 (160, "INF",        "Inf", "Infinity",  EnumSet.of(TokenType.MISC, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NAN                 (161, "NAN",        "NaN",                  EnumSet.of(TokenType.MISC, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        INNERCLASS          (162, "INNERCLASS", "InnerClass",  EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT,  TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        OF                  (163, "OF",         "of",          EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT,  TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SYNTHETIC           (164, "SYNTHETIC",  "synthetic",   EnumSet.of(TokenType.MODIFIER,    TokenType.JASM_IDENT,  TokenType.CLASS_NAME), KeywordType.KEYWORD),
        STRICT              (165, "STRICT",     "strict",      EnumSet.of(TokenType.MODIFIER,    TokenType.JASM_IDENT,  TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        DEPRECATED          (166, "DEPRECATED", "deprecated",  EnumSet.of(TokenType.MODIFIER,    TokenType.JASM_IDENT,  TokenType.CLASS_NAME), KeywordType.KEYWORD),
        VERSION             (167, "VERSION",    "version",     EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT,  TokenType.CLASS_NAME), KeywordType.KEYWORD),
        MODULE              (168, "MODULE",     "module",      EnumSet.of(TokenType.DECLARATION, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        ANNOTATION          (169, "ANNOTATION", "@",           EnumSet.of(TokenType.MISC)),
        PARAM_NAME          (170, "PARAM_NAME", "#",           EnumSet.of(TokenType.MISC)),

        VARARGS             (171, "VARARGS",    "varargs",  EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        BRIDGE              (172, "BRIDGE",     "bridge",   EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        // Declaration keywords
        BOOTSTRAPMETHOD     (173, "BOOTSTRAPMETHOD", "BootstrapMethod", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NESTHOST            (174, "NESTHOST",       "NestHost",         EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SIGNATURE           (175, "SIGNATURE",      "Signature",        EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        NESTMEMBERS         (176, "NESTMEMBERS",    "NestMembers",      EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //
        RECORD              (177, "RECORD",    "Record",                EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        COMPONENT           (178, "COMPONENT", "Component",             EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        //
        PERMITTEDSUBCLASSES (179, "PERMITTEDSUBCLASSES", "PermittedSubclasses", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SOURCEFILE          (180, "SOURCEFILE", "SourceFile", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),
        SOURCEDEBUGEXTENSION(181, "SOURCEDEBUGEXTENSION", "SourceDebugExtension", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME), KeywordType.KEYWORD),

        //Module statements
        REQUIRES            (182, "REQUIRES", "requires", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        EXPORTS             (183, "EXPORTS",  "exports",  EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        TO                  (184, "TO",       "to",       EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        USES                (185, "USES",     "uses",     EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        PROVIDES            (186, "PROVIDES", "provides", EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        WITH                (187, "WITH",     "with",     EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),
        OPENS               (188, "OPENS",    "opens",    EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME), KeywordType.KEYWORD),

        // Table 4.7.20.2-1 type_path_kind
        ARRAY_TYPEPATH      (190, TypeAnnotationTypes.EPathKind.ARRAY.parseKey(),  TypeAnnotationTypes.EPathKind.ARRAY.parseKey(),
                                                                             EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        INNER_TYPE_TYPEPATH    (191, TypeAnnotationTypes.EPathKind.INNER_TYPE.parseKey(),    TypeAnnotationTypes.EPathKind.INNER_TYPE.parseKey(),
                                                                             EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        WILDCARD_TYPEPATH      (192, TypeAnnotationTypes.EPathKind.WILDCARD.parseKey(),    TypeAnnotationTypes.EPathKind.WILDCARD.parseKey(),
                                                                             EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        TYPE_ARGUMENT_TYPEPATH (193, TypeAnnotationTypes.EPathKind.TYPE_ARGUMENT.parseKey(),    TypeAnnotationTypes.EPathKind.TYPE_ARGUMENT.parseKey(),
                                                                             EnumSet.of(TokenType.TYPE_PATH_KIND, TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        // Valhalla
        VALUE                   (200, "VALUE",     "value",     EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME, TokenType.JASM_IDENT), KeywordType.KEYWORD),
        PERMITS_VALUE           (201, "PERMITS_VALUE", "permits_value", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD),
        PRIMITIVE               (202, "PRIMITIVE", "primitive", EnumSet.of(TokenType.MODIFIER, TokenType.CLASS_NAME, TokenType.MODULE_NAME, TokenType.JASM_IDENT ), KeywordType.KEYWORD),
        // Valhalla  Declaration keyword(s)
        PRELOAD                 (203, "PRELOAD",    "Preload",  EnumSet.of(TokenType.DECLARATION, TokenType.JASM_IDENT, TokenType.CLASS_NAME, TokenType.MODULE_NAME ), KeywordType.KEYWORD);

        final static EnumSet<Token> ALL_TOKENS = EnumSet.allOf(Token.class);
        // Misc Keywords
        final private Integer value;                        // 160
        final private String printValue;                    // INF
        final private String parseKey;                      // inf
        final private String  alias;                        // Infinity
        final private EnumSet<TokenType>  tokenType;        // TokenType.MISC, TokenType.MODULE_NAME
        final private KeywordType key_type;                 // KeywordType.KEYWORD

        public static Optional<Token> get(String  parseKey, KeywordType keywordType) {
            return ALL_TOKENS.stream().
                    filter(t->t.key_type == keywordType).
                    filter(t->t.parseKey.equals(parseKey) || ( t.alias != null && t.alias.equals(parseKey))).
                    findFirst();
        }

        /**
         * Checks that this enum element is in an enum list
         *
         * @param tokens the list of enum elements for checking
         * @return true if a tokens list contains this enum element
         */
        public boolean in(Token... tokens) {
            return (tokens == null) ? false : Arrays.asList(tokens).contains(this);
        }

        // By default, if a KeywordType is not specified, it has the value 'TOKEN'
        Token(Integer val, String print, String parseKey, EnumSet<TokenType> tokenType) {
            this(val, print, parseKey, null, tokenType, KeywordType.TOKEN);
        }

        Token(Integer val, String print, String parseKey, String als, EnumSet<TokenType> tokenType) {
            this(val, print, parseKey, als, tokenType, KeywordType.TOKEN);
        }

        Token(Integer val, String print, String parseKey, EnumSet<TokenType> tokenType, KeywordType ktype) {
            this(val, print, parseKey, null, tokenType, ktype);
        }

        Token(Integer val, String print, String parseKey, String als, EnumSet<TokenType> tokenType, KeywordType ktype) {
            this.value = val;
            this.printValue = print;
            this.parseKey = parseKey;
            this.tokenType = tokenType;
            this.key_type = ktype;
            this.alias = als;
        }

        public String printValue() {
            return printValue;
        }

        public String parseKey() {
            return parseKey;
        }

        public int value() {
            return value;
        }

        public boolean isPossibleJasmIdentifier() {
            return tokenType.contains(TokenType.JASM_IDENT);
        }

        public boolean isPossibleModuleName() {  return tokenType.contains(TokenType.MODULE_NAME); }

        public boolean isPossibleClassName() {  return tokenType.contains(TokenType.CLASS_NAME); }

        /**
         * Checks a token belonging to the table: Table 4.7.20.2-A. Interpretation of type_path_kind values
         *
         * @return true if token is ARRAY, INNER_TYPE, WILDCARD or TYPE_ARGUMENT
         */
        public boolean possibleTypePathKind() { return tokenType.contains(TokenType.TYPE_PATH_KIND); }

        @Override
        public String toString() {
            return "<" + printValue + "> [" + value + "]";
        }
    }

    public static Token keyword_token_ident(String idValue) {
        return Token.get(idValue,KeywordType.KEYWORD).orElse(Token.IDENT);
    }
}
