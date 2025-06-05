/*
 * Copyright (c) 2009, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jcoder;

import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.EAttribute;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_CLASS;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType.CONSTANT_UTF8;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.SEMICOLON;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.STRINGVAL;
import static org.openjdk.asmtools.jcoder.JcodTokens.Token;

/**
 * Compiles just 1 source file
 */
class Jcoder {

    protected JcoderEnvironment environment;
    protected Scanner scanner;
    protected Hashtable<Integer, EAttribute> attributes = new Hashtable<>();

    /*-------------------------------------------------------- */
    /* Jcoder Fields */
    private final ArrayList<ByteBuffer> Classes = new ArrayList<>();
    private ByteBuffer buf;
    private DataOutputStream outStream;
    private int depth = 0;
    private String tabStr = "";
    private final Context context;

    /*-------------------------------------------------------- */
    /* Jcoder inner classes */

    /*-------------------------------------------------------- */
    /* ContextTag (marker) - describes the type of token */
    /*    this is rather cosmetic, no function currently. */
    private enum ContextTag {
        NULL(""),
        CLASS("Class"),
        CONSTANTPOOL("Constant-Pool"),
        INTERFACES("Interfaces"),
        INTERFACE("Interface"),
        METHODS("Methods"),
        METHOD("Method"),
        FIELDS("Fields"),
        FIELD("Field"),
        ATTRIBUTE("Attribute");

        private final String printValue;

        ContextTag(String value) {
            printValue = value;
        }

        public String printval() {
            return printValue;
        }
    }

    /* ContextVal (marker) - Specific value on a context stack */
    private static class ContextVal {

        public ContextTag tag;
        int compCount;
        ContextVal owner;

        ContextVal(ContextTag tg) {
            tag = tg;
            compCount = 0;
            owner = null;
        }

        ContextVal(ContextTag tg, ContextVal ownr) {
            tag = tg;
            compCount = 0;
            owner = ownr;
        }
    }

    /* Context - Context stack */
    public static class Context {

        Stack<ContextVal> stack;

        private boolean hasCP;
        private boolean hasMethods;
        private boolean hasInterfaces;
        private boolean hasFields;

        // Index of the constant pool entry, used for identity of the Attribute
        private int cpIndex = 0;

        Context() {
            stack = new Stack<>();
            init();
        }

        boolean isConstantPool() {
            return !stack.empty() && (stack.peek().tag == ContextTag.CONSTANTPOOL);
        }

        boolean isAttribute() {
            return !stack.empty() && (stack.peek().tag == ContextTag.ATTRIBUTE);
        }

        public void init() {
            stack.removeAllElements();
            hasCP = false;
            hasMethods = false;
            hasInterfaces = false;
            hasFields = false;
            cpIndex = 0;
        }

        void update() {
            if (stack.empty()) {
                stack.push(new ContextVal(ContextTag.CLASS));
                return;
            }

            ContextVal currentCtx = stack.peek();
            switch (currentCtx.tag) {
                case CLASS -> {
                    if (!hasCP) {
                        stack.push(new ContextVal(ContextTag.CONSTANTPOOL));
                        hasCP = true;
                    } else if (!hasInterfaces) {
                        stack.push(new ContextVal(ContextTag.INTERFACES));
                        hasInterfaces = true;
                    } else if (!hasFields) {
                        stack.push(new ContextVal(ContextTag.FIELDS));
                        hasFields = true;
                    } else if (!hasMethods) {
                        stack.push(new ContextVal(ContextTag.METHODS));
                        hasMethods = true;
                    } else {
                        // must be class attributes
                        currentCtx.compCount += 1;
                        stack.push(new ContextVal(ContextTag.ATTRIBUTE, currentCtx));
                    }
                }
                case INTERFACES -> {
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.INTERFACE, currentCtx));
                }
                case FIELDS -> {
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.FIELD, currentCtx));
                }
                case METHODS -> {
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.METHOD, currentCtx));
                }
                case FIELD, METHOD, ATTRIBUTE -> {
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.ATTRIBUTE, currentCtx));
                }
            }
        }

        void exit() {
            if (!stack.isEmpty()) {
                stack.pop();
            }
        }

        public String toString() {
            if (stack.isEmpty()) {
                return "";
            }
            ContextVal currentCtx = stack.peek();
            String retval = currentCtx.tag.printval();
            switch (currentCtx.tag) {
                case INTERFACE, METHOD, FIELD, ATTRIBUTE -> {
                    if (currentCtx.owner != null) {
                        retval += "[" + currentCtx.owner.compCount + "]";
                    }
                }
            }
            return retval;
        }

        public void setConstantPoolIndex(int cpx) {
            cpIndex = cpx;
        }

        public int getConstantPoolIndex() {
            return cpIndex;
        }
    }

    /* Jcoder */

    /**
     * Create a parser
     */
    Jcoder(JcoderEnvironment environment, HashMap<String, String> macros) throws IOException {
        scanner = new Scanner(environment, macros);
        this.environment = environment;
        context = new Context();

    }

    /**
     * Expect a token, return its value, scan the next token or throw an exception.
     */
    private void expect(Token t) throws SyntaxError, IOException {
        if (scanner.token != t) {
            environment.traceln(() -> "expect: %s instead of %s".formatted(t.printval(), scanner.token.printval()));
            if (t == Token.IDENT) {
                environment.error(scanner.pos, "err.identifier.expected");
            } else {
                environment.error(scanner.pos, "err.token.expected", t.toString());
            }
            throw new SyntaxError();
        }
        scanner.scan();
    }

    private void recoverField() throws SyntaxError, IOException {
        while (true) {
            switch (scanner.token) {
                case LBRACE -> {
                    scanner.match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                }
                case LPAREN -> {
                    scanner.match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                }
                case LSQBRACKET -> {
                    scanner.match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                }
                case RBRACE, EOF, INTERFACE, CLASS ->
                    // begin of something outside a class, panic more
                        throw new SyntaxError();
                default ->
                    // don't know what to do, skip
                        scanner.scan();
            }
        }
    }

    /**
     * Parse an array of struct.
     */
    private void parseArray() throws IOException {
        scanner.scan();
        int length0 = buf.length;
        long pos0 = scanner.pos;
        int num_expected;
        if (scanner.token == Token.INTVAL) {
            num_expected = scanner.intValue;
            scanner.scan();
        } else {
            num_expected = -1;
        }
        expect(Token.RSQBRACKET);
        int numSize;
        switch (scanner.token) {
            case BYTEINDEX -> {
                scanner.scan();
                numSize = 1;
            }
            case SHORTINDEX -> {
                scanner.scan();
                numSize = 2;
            }
            case ZEROINDEX -> {
                scanner.scan();
                numSize = 0;
            }
            default -> numSize = 2;
        }
        buf.append(num_expected, numSize);

        int num_present = parseStruct();
        if (num_expected == -1) {
            environment.trace(" buf.writeAt(" + length0 + ", " + num_present + ", " + numSize + ");  ");
            // skip array size
            if (numSize > 0) {
                buf.writeAt(length0, num_present, numSize);
            }
        } else if (num_expected != num_present) {
            if (context.isConstantPool() && num_expected == num_present + 1) return;
            environment.warning(pos0, "warn.array.wronglength", num_expected, num_present);
        }
    }

    /**
     * Parse a byte array.
     */
    private void parseByteArray() throws IOException {
        scanner.scan();
        expect(Token.LSQBRACKET);
        int length0 = buf.length;
        long pos0 = scanner.pos;
        int len_expected;
        if (scanner.token == Token.INTVAL) {
            len_expected = scanner.intValue;
            scanner.scan();
        } else {
            len_expected = -1;
        }
        expect(Token.RSQBRACKET);
        int lenSize;
        switch (scanner.token) {
            case BYTEINDEX -> {
                scanner.scan();
                lenSize = 1;
            }
            case SHORTINDEX -> {
                scanner.scan();
                lenSize = 2;
            }
            case ZEROINDEX -> {
                scanner.scan();
                lenSize = 0;
            }
            default -> lenSize = 4;
        }

        // skip array size
        if (lenSize > 0) {
            buf.append(len_expected, lenSize);
        }
        int length1 = buf.length;
        parseStruct();
        int len_present = buf.length - length1;
        if (len_expected == -1) {
            environment.trace(" buf.writeAt(" + length0 + ", " + len_present + ", " + lenSize + ");  ");
            // skip array size
            if (lenSize > 0) {
                buf.writeAt(length0, len_present, lenSize);
            }
        } else if (len_expected != len_present) {
            environment.warning(pos0, "warn.array.wronglength", len_expected, len_present);
        }
    }

    /**
     * Parse an Attribute.
     */
    private void parseAttr() throws IOException {
        scanner.scan();
        expect(Token.LPAREN);
        int cpx; // index int const. pool
        if (scanner.token == Token.INTVAL) {
            cpx = scanner.intValue;
            context.setConstantPoolIndex(cpx);
            scanner.scan();
        } else {
            environment.error(scanner.pos, "err.attrname.expected");
            throw new SyntaxError();
        }
        buf.append(cpx, 2);
        long pos0 = scanner.pos;
        int length0 = buf.length;
        int len_expected;
        if (scanner.token == Token.COMMA) {
            scanner.scan();
            len_expected = scanner.intValue;
            expect(Token.INTVAL);
        } else {
            len_expected = -1;
        }
        buf.append(len_expected, 4);
        expect(Token.RPAREN);
        parseStruct();
        int len_present = buf.length - (length0 + 4);
        if (len_expected == -1) {
            buf.writeAt(length0, len_present, 4);
        } else if (len_expected != len_present) {
            environment.warning(pos0, "warn.attr.wronglength", len_expected, len_present);
        }
    } // end parseAttr

    /**
     * Parse a Component of JavaCard .cap file.
     */
    private void parseComp() throws IOException {
        scanner.scan();
        expect(Token.LPAREN);
        int tag = scanner.intValue; // index int const. pool
        expect(Token.INTVAL);
        buf.append(tag, 1);
        long pos0 = scanner.pos;
        int length0 = buf.length;
        int len_expected;
        if (scanner.token == Token.COMMA) {
            scanner.scan();
            len_expected = scanner.intValue;
            expect(Token.INTVAL);
        } else {
            len_expected = -1;
        }
        buf.append(len_expected, 2);
        expect(Token.RPAREN);
        parseStruct();
        int len_present = buf.length - (length0 + 2);
        if (len_expected == -1) {
            buf.writeAt(length0, len_present, 2);
        } else if (len_expected != len_present) {
            environment.warning(pos0, "warn.attr.wronglength", len_expected, len_present);
        }
    } // end parseComp

    private void adjustDepth(boolean up) {
        if (up) {
            depth += 1;
            context.update();
            scanner.setDebugCP(context.isConstantPool());
        } else {
            depth -= 1;
            context.exit();
        }
        int tabAmt = 4;
        int len = depth * tabAmt;
        tabStr = " ".repeat(Math.max(0, len));
    }

    /**
     * Parse a structure.
     */
    private int parseStruct() throws IOException {
        int scanedCFV = 0;
        int minor = 0;
        int major = 0;
        adjustDepth(true);
        environment.traceln(" ");
        environment.traceln(tabStr + "MapStruct { <" + context + "> ");
        expect(Token.LBRACE);
        int num = 0;
        int addElem = 0;
        while (true) {
            try {
                switch (scanner.token) {
                    case COMMA: // ignored
                        scanner.scan();
                        break;
                    case SEMICOLON:
                        num++;
                        addElem = 0;
                        scanner.scan();
                        break;
                    case CLASS:
                        scanner.addConstDebug(CONSTANT_CLASS);
                        environment.trace("class ");
                        scanner.longValue = CONSTANT_CLASS.getTag();
                        scanner.intSize = 1;
                    case INTVAL:
                        environment.trace("int [" + scanner.longValue + "] ");
                        if (scanner.longValue == 0xCAFEBABEl && environment.cfv.isSetByParameter()) {
                            scanedCFV++;
                        } else {
                            if (scanedCFV > 0) {
                                if (scanedCFV == 1) {
                                    scanedCFV++;
                                    minor = scanner.intValue;
                                    // skip writing until major analysis
                                    scanner.scan();
                                    break;
                                } else {
                                    scanedCFV = 0;
                                    major = scanner.intValue;
                                    environment.trace(" Got file version: " + major + ":" + minor);
                                    // check version update if needed and go on
                                    environment.cfv.setFileVersion((short) major, (short) minor);
                                    buf.append(environment.cfv.minor_version(), scanner.intSize);
                                    buf.append(environment.cfv.major_version(), scanner.intSize);
                                    scanner.scan();
                                    addElem = 1;
                                    break;
                                }
                            }
                        }
                        buf.append(scanner.longValue, scanner.intSize);
                        scanner.scan();
                        addElem = 1;
                        break;
                    case STRINGVAL:
                        String name = parseUTF();
                        if (context.isConstantPool()) {
                            EAttribute attr = EAttribute.get(name);
                            if (attr != EAttribute.ATT_Unrecognized) {
                                attributes.put(num, attr);
                            }
                        }
                        // SEMICOLON was already parsed in
                        num++;
                        addElem = 0;
                        break;
                    case LONGSTRINGVAL:
                        scanner.scan();
                        environment.traceln(tabStr + "LongString [\"" + Arrays.toString(scanner.longStringValue.data) + "\"] ");
                        buf.write(scanner.longStringValue.data, 0, scanner.longStringValue.length);
                        addElem = 1;
                        break;
                    case LBRACE:
                        parseStruct();
                        addElem = 1;
                        break;
                    case LSQBRACKET:
                        parseArray();
                        addElem = 1;
                        break;
                    case BYTES:
                        environment.trace("bytes ");
                        parseByteArray();
                        addElem = 1;
                        break;
                    case ATTR:
                        environment.trace("attr ");
                        parseAttr();
                        addElem = 1;
                        break;
                    case COMP:
                        environment.trace("comp ");
                        parseComp();
                        addElem = 1;
                        break;
                    case RBRACE:
                        scanner.scan();
                        environment.traceln(System.lineSeparator() + tabStr + "} // MapStruct  <" + context + "> ]");
                        adjustDepth(false);
                        return num + addElem;
                    default:
                        environment.traceln("unexp token=" + scanner.token);
                        environment.traceln("   scanner.stringval = \"" + scanner.stringValue + "\"");
                        environment.error(scanner.pos, "err.element.expected");
                        throw new SyntaxError();
                }
            } catch (SyntaxError e) {
                recoverField();
            }
        }
    } // end parseStruct

    String decodeText(String input, String encoding) throws IOException {
        return
                new BufferedReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(input.getBytes()),
                                Charset.forName(encoding)))
                        .readLine();
    }

    // Parse multiline UTF strings
    private String parseUTF() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean prevSemicolonParsed = true;
        while (true) {
            switch (scanner.token) {
                case STRINGVAL:
                    if (!prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                        throw new SyntaxError();
                    }
                    scanner.scan();
                    scanner.addConstDebug(CONSTANT_UTF8);
                    environment.traceln(tabStr + "UTF8 [\"" + scanner.stringValue + "\"] ");
                    sb.append(scanner.stringValue);
                    prevSemicolonParsed = false;
                    break;
                case SEMICOLON:
                    if (prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected", STRINGVAL.parseKey());
                        throw new SyntaxError();
                    }
                    prevSemicolonParsed = true;
                    scanner.scan();
                    break;
                default:
                    if (!prevSemicolonParsed) {
                        environment.error(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                        throw new SyntaxError();
                    }
                    if (context.isAttribute() &&
                            attributes.get(context.getConstantPoolIndex()) == EAttribute.ATT_SourceDebugExtension) {
                        outStream.writeBytes(sb.toString());
                    } else {
                        outStream.writeUTF(sb.toString());
                    }
                    return sb.toString();
            }
        }
    }

    /**
     * Recover after a syntax error in the file. This involves discarding tokens until an
     * EOF or a possible legal continuation is encountered.
     */
    private void recoverFile() throws IOException {
        while (true) {
            switch (scanner.token) {
                case CLASS, INTERFACE, EOF -> {
                    // Start of a new source file statement, continue or EOF
                    return;
                }
                case LBRACE -> {
                    scanner.match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                }
                case LPAREN -> {
                    scanner.match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                }
                case LSQBRACKET -> {
                    scanner.match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                }
                default ->
                    // Don't know what to do, skip
                        scanner.scan();
            }
        }
    }

    /**
     * Parse module declaration
     */
    private void parseModule() throws IOException {
        // skip module name as a redundant element
        scanner.skipTill(Scanner.LBRACE);
        buf = new ByteBuffer();
        outStream = new DataOutputStream(buf);
        buf.className = "module-info.class";
        scanner.scan();
        environment.traceln("starting " + buf.className);
        // Parse the clause
        parseClause();
        environment.traceln("ending " + buf.className);
    }

    /**
     * Parse a class or interface declaration.
     */
    private void parseClass(Token prev) throws IOException {
        scanner.scan();
        buf = new ByteBuffer();
        outStream = new DataOutputStream(buf);
        // Parse the class name
        switch (scanner.token) {
            case BYTEINDEX, SHORTINDEX, ATTR, BYTES, MACRO, COMP, FILE, IDENT -> {
                if (prev == Token.FILE) {
                    buf.className = scanner.stringValue;
                } else {
                    buf.className = scanner.stringValue + ".class";
                }
            }
            case STRINGVAL -> buf.className = scanner.stringValue;
            default -> {
                environment.error(scanner.prevPos, "err.name.expected", "\"" + scanner.token.parsekey() + "\"");
                throw new SyntaxError();
            }
        }
        scanner.scan();
        environment.traceln("starting class " + buf.className);
        // Parse the clause
        parseClause();
        environment.traceln("ending class " + buf.className);


    } // end parseClass

    private void parseClause() throws IOException {
        switch (scanner.token) {
            case LBRACE -> parseStruct();
            case LSQBRACKET -> parseArray();
            case BYTES -> parseByteArray();
            case ATTR -> parseAttr();
            case COMP -> parseComp();
            default -> environment.error(scanner.pos, "err.struct.expected");
        }
    }

    // Parse an Jcoder file.
    void parseFile() {
        environment.traceln("PARSER");
        context.init();
        if (scanner.token == Token.EOF) {
            environment.error("err.file.empty", environment.getSimpleInputFileName());
            return;
        }
        try {
            while (scanner.token != Token.EOF) {
                try {
                    switch (scanner.token) {
                        case CLASS, MODULE, INTERFACE, FILE -> {
                            Token t = scanner.token;
                            if (t == Token.MODULE) {
                                parseModule();
                            } else {
                                parseClass(t);
                            }
                            // End of the class,interface or module
                            Classes.add(buf);
                        }
                        case SEMICOLON ->
                            // Bogus semi colon
                                scanner.scan();
                        case EOF -> {
                            return;   // The end
                        }
                        default -> {
                            environment.traceln("unexpected token=" + scanner.token);
                            environment.error(scanner.pos, "err.toplevel.expected");
                            throw new SyntaxError();
                        }
                    }
                } catch (SyntaxError e) {
                    String msg = e.getMessage();
                    environment.traceln(() -> "SyntaxError %s".formatted(msg == null ? "" : msg));
                    environment.printException(e);
                    recoverFile();
                }
            }
        } catch (IOException e) {
            environment.error(scanner.pos, "err.io.exception", environment.getSimpleInputFileName());
        }
    } //end parseFile

    /**
     * write to the directory passed with -d option
     */
    public void write(ByteBuffer cls) throws IOException {
        String myname = cls.className;
        if (myname == null) {
            environment.error("err.cannot.write");
            return;
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(environment.getToolOutput().getDataOutputStream())) {
            bos.write(cls.data, 0, cls.length);
        } catch (IOException ignored) {
        }
    }

    /**
     * Writes the classes
     */
    public void write() throws IOException {
        for (ByteBuffer cls : Classes) {
            environment.getToolOutput().startClass(cls.className, Optional.empty(), environment);
            write(cls);
            environment.getToolOutput().finishClass(cls.className);
        }
    }
}
