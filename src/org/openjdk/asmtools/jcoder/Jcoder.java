/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jcoder.JcodTokens.*;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;

/**
 * Compiles just 1 source file
 */
class Jcoder {

    /*-------------------------------------------------------- */
    /* Jcoder Fields */
    ArrayList<ByteBuffer> Classes = new ArrayList<>();
    ByteBuffer buf;
    DataOutputStream bufstream;
    String pkg = null, pkgPrefix = "";
    int depth = 0;
    String tabStr = "";
    Context context = null;
    protected SourceFile env;
    protected Scanner scanner;

    /*-------------------------------------------------------- */
    /* Jcoder inner classes */

    /*-------------------------------------------------------- */
    /* ContextTag (marker) - describes the type of token */
    /*    this is rather cosmetic, no function currently. */
    static public enum ContextTag {
        NULL                (0, ""),
        CLASS               (1, "Class"),
        CONSTANTPOOL        (2, "Constant-Pool"),
        INTERFACES          (3, "Interfaces"),
        INTERFACE           (4, "Interface"),
        METHODS             (5, "Methods"),
        METHOD              (6, "Method"),
        FIELDS              (7, "Fields"),
        FIELD               (8, "Field"),
        ATTRIBUTE           (9, "Attribute");

        private final Integer value;
        private final String  printval;

        ContextTag(Integer val,  String print) {
            value = val;
            printval = print;
        }

        public String printval() {
            return printval;
        }
    }

    /*-------------------------------------------------------- */
    /* ContextVal (marker) - Specific value on a context stack */
    public class ContextVal {

        public ContextTag tag;
        public int compCount;
        public ContextVal owner;

        public ContextVal(ContextTag tg) {
            tag = tg;
            compCount = 0;
            owner = null;
        }

        public ContextVal(ContextTag tg, ContextVal ownr) {
            tag = tg;
            compCount = 0;
            owner = ownr;
        }
    }


    /*-------------------------------------------------------- */
    /* Context - Context stack */
    public class Context {

        Stack<ContextVal> stack;

        private boolean hasCP;
        private boolean hasMethods;
        private boolean hasInterfaces;
        private boolean hasFields;

        Context() {
            stack = new Stack<>();
            init();
        }

        public boolean isConstantPool() {
            if (stack.empty()) {
                return false;
            }
            return (stack.peek().tag == ContextTag.CONSTANTPOOL);
        }

        public void init() {
            stack.removeAllElements();
            hasCP = false;
            hasMethods = false;
            hasInterfaces = false;
            hasFields = false;
        }

        public void update() {
            if (stack.empty()) {
                stack.push(new ContextVal(ContextTag.CLASS));
                return;
            }

            ContextVal currentCtx = stack.peek();
            switch (currentCtx.tag) {
                case CLASS:
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
                    break;
                case INTERFACES:
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.INTERFACE, currentCtx));
                    break;
                //               case INTERFACE:
//                    break;
                case FIELDS:
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.FIELD, currentCtx));
                    break;
                case METHODS:
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.METHOD, currentCtx));
                    break;
                case FIELD:
                case METHOD:
                case ATTRIBUTE:
                    currentCtx.compCount += 1;
                    stack.push(new ContextVal(ContextTag.ATTRIBUTE, currentCtx));
                    break;
                default:
                    break;
            }
        }

        public void exit() {
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
                case INTERFACE:
                case METHOD:
                case FIELD:
                case ATTRIBUTE:
                    if (currentCtx.owner != null) {
                        retval += "[" + currentCtx.owner.compCount + "]";
                    }
            }

            return retval;
        }
    }


    /*-------------------------------------------------------- */
    /* Jcoder */
    /**
     * Create a parser
     */
    protected Jcoder(SourceFile sf, HashMap<String, String> macros) throws IOException {
        scanner = new Scanner(sf, macros);
        env = sf;
        context = new Context();

    }
    /*-------------------------------------------------------- */

    /**
     * Expect a token, return its value, scan the next token or throw an exception.
     */
    protected final void expect(Token t) throws SyntaxError, IOException {
        if (scanner.token != t) {
            env.traceln("expect:" + t + " instead of " + scanner.token);
            switch (t) {
                case IDENT:
//                  env.error(prevPos, "identifier.expected");
                    env.error(scanner.pos, "identifier.expected");
                    break;
                default:
//                  env.error(prevPos, "token.expected", keywordName(t)]);
                    env.error(scanner.pos, "token.expected", t.toString());
                    break;
            }
            throw new SyntaxError();
        }
        scanner.scan();
    }

    protected void recoverField() throws SyntaxError, IOException {
        while (true) {
            switch (scanner.token) {
                case LBRACE:
                    scanner.match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    scanner.match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    scanner.match(Token.LSQBRACKET, Token.RSQBRACKET);
                    scanner.scan();
                    break;

                case RBRACE:
                case EOF:
                case INTERFACE:
                case CLASS:
                    // begin of something outside a class, panic more
//                  endClass(pos);
                    throw new SyntaxError();

                default:
                    // don't know what to do, skip
                    scanner.scan();
                    break;
            }
        }
    }

    /**
     * Parse an array of struct.
     */
    protected void parseArray() throws IOException {
        scanner.scan();
        int length0 = buf.length, pos0 = scanner.pos;
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
            case BYTEINDEX:
                scanner.scan();
                numSize = 1;
                break;
            case SHORTINDEX:
                scanner.scan();
                numSize = 2;
                break;
            case ZEROINDEX:
                scanner.scan();
                numSize = 0;
                break;
            default:
                numSize = 2;
        }

        // skip array size
        if (numSize > 0) {
            buf.append(num_expected, numSize);
        }
        int num_present = parseStruct();
        if (num_expected == -1) {
            env.trace(" buf.writeAt(" + length0 + ", " + num_present + ", " + numSize + ");  ");
            // skip array size
            if (numSize > 0) {
                buf.writeAt(length0, num_present, numSize);
            }
        } else if (num_expected != num_present) {
            env.error(pos0, "warn.array.wronglength", new Integer(num_expected), new Integer(num_present));
        }
    }

    /**
     * Parse a byte array.
     */
    protected void parseByteArray() throws IOException {
        scanner.scan();
        expect(Token.LSQBRACKET);
        int length0 = buf.length, pos0 = scanner.pos;
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
            case BYTEINDEX:
                scanner.scan();
                lenSize = 1;
                break;
            case SHORTINDEX:
                scanner.scan();
                lenSize = 2;
                break;
            case ZEROINDEX:
                scanner.scan();
                lenSize = 0;
                break;
            default:
                lenSize = 4;
        }

        // skip array size
        if (lenSize > 0) {
            buf.append(len_expected, lenSize);
        }
        int length1 = buf.length;
        parseStruct();
        int len_present = buf.length - length1;
        if (len_expected == -1) {
            env.trace(" buf.writeAt(" + length0 + ", " + len_present + ", " + lenSize + ");  ");
            // skip array size
            if (lenSize > 0) {
                buf.writeAt(length0, len_present, lenSize);
            }
        } else if (len_expected != len_present) {
            env.error(pos0, "warn.array.wronglength", new Integer(len_expected), new Integer(len_present));
        }
    }

    /**
     * Parse an Attribute.
     */
    protected void parseAttr() throws IOException {
        scanner.scan();
        expect(Token.LPAREN);
        int cpx; // index int const. pool
        if (scanner.token == Token.INTVAL) {
            cpx = scanner.intValue;
            scanner.scan();

            /*  } else if (token==STRINGVAL) {
             Integer Val=(Integer)(CP_Strings.get(stringValue));
             if (Val == null) {
             env.error(pos, "attrname.notfound", stringValue);
             throw new SyntaxError();
             }
             cpx=Val.intValue();
             */        } else {
            env.error(scanner.pos, "attrname.expected");
            throw new SyntaxError();
        }
        buf.append(cpx, 2);
        int pos0 = scanner.pos, length0 = buf.length;
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
            env.error(pos0, "warn.attr.wronglength", new Integer(len_expected), new Integer(len_present));
        }
    } // end parseAttr

    /**
     * Parse a Component of JavaCard .cap file.
     */
    protected void parseComp() throws IOException {
        scanner.scan();
        expect(Token.LPAREN);
        int tag = scanner.intValue; // index int const. pool
        expect(Token.INTVAL);
        buf.append(tag, 1);
        int pos0 = scanner.pos, length0 = buf.length;
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
            env.error(pos0, "warn.attr.wronglength", new Integer(len_expected), new Integer(len_present));
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
        StringBuilder bldr = new StringBuilder();
        int tabAmt = 4;
        int len = depth * tabAmt;
        for (int i = 0; i < len; i++) {
            bldr.append(" ");
        }
        tabStr = bldr.toString();
    }

    /**
     * Parse a structure.
     */
    protected int parseStruct() throws IOException {
        adjustDepth(true);
        env.traceln(" ");
        env.traceln(tabStr + "Struct { <" + context + "> ");
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
                        scanner.addConstDebug(ConstType.CONSTANT_CLASS);
                        env.trace("class ");
                        scanner.longValue = ConstType.CONSTANT_CLASS.value();
                        scanner.intSize = 1;
                    case INTVAL:
                        env.trace("int [" + scanner.longValue + "] ");
                        buf.append(scanner.longValue, scanner.intSize);
                        scanner.scan();
                        addElem = 1;
                        break;
                    case STRINGVAL:
                        scanner.scan();
                        scanner.addConstDebug(ConstType.CONSTANT_UTF8);
                        env.trace("UTF8 [\"" + scanner.stringValue + "\"] ");
                        bufstream.writeUTF(scanner.stringValue);
                        addElem = 1;
                        break;
                    case LONGSTRINGVAL:
                        scanner.scan();
                        env.traceln("LongString [\"" + scanner.longStringValue.data + "\"] ");
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
                        env.trace("bytes ");
                        parseByteArray();
                        addElem = 1;
                        break;
                    case ATTR:
                        env.trace("attr ");
                        parseAttr();
                        addElem = 1;
                        break;
                    case COMP:
                        env.trace("comp ");
                        parseComp();
                        addElem = 1;
                        break;
                    case RBRACE:
                        scanner.scan();
                        env.traceln(" ");
                        env.traceln(tabStr + "} // Struct  <" + context + "> [");
                        adjustDepth(false);
                        return num + addElem;
                    default:
                        env.traceln("unexp token=" + scanner.token);
                        env.traceln("   scanner.stringval = \"" + scanner.stringValue + "\"");
                        env.error(scanner.pos, "element.expected");
                        throw new SyntaxError();
                }
            } catch (SyntaxError e) {
                recoverField();
            }
        }
    } // end parseStruct

    /**
     * Recover after a syntax error in the file. This involves discarding tokens until an
     * EOF or a possible legal continuation is encountered.
     */
    protected void recoverFile() throws IOException {
        while (true) {
            switch (scanner.token) {
                case CLASS:
                case INTERFACE:
                    // Start of a new source file statement, continue
                    return;

                case LBRACE:
                    scanner.match(Token.LBRACE, Token.RBRACE);
                    scanner.scan();
                    break;

                case LPAREN:
                    scanner.match(Token.LPAREN, Token.RPAREN);
                    scanner.scan();
                    break;

                case LSQBRACKET:
                    scanner.match(Token.LSQBRACKET, Token.RSQBRACKET);
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
     * Parse a class or interface declaration.
     */
    protected void parseClass() throws IOException {
        // Begin a new class
        Token prev = scanner.token;
        scanner.scan();
        buf = new ByteBuffer();
        bufstream = new DataOutputStream(buf);

        String doc = scanner.docComment;

        // Parse the class name
        int p = scanner.pos;
        switch (scanner.token) {
            case STRINGVAL:
                buf.myname = scanner.stringValue;
                break;
            case BYTEINDEX:
            case SHORTINDEX:
            case ATTR:
            case BYTES:
            case MACRO:
            case COMP:
            case FILE:
            case IDENT:
                if (prev == Token.FILE) {
                    buf.myname = scanner.stringValue;
                } else if( prev == Token.MODULE) {
                    buf.myname = "module-info.class";
                } else {
                    buf.myname = scanner.stringValue + ".class";
                }
                break;
            default:
                env.error(scanner.prevPos, "name.expected");
                throw new SyntaxError();
        }
        scanner.scan();

        env.traceln("starting class " + buf.myname);
        // Parse the clause
        switch (scanner.token) {
            case LBRACE:
                parseStruct(); // ??
                break;
            case LSQBRACKET:
                parseArray();
                break;
            case BYTES:
                parseByteArray();
                break;
            case ATTR:
                parseAttr();
                break;
            case COMP:
                parseComp();
                break;
            default:
                env.error(scanner.pos, "struct.expected");
        }

        env.traceln("ending class " + buf.myname);

        // End the class
        env.flushErrors();
        Classes.add(buf);
    } // end parseClass

    /**
     * Parse an Jcoder file.
     */
    public ArrayList<ByteBuffer> parseFile() {
        env.traceln("PARSER");
        context.init();
        try {
            while (scanner.token != Token.EOF) {
                try {
                    switch (scanner.token) {
                        case CLASS:
                        case MODULE:
                        case INTERFACE:
                        case FILE:
                            // Start of a class
//                          scan();
                            parseClass();
                            break;

                        case SEMICOLON:
                            // Bogus semi colon
                            scanner.scan();
                            break;

                        case EOF:
                            // The end
                            return Classes;

                        default:
                            env.traceln("unexpected token=" + scanner.token.toString());
                            env.error(scanner.pos, "toplevel.expected");
                            throw new SyntaxError();
                    }
                } catch (SyntaxError e) {
                    env.traceln("SyntaxError " + e.getMessage());
                    e.printStackTrace();
                    recoverFile();
                }
            }
        } catch (IOException e) {
            env.error(scanner.pos, "io.exception", env.getSource());
            return Classes;
        }
        return Classes;
    } //end parseFile

    /*---------------------------------------------*/
    static char fileSeparator; //=System.getProperty("file.separator");

    /**
     * write to the directory passed with -d option
     */
    public void write(ByteBuffer cls, File destdir) throws IOException {
        String myname = cls.myname;
        if (myname == null) {
            env.error("cannot.write", null);
            return;
        }

        env.traceln("writing class " + myname);
        File outfile;
        if (destdir == null) {
            int startofname = myname.lastIndexOf('/');
            if (startofname != -1) {
                myname = myname.substring(startofname + 1);
            }
            outfile = new File(myname);
        } else {
            env.traceln("writing -d " + destdir.getPath());
            if (fileSeparator == 0) {
                fileSeparator = System.getProperty("file.separator").charAt(0);
            }
            if (fileSeparator != '/') {
                myname = myname.replace('/', fileSeparator);
            }
            outfile = new File(destdir, myname);
            File outdir = new File(outfile.getParent());
            if (!outdir.exists() && !outdir.mkdirs()) {
                env.error("cannot.write", outdir.getPath());
                return;
            }
        }

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
        out.write(cls.data, 0, cls.length);
        try {
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * Writes the classes
     */
    public void write(File destdir) throws IOException {
        for (ByteBuffer cls : Classes) {
            write(cls, destdir);
        }
    }  // end write()
} // end Jcoder
