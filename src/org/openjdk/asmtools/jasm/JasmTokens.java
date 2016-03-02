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

import java.util.HashMap;

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
    static public enum KeywordType {
        TOKEN            (0, "TOKEN"),
        VALUE            (1, "VALUE"),
        JASMIDENTIFIER   (2, "JASM"),
        KEYWORD          (3, "KEYWORD");

        private final Integer value;
        private final String printval;

        KeywordType(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }


    /*-------------------------------------------------------- */
    /* Marker - describes the type of token */
    /*    this is rather cosmetic, no function currently. */
    static public enum TokenType {
        MODIFIER            (0, "Modifier"),
        OPERATOR            (1, "Operator"),
        VALUE               (2, "Value"),
        TYPE                (3, "Type"),
        EXPRESSION          (4, "Expression"),
        STATEMENT           (5, "Statement"),
        DECLARATION         (6, "Declaration"),
        PUNCTUATION         (7, "Punctuation"),
        SPECIAL             (8, "Special"),
        JASM                (9, "Jasm"),
        MISC                (10, "Misc");

        private final Integer value;
        private final String printval;

        TokenType(Integer val, String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }

    /*-------------------------------------------------------- */
    /**
     * Scanner Tokens (Definitive List)
     */
    static public enum Token {
        EOF                 (-1, "EOF",         "EOF",  TokenType.MISC),
        COMMA               (0, "COMMA",        ",",    TokenType.OPERATOR),
        ASSIGN              (1, "ASSIGN",       "=",    TokenType.OPERATOR),

        ASGMUL              (2, "ASGMUL",       "*=",   TokenType.OPERATOR),
        ASGDIV              (3, "ASGDIV",       "/=",   TokenType.OPERATOR),
        ASGREM              (4, "ASGREM",       "%=",   TokenType.OPERATOR),
        ASGADD              (5, "ASGADD",       "+=",   TokenType.OPERATOR),
        ASGSUB              (6, "ASGSUB",       "-=",   TokenType.OPERATOR),
        ASGLSHIFT           (7, "ASGLSHIFT",    "<<=",  TokenType.OPERATOR),
        ASGRSHIFT           (8, "ASGRSHIFT",    ">>=",  TokenType.OPERATOR),
        ASGURSHIFT          (9, "ASGURSHIFT",   "<<<=", TokenType.OPERATOR),
        ASGBITAND           (10, "ASGBITAND",   "&=",   TokenType.OPERATOR),
        ASGBITOR            (11, "ASGBITOR",    "|=",   TokenType.OPERATOR),
        ASGBITXOR           (12, "ASGBITXOR",   "^=",   TokenType.OPERATOR),

        COND                (13, "COND",        "?:",   TokenType.OPERATOR),
        OR                  (14, "OR",          "||",   TokenType.OPERATOR),
        AND                 (15, "AND",         "&&",   TokenType.OPERATOR),
        BITOR               (16, "BITOR",       "|",    TokenType.OPERATOR),
        BITXOR              (17, "BITXOR",      "^",    TokenType.OPERATOR),
        BITAND              (18, "BITAND",      "&",    TokenType.OPERATOR),
        NE                  (19, "NE",          "!=",   TokenType.OPERATOR),
        EQ                  (20, "EQ",          "==",   TokenType.OPERATOR),
        GE                  (21, "GE",          ">=",   TokenType.OPERATOR),
        GT                  (22, "GT",          ">",    TokenType.OPERATOR),
        LE                  (23, "LE",          "<=",   TokenType.OPERATOR),
        LT                  (24, "LT",          "<",    TokenType.OPERATOR),
        INSTANCEOF          (25, "INSTANCEOF",  "instanceof",  TokenType.OPERATOR),
        LSHIFT              (26, "LSHIFT",      "<<",   TokenType.OPERATOR),
        RSHIFT              (27, "RSHIFT",      ">>",   TokenType.OPERATOR),
        URSHIFT             (28, "URSHIFT",     "<<<",  TokenType.OPERATOR),
        ADD                 (29, "ADD",         "+",    TokenType.OPERATOR),
        SUB                 (30, "SUB",         "-",    TokenType.OPERATOR),
        DIV                 (31, "DIV",         "/",    TokenType.OPERATOR),
        REM                 (32, "REM",         "%",    TokenType.OPERATOR),
        MUL                 (33, "MUL",         "*",    TokenType.OPERATOR),
        CAST                (34, "CAST",        "cast", TokenType.OPERATOR),
        POS                 (35, "POS",         "+",    TokenType.OPERATOR),
        NEG                 (36, "NEG",         "-",    TokenType.OPERATOR),
        NOT                 (37, "NOT",         "!",    TokenType.OPERATOR),
        BITNOT              (38, "BITNOT",      "~",    TokenType.OPERATOR),
        PREINC              (39, "PREINC",      "++",   TokenType.OPERATOR),
        PREDEC              (40, "PREDEC",      "--",   TokenType.OPERATOR),
        NEWARRAY            (41, "NEWARRAY",    "new",  TokenType.OPERATOR),
        NEWINSTANCE         (42, "NEWINSTANCE", "new",  TokenType.OPERATOR),
        NEWFROMNAME         (43, "NEWFROMNAME", "new",  TokenType.OPERATOR),
        POSTINC             (44, "POSTINC",     "++",   TokenType.OPERATOR),
        POSTDEC             (45, "POSTDEC",     "--",   TokenType.OPERATOR),
        FIELD               (46, "FIELD",       "field", TokenType.OPERATOR),
        METHOD              (47, "METHOD",      "method",  TokenType.OPERATOR),
        ARRAYACCESS         (48, "ARRAYACCESS", "[]",   TokenType.OPERATOR),
        NEW                 (49, "NEW",         "new",  TokenType.OPERATOR),
        INC                 (50, "INC",         "++",   TokenType.OPERATOR),
        DEC                 (51, "DEC",         "--",   TokenType.OPERATOR),

        CONVERT             (55, "CONVERT",     "convert", TokenType.OPERATOR),
        EXPR                (56, "EXPR",        "expr", TokenType.OPERATOR),
        ARRAY               (57, "ARRAY",       "array", TokenType.OPERATOR),
        GOTO                (58, "GOTO",        "goto", TokenType.OPERATOR),



    /*
     * Value tokens
     */
        IDENT               (60, "IDENT",       "Identifier", TokenType.VALUE, KeywordType.VALUE, true),
        BOOLEANVAL          (61, "BOOLEANVAL",  "Boolean",  TokenType.VALUE, KeywordType.VALUE),
        BYTEVAL             (62, "BYTEVAL",     "Byte",     TokenType.VALUE),
        CHARVAL             (63, "CHARVAL",     "Char",     TokenType.VALUE),
        SHORTVAL            (64, "SHORTVAL",    "Short",    TokenType.VALUE),
        INTVAL              (65, "INTVAL",      "Integer",  TokenType.VALUE, KeywordType.VALUE),
        LONGVAL             (66, "LONGVAL",     "Long",     TokenType.VALUE, KeywordType.VALUE),
        FLOATVAL            (67, "FLOATVAL",    "Float",    TokenType.VALUE, KeywordType.VALUE),
        DOUBLEVAL           (68, "DOUBLEVAL",   "Double",   TokenType.VALUE, KeywordType.VALUE),
        STRINGVAL           (69, "STRINGVAL",   "String",   TokenType.VALUE, KeywordType.VALUE),

    /*
     * Type keywords
     */
        BYTE                (70, "BYTE",        "byte",     TokenType.TYPE),
        CHAR                (71, "CHAR",        "char",     TokenType.TYPE),
        SHORT               (72, "SHORT",       "short",    TokenType.TYPE),
        INT                 (73, "INT",         "int",      TokenType.TYPE),
        LONG                (74, "LONG",        "long",     TokenType.TYPE),
        FLOAT               (75, "FLOAT",       "float",    TokenType.TYPE),
        DOUBLE              (76, "DOUBLE",      "double",   TokenType.TYPE),
        VOID                (77, "VOID",        "void",     TokenType.TYPE),
        BOOLEAN             (78, "BOOLEAN",     "boolean",  TokenType.TYPE),

    /*
     * Expression keywords
     */
        TRUE                (80, "TRUE",        "true",     TokenType.EXPRESSION),
        FALSE               (81, "FALSE",       "false",    TokenType.EXPRESSION),
        THIS                (82, "THIS",        "this",     TokenType.EXPRESSION),
        SUPER               (83, "SUPER",       "super",     TokenType.MODIFIER, KeywordType.KEYWORD),
        NULL                (84, "NULL",        "null",     TokenType.EXPRESSION),

    /*
     * Statement keywords
     */
        IF                  (90, "IF",          "if",       TokenType.STATEMENT),
        ELSE                (91, "ELSE",        "else",     TokenType.STATEMENT),
        FOR                 (92, "FOR",         "for",      TokenType.STATEMENT),
        WHILE               (93, "WHILE",       "while",    TokenType.STATEMENT),
        DO                  (94, "DO",          "do",       TokenType.STATEMENT),
        SWITCH              (95, "SWITCH",      "switch",   TokenType.STATEMENT),
        CASE                (96, "CASE",        "case",     TokenType.STATEMENT),
        DEFAULT             (97,  "DEFAULT",    "default",  TokenType.STATEMENT, KeywordType.KEYWORD),
        BREAK               (98, "BREAK",       "break",    TokenType.STATEMENT),
        CONTINUE            (99, "CONTINUE",    "continue", TokenType.STATEMENT),
        RETURN              (100, "RETURN",     "return",   TokenType.STATEMENT),
        TRY                 (101, "TRY",        "try",      TokenType.STATEMENT),
        CATCH               (102, "CATCH",      "catch",    TokenType.STATEMENT),
        FINALLY             (103, "FINALLY",    "finally",  TokenType.STATEMENT),
        THROW               (104, "THROW",      "throw",    TokenType.STATEMENT),
        STAT                (105, "STAT",       "stat",     TokenType.STATEMENT),
        EXPRESSION          (106, "EXPRESSION", "expression",  TokenType.STATEMENT),
        DECLARATION         (107, "DECLARATION", "declaration",   TokenType.STATEMENT),
        VARDECLARATION      (108, "VARDECLARATION", "vdeclaration", TokenType.STATEMENT),

    /*
     * Declaration keywords
     */
        IMPORT              (110, "IMPORT",     "import",   TokenType.DECLARATION),
        CLASS               (111, "CLASS",      "class",    TokenType.DECLARATION, KeywordType.KEYWORD),
        EXTENDS             (112, "EXTENDS",    "extends",  TokenType.DECLARATION, KeywordType.KEYWORD),
        IMPLEMENTS          (113, "IMPLEMENTS", "implements", TokenType.DECLARATION, KeywordType.KEYWORD),
        INTERFACE           (114, "INTERFACE",  "interface", TokenType.DECLARATION, KeywordType.KEYWORD),
        PACKAGE             (115, "PACKAGE",    "package",  TokenType.DECLARATION, KeywordType.KEYWORD),
        ENUM                (116, "ENUM",       "enum",     TokenType.DECLARATION, KeywordType.KEYWORD),
        MANDATED            (117, "MANDATED",   "mandated", TokenType.DECLARATION, KeywordType.KEYWORD),
     /*
     * Modifier keywords
     */
        PRIVATE             (120, "PRIVATE",    "private",  TokenType.MODIFIER, KeywordType.KEYWORD),
        PUBLIC              (121, "PUBLIC",     "public",   TokenType.MODIFIER, KeywordType.KEYWORD),
        PROTECTED           (122, "PROTECTED",  "protected", TokenType.MODIFIER, KeywordType.KEYWORD),
        CONST               (123, "CONST",      "const",    TokenType.DECLARATION, KeywordType.KEYWORD),
        STATIC              (124, "STATIC",     "static",   TokenType.MODIFIER, KeywordType.KEYWORD),
        TRANSIENT           (125, "TRANSIENT",  "transient", TokenType.MODIFIER, KeywordType.KEYWORD),
        SYNCHRONIZED        (126, "SYNCHRONIZED", "synchronized", TokenType.MODIFIER, KeywordType.KEYWORD),
        NATIVE              (127, "NATIVE",     "native",   TokenType.MODIFIER, KeywordType.KEYWORD),
        FINAL               (128, "FINAL",      "final",    TokenType.MODIFIER, KeywordType.KEYWORD),
        VOLATILE            (129, "VOLATILE",   "volatile", TokenType.MODIFIER, KeywordType.KEYWORD),
        ABSTRACT            (130, "ABSTRACT",   "abstract", TokenType.MODIFIER, KeywordType.KEYWORD),

    /*
     * Punctuation
     */
        SEMICOLON           (135, "SEMICOLON",  ";",    TokenType.PUNCTUATION, KeywordType.VALUE),
        COLON               (136, "COLON",      ":",    TokenType.PUNCTUATION, KeywordType.VALUE),
        QUESTIONMARK        (137, "QUESTIONMARK", "?",  TokenType.PUNCTUATION),
        LBRACE              (138, "LBRACE",     "{",    TokenType.PUNCTUATION, KeywordType.VALUE),
        RBRACE              (139, "RBRACE",     "}",    TokenType.PUNCTUATION, KeywordType.VALUE),
        LPAREN              (140, "LPAREN",     "(",    TokenType.PUNCTUATION),
        RPAREN              (141, "RPAREN",     ")",    TokenType.PUNCTUATION),
        LSQBRACKET          (142, "LSQBRACKET", "[",    TokenType.PUNCTUATION),
        RSQBRACKET          (143, "RSQBRACKET", "]",    TokenType.PUNCTUATION),
        THROWS              (144, "THROWS",     "throws",  TokenType.DECLARATION, KeywordType.KEYWORD),
    /*
     * Special tokens
     */
        ERROR               (145, "ERROR",      "error",    TokenType.MODIFIER),
        COMMENT             (146, "COMMENT",    "comment",   TokenType.MODIFIER),
        TYPE                (147, "TYPE",       "type",     TokenType.MODIFIER),
        LENGTH              (148, "LENGTH",     "length",   TokenType.DECLARATION),
        INLINERETURN        (149, "INLINERETURN", "inline-return", TokenType.MODIFIER),
        INLINEMETHOD        (150, "INLINEMETHOD", "inline-method", TokenType.MODIFIER),
        INLINENEWINSTANCE   (151, "INLINENEWINSTANCE", "inline-new", TokenType.MODIFIER),

    /*
     * Added for jasm
     */
        METHODREF           (152, "METHODREF",  "Method",   TokenType.DECLARATION, KeywordType.KEYWORD, true),
        FIELDREF            (153, "FIELD",      "Field",    TokenType.DECLARATION, KeywordType.KEYWORD, true),
        STACK               (154, "STACK",      "stack",    TokenType.DECLARATION, KeywordType.KEYWORD, true),
        LOCAL               (155, "LOCAL",      "locals",   TokenType.DECLARATION, KeywordType.KEYWORD, true),
        CPINDEX             (156, "CPINDEX",    "CPINDEX",  TokenType.DECLARATION, true),
        CPNAME              (157, "CPNAME",     "CPName",   TokenType.DECLARATION, true),
        SIGN                (158, "SIGN",       "SIGN",     TokenType.DECLARATION, true),
        BITS                (159, "BITS",       "bits",     TokenType.MISC, KeywordType.KEYWORD, true),
        INF                 (160, "INF",        "Inf", "Infinity", TokenType.MISC, KeywordType.KEYWORD),
        NAN                 (161, "NAN",        "NaN",      TokenType.MISC, KeywordType.KEYWORD, true),
        INNERCLASS          (162, "INNERCLASS", "InnerClass", TokenType.DECLARATION, KeywordType.KEYWORD, true),
        OF                  (163, "OF",         "of",       TokenType.DECLARATION, KeywordType.KEYWORD, true),
        SYNTHETIC           (164, "SYNTHETIC",  "synthetic", TokenType.MODIFIER, KeywordType.KEYWORD, true),
        STRICT              (165, "STRICT",     "strict",   TokenType.MODIFIER, KeywordType.KEYWORD, true),
        DEPRECATED          (166, "DEPRECATED", "deprecated", TokenType.MODIFIER, KeywordType.KEYWORD, true),
        VERSION             (167, "VERSION",    "version",  TokenType.DECLARATION, KeywordType.KEYWORD, true),
        MODULE              (168, "MODULE",     "module",   TokenType.DECLARATION, KeywordType.KEYWORD),
        ANNOTATION          (169, "ANNOTATION", "@",        TokenType.MISC),
        PARAM_NAME          (173, "PARAM_NAME", "#",        TokenType.MISC),

        VARARGS             (170, "VARARGS",    "varargs",  TokenType.MODIFIER, KeywordType.KEYWORD),
        BRIDGE              (171, "BRIDGE",     "bridge",   TokenType.MODIFIER, KeywordType.KEYWORD),

        // Declaration keywords
        BOOTSTRAPMETHOD     (172, "BOOTSTRAPMETHOD", "BootstrapMethod", TokenType.DECLARATION, KeywordType.KEYWORD, true),

        //Module statements
        REQUIRES            (180, "REQUIRES", "requires", TokenType.DECLARATION, KeywordType.KEYWORD, true),
        EXPORTS             (182, "EXPORTS",  "exports",  TokenType.DECLARATION, KeywordType.KEYWORD, true),
        TO                  (183, "TO",       "to",       TokenType.DECLARATION, KeywordType.KEYWORD, true),
        USES                (184, "USES",     "uses",     TokenType.DECLARATION, KeywordType.KEYWORD, true),
        PROVIDES            (185, "PROVIDES", "provides", TokenType.DECLARATION, KeywordType.KEYWORD, true),
        WITH                (186, "WITH",     "with",     TokenType.DECLARATION, KeywordType.KEYWORD, true);

        // Misc Keywords
        private Integer value;
        private String printval;
        private String alias;
        private TokenType tok_type;
        private KeywordType key_type;
        private String parsekey;
        private boolean possible_jasm_identifier;

        // By default, if a KeywordType is not specified, it has the value 'TOKEN'
        Token(Integer val, String print, String op, TokenType ttype) {
            init(val, print, op, null, ttype, KeywordType.TOKEN, false);
        }

        Token(Integer val, String print, String op, TokenType ttype, boolean ident) {
            init(val, print, op, null, ttype, KeywordType.TOKEN, ident);
        }

        Token(Integer val, String print, String op, String als, TokenType ttype) {
            init(val, print, op, als, ttype, KeywordType.TOKEN, false);
        }

        Token(Integer val, String print, String op, TokenType ttype, KeywordType ktype) {
            init(val, print, op, null, ttype, ktype, false);
        }

        Token(Integer val, String print, String op, TokenType ttype, KeywordType ktype, boolean ident) {
            init(val, print, op, null, ttype, ktype, ident);
        }

        Token(Integer val, String print, String op, String als, TokenType ttype, KeywordType ktype) {
            init(val, print, op, als, ttype, ktype, false);
        }

        private void init(Integer val, String print, String op, String als, TokenType ttype, KeywordType ktype, boolean ident) {
            value = val;
            printval = print;
            parsekey = op;
            tok_type = ttype;
            key_type = ktype;
            alias = als;
            possible_jasm_identifier = ident;
        }

        public String printval() {
            return printval;
        }

        public String parsekey() {
            return parsekey;
        }

        public int value() {
            return value;
        }

        public boolean possibleJasmIdentifier() {
            return possible_jasm_identifier;
        }

        @Override
        public String toString() {
            return "<" + printval + "> [" + value + "]";
        }

    }

    /**
     * Initialized keyword and token Hash Maps (and Reverse Tables)
     */
    static protected final int MaxTokens = 172;
    private static HashMap<Integer, Token> TagToTokens = new HashMap<>(MaxTokens);
    private static HashMap<String, Token> SymbolToTokens = new HashMap<>(MaxTokens);
    private static HashMap<String, Token> ParsekeyToTokens = new HashMap<>(MaxTokens);

    static protected final int MaxValTokens = 12;
    private static HashMap<Integer, Token> TagToValTokens = new HashMap<>(MaxValTokens);
    private static HashMap<String, Token> SymbolToValTokens = new HashMap<>(MaxValTokens);
    private static HashMap<String, Token> ParsekeyToValTokens = new HashMap<>(MaxValTokens);

    private static HashMap<Integer, Token> PossibleJasmIdentifiers = new HashMap<>(MaxValTokens);

    static protected final int MaxKeywords = 40;
    private static HashMap<Integer, Token> TagToKeywords = new HashMap<>(MaxKeywords);
    private static HashMap<String, Token> SymbolToKeywords = new HashMap<>(MaxKeywords);
    private static HashMap<String, Token> ParsekeyToKeywords = new HashMap<>(MaxKeywords);

    static {

        // register all of the tokens
        for (Token tk : Token.values()) {
            registerToken(tk);
        }
    }

    private static void registerToken(Token tk) {
        // Tag is a keyword
        if (tk.key_type == KeywordType.KEYWORD) {
            TagToKeywords.put(tk.value, tk);
            if (tk.alias != null) {
                ParsekeyToKeywords.put(tk.alias, tk);
            }
            SymbolToKeywords.put(tk.printval, tk);
            if (tk.parsekey != null) {
                ParsekeyToKeywords.put(tk.parsekey, tk);
            }
        }

        // Values (and Keywords) go on the Val tokens list
        if (tk.key_type == KeywordType.KEYWORD
                || tk.key_type == KeywordType.VALUE) {
            TagToValTokens.put(tk.value, tk);
            SymbolToValTokens.put(tk.printval, tk);
            if (tk.alias != null) {
                SymbolToValTokens.put(tk.alias, tk);
            }
            if (tk.parsekey != null) {
                ParsekeyToValTokens.put(tk.parsekey, tk);
            }
        }

        // make the list of 'possible jasm identifiers'
        if (tk.possible_jasm_identifier) {
            PossibleJasmIdentifiers.put(tk.value(), tk);
        }

        // Finally, register all tokens
        TagToTokens.put(tk.value, tk);
        SymbolToTokens.put(tk.printval, tk);
        ParsekeyToTokens.put(tk.printval, tk);
    }

    /* Token accessors */
    public static Token token(int tk) {
        return TagToTokens.get(tk);
    }

    public static Token val_token(int tk) {
        return TagToValTokens.get(tk);
    }

    public static Token keyword_token(int tk) {
        return TagToKeywords.get(tk);
    }

    public static Token possibleJasmIdentifiers(int token) {
        return PossibleJasmIdentifiers.get(token);
    }

    /* Reverse lookup accessors */
    public static Token token(String parsekey) {
        return ParsekeyToTokens.get(parsekey);
    }

    public static Token val_token(String parsekey) {
        return ParsekeyToValTokens.get(parsekey);
    }

    public static Token keyword_token(String parsekey) {
        return ParsekeyToKeywords.get(parsekey);
    }

    /* Reverse lookup by ID accessors */
    public static Token token_ID(String ID) {
        return ParsekeyToTokens.get(ID);
    }

    public static Token val_token_ID(String ID) {
        return ParsekeyToValTokens.get(ID);
    }

    public static Token keyword_token_ID(String ID) {
        return ParsekeyToKeywords.get(ID);
    }

    public static String keywordName(int token) {
        String retval = null;
        Token tk = keyword_token(token);
        if (tk != null) {
            retval = tk.parsekey;
        }
        return retval;
    }

    public static int val_token_int(String idValue) {
        Token kwd = val_token(idValue);
        int retval = Token.IDENT.value;

        if (kwd != null) {
            retval = kwd.value;
        }
        return retval;
    }

    public static Token keyword_token_ident(String idValue) {
        Token kwd = keyword_token(idValue);

        if (kwd == null) {
            kwd = Token.IDENT;
        }
        return kwd;
    }

    public static int keyword_token_int(String idValue) {
        return keyword_token_ident(idValue).value();
    }
}
