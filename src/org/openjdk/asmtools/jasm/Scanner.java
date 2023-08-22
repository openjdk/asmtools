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


import org.openjdk.asmtools.asmutils.StringUtils;
import org.openjdk.asmtools.common.SyntaxError;

import java.util.function.Predicate;

import static java.lang.String.format;
import static org.openjdk.asmtools.common.CompilerConstants.EOF;
import static org.openjdk.asmtools.common.CompilerConstants.OFFSET_BITS;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.JasmTokens.keyword_token_ident;

/**
 * A Scanner for Jasm tokens. Errors are reported to the environment object.<p>
 * <p>
 * The scanner keeps track of the current token, the value of the current token (if any),
 * and the start position of the current token.<p>
 * <p>
 * The scan() method advances the scanner to the next token in the input.<p>
 * <p>
 * The match() method is used to quickly match opening brackets (ie: '(', '{', or '[')
 * with their closing counter part. This is useful during error recovery.<p>
 * <p>
 * The compiler treats either "\n", "\r" or "\r\n" as the end of a line.<p>
 */
public class Scanner extends ParseBase {

    // The current character
    protected int ch;

    // Current token
    protected Token token;

    // The position of the current token
    protected int pos;

    // Token values.
    protected int intValue;
    protected long longValue;
    protected float floatValue;
    protected double doubleValue;
    protected String stringValue;
    protected String idValue;
    protected int radix;        // Radix, when reading int or long

    /*   doc comment preceding the most recent token  */
    protected String docComment;
    /**
     * The position of the previous token
     */
    protected int prevPos;
    protected int sign;              // sign, when reading number
    protected boolean inBits;        // inBits prefix, when reading number

    /* A growable character buffer. */
    private int count;
    private char[] buffer = new char[32];
    //
    private Predicate<Integer> escapingAllowed;
    private final Predicate<Integer> noFunc = (ch) -> false;
    private final Predicate<Integer> yesAndProcessFunc = (ch) -> {
        boolean res = ((ch == '\\') || (ch == ':') || (ch == '@'));
        if (res)
            putCh('\\');
        return res;
    };

    /**
     * main constructor.
     * <p>
     * Create a scanner to scan an input stream.
     */
    protected Scanner(JasmEnvironment environment) {
        super.init(environment);
        escapingAllowed = noFunc;
        ch = environment.read();
        xscan();
    }

    protected void scanModuleStatement() {
        try {
            escapingAllowed = yesAndProcessFunc;
            scan();
        } finally {
            escapingAllowed = noFunc;
        }
    }

    /**
     * Scan the next token.
     */
    protected void scan() {
        int signloc = 1;
        prevPos = pos;
        prefix:
        for (; ; ) {
            xscan();
            switch (token) {
                case SIGN:
                    signloc = signloc * intValue;
                    break;
                default:
                    break prefix;
            }
        }
        switch (token) {
            case INTVAL, LONGVAL, FLOATVAL, DOUBLEVAL, INF, NAN -> sign = signloc;
            default -> {
            }
        }
    }

    /**
     * Check the token may be identifier
     */
    protected final boolean checkTokenIdent() {
        return token.isPossibleJasmIdentifier();
    }

    /**
     * Expects a token, scans the next token or throws an exception.
     */
    protected final void expect(Token t) throws SyntaxError {
        check(t);
        scan();
    }

    /**
     * Checks a token, throws an exception if not the same
     */
    protected final void check(Token t) throws SyntaxError {
        if (token != t) {
            if ((t != Token.IDENT) || !checkTokenIdent()) {
                environment.traceln("expect: " + t + " instead of " + token);
                if (t == Token.IDENT) {
                    environment.error(pos, "err.identifier.expected");
                } else {
                    environment.error(pos, "err.token.expected", "<" + t.parseKey() + ">");
                }
                environment.traceln("<<<<<PROBLEM>>>>>>>: ");
                throw new SyntaxError();
            }
        }
    }

    private void putCh(int ch) {
        if (count == buffer.length) {
            char[] newBuffer = new char[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
        buffer[count++] = (char) ch;
    }

    private String bufferString() {
        char[] buf = new char[count];
        System.arraycopy(buffer, 0, buf, 0, count);
        return new String(buf);
    }

    /**
     * Scan a comment. This method should be called once the initial /, * and the next
     * character have been read.
     */
    private void skipComment() {
        while (true) {
            switch (ch) {
                case EOF:
                    environment.error(pos, "err.eof.in.comment");
                    return;
                case '*':
                    if ((ch = environment.read()) == '/') {
                        ch = environment.read();
                        return;
                    }
                    break;
                default:
                    ch = environment.read();
                    break;
            }
        }
    }

    /**
     * Scan a doc comment. This method should be called once the initial /, * and * have
     * been read. It gathers the content of the comment (without leading spaces and '*'s)
     * in the string buffer.
     */
    @SuppressWarnings("empty-statement")
    private String scanDocComment() {
        count = 0;

        if (ch == '*') {
            do {
                ch = environment.read();
            } while (ch == '*');
            if (ch == '/') {
                ch = environment.read();
                return "";
            }
        }
        switch (ch) {
            case '\n', ' ' -> ch = environment.read();
        }

        boolean seenstar = false;
        int c = count;
        while (true) {
            switch (ch) {
                case EOF:
                    environment.error(pos, "err.eof.in.comment");
                    return bufferString();
                case '\n':
                    putCh('\n');
                    ch = environment.read();
                    seenstar = false;
                    c = count;
                    break;
                case ' ':
                case '\t':
                    putCh(ch);
                    ch = environment.read();
                    break;
                case '*':
                    if (seenstar) {
                        if ((ch = environment.read()) == '/') {
                            ch = environment.read();
                            count = c;
                            return bufferString();
                        }
                        putCh('*');
                    } else {
                        seenstar = true;
                        count = c;
                        while ((ch = environment.read()) == '*') ;
                        switch (ch) {
                            case ' ' -> ch = environment.read();
                            case '/' -> {
                                ch = environment.read();
                                count = c;
                                return bufferString();
                            }
                        }
                    }
                    break;
                default:
                    if (!seenstar) {
                        seenstar = true;
                    }
                    putCh(ch);
                    ch = environment.read();
                    c = count;
                    break;
            }
        }
    }

    /**
     * Scan a decimal at this point
     */
    private void scanCPRef() {
        switch (ch = environment.read()) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                boolean overflow = false;
                long value = ch - '0';
                count = 0;
                putCh(ch);                // save character in buffer
                numberLoop:
                for (; ; ) {
                    switch (ch = environment.read()) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            putCh(ch);
                            if (overflow) {
                                break;
                            }
                            value = (value * 10) + (ch - '0');
                            overflow = (value > 0xFFFF);
                            break;
                        default:
                            break numberLoop;
                    }
                } // while true
                intValue = (int) value;
                stringValue = bufferString();
                token = Token.CPINDEX;
                if (overflow) {
                    environment.error(pos, "err.overflow");
                }
            }
            default -> {
                stringValue = Character.toString((char) ch);
                environment.error(environment.getPosition(), "err.invalid.number", stringValue);
                intValue = 0;
                token = Token.CPINDEX;
                ch = environment.read();
            }
        }
    } // scanCPRef()

    /**
     * Scan a number. The first digit of the number should be the current character. We
     * may be scanning hex, decimal, or octal at this point
     */
    private void scanNumber() {
        boolean seenNonOctal = false;
        boolean overflow = false;
        radix = (ch == '0' ? 8 : 10);
        long value = ch - '0';
        count = 0;
        putCh(ch);                // save character in buffer
        numberLoop:
        for (; ; ) {
            switch (ch = environment.read()) {
                case '.':
                    if (radix == 16) {
                        break numberLoop; // an illegal character
                    }
                    scanReal();
                    return;

                case '8':
                case '9':
                    // We can't yet throw an error if reading an octal.  We might
                    // discover we're really reading a real.
                    seenNonOctal = true;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    putCh(ch);
                    if (radix == 10) {
                        overflow = overflow || (value * 10) / 10 != value;
                        value = (value * 10) + (ch - '0');
                        overflow = overflow || (value - 1 < -1);
                    } else if (radix == 8) {
                        overflow = overflow || (value >>> 61) != 0;
                        value = (value << 3) + (ch - '0');
                    } else {
                        overflow = overflow || (value >>> 60) != 0;
                        value = (value << 4) + (ch - '0');
                    }
                    break;
                case 'd':
                case 'D':
                case 'e':
                case 'E':
                case 'f':
                case 'F':
                    if (radix != 16) {
                        scanReal();
                        return;
                    }
                    // fall through
                case 'a':
                case 'A':
                case 'b':
                case 'B':
                case 'c':
                case 'C':
                    putCh(ch);
                    if (radix != 16) {
                        break numberLoop; // an illegal character
                    }
                    overflow = overflow || (value >>> 60) != 0;
                    value = (value << 4) + 10
                            + Character.toLowerCase((char) ch) - 'a';
                    break;
                case 'l':
                case 'L':
                    ch = environment.read();        // skip over 'l'
                    longValue = value;
                    token = Token.LONGVAL;
                    break numberLoop;
                case 'x':
                case 'X':
                    // if the first character is a '0' and this is the second
                    // letter, then read in a hexadecimal number.  Otherwise, error.
                    if (count == 1 && radix == 8) {
                        radix = 16;
                        break;
                    } else {
                        // we'll get an illegal character error
                        break numberLoop;
                    }
                default:
                    intValue = (int) value;
                    token = Token.INTVAL;
                    break numberLoop;
            }
        } // while true
        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (Character.isLetterOrDigit(ch) || ch == '.') {
            environment.error(environment.getPosition(), "err.invalid.number", Character.toString((char) ch));
            do {
                ch = environment.read();
            } while (Character.isLetterOrDigit(ch) || ch == '.');
            intValue = 0;
            token = Token.INTVAL;
        } else if (radix == 8 && seenNonOctal) {
            intValue = 0;
            token = Token.INTVAL;
            environment.error(environment.getPosition(), "err.invalid.octal.number");
        } else if (overflow
                || (token == Token.INTVAL
                && ((radix == 10) ? (intValue - 1 < -1)
                : ((value & 0xFFFFFFFF00000000L) != 0)))) {
            intValue = 0;        // so we don't get second overflow in Parser
            longValue = 0;
            environment.error(pos, "err.overflow");
        }
    } // scanNumber()

    /**
     * Scan a float. We are either looking at the decimal, or we have already seen it and
     * put it into the buffer. We haven't seen an exponent. Scan a float. Should be called
     * with the current character is either the 'e', 'E' or '.'
     */
    private void scanReal() {
        boolean seenExponent = false;
        boolean isSingleFloat = false;
        char lastChar;
        if (ch == '.') {
            putCh(ch);
            ch = environment.read();
        }

        numberLoop:
        for (; ; ch = environment.read()) {
            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    putCh(ch);
                    break;
                case 'e':
                case 'E':
                    if (seenExponent) {
                        break numberLoop; // we'll get a format error
                    }
                    putCh(ch);
                    seenExponent = true;
                    break;
                case '+':
                case '-':
                    lastChar = buffer[count - 1];
                    if (lastChar != 'e' && lastChar != 'E') {
                        break numberLoop; // this isn't an error, though!
                    }
                    putCh(ch);
                    break;
                case 'f':
                case 'F':
                    ch = environment.read(); // skip over 'f'
                    isSingleFloat = true;
                    break numberLoop;
                case 'd':
                case 'D':
                    ch = environment.read(); // skip over 'd'
                    // fall through
                default:
                    break numberLoop;
            } // sswitch
        } // loop

        // we have just finished reading the number.  The next thing better
        // not be a letter or digit.
        if (Character.isLetterOrDigit(ch) || ch == '.') {
            environment.error(environment.getPosition(), "err.invalid.number", Character.toString((char) ch));
            do {
                ch = environment.read();
            } while (Character.isLetterOrDigit(ch) || ch == '.');
            doubleValue = 0;
            token = Token.DOUBLEVAL;
        } else {
            token = isSingleFloat ? Token.FLOATVAL : Token.DOUBLEVAL;
            try {
                lastChar = buffer[count - 1];
                if (lastChar == 'e' || lastChar == 'E'
                        || lastChar == '+' || lastChar == '-') {
                    environment.error(environment.getPosition() - 1, "err.float.format");
                } else if (isSingleFloat) {
                    floatValue = Float.parseFloat(bufferString());
                    if (Float.isInfinite(floatValue)) {
                        environment.error(pos, "err.overflow");
                    }
                } else {
                    doubleValue = Double.parseDouble(bufferString());
                    if (Double.isInfinite(doubleValue)) {
                        environment.error(pos, "err.overflow");
                        environment.error(pos, "err.overflow");
                    }
                }
            } catch (NumberFormatException ee) {
                environment.error(pos, "err.float.format");
                doubleValue = 0;
                floatValue = 0;
            }
        }
    } // scanReal

    /**
     * Scan an escape character.
     *
     * @return the character or '\\'
     */
    private int scanEscapeChar() {
        int p = environment.getPosition();

        switch (ch = environment.read()) {
            case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                int n = ch - '0';
                for (int i = 2; i > 0; i--) {
                    switch (ch = environment.read()) {
                        case '0', '1', '2', '3', '4', '5', '6', '7' -> n = (n << 3) + ch - '0';
                        default -> {
                            if (n > 0xFF) {
                                environment.error(p, "err.invalid.escape.char");
                            }
                            return n;
                        }
                    }
                }
                ch = environment.read();
                if (n > 0xFF) {
                    environment.error(p, "err.invalid.escape.char");
                }
                return n;
            }
            case 'r' -> {
                ch = environment.read();
                return '\r';
            }
            case 'n' -> {
                ch = environment.read();
                return '\n';
            }
            case 'f' -> {
                ch = environment.read();
                return '\f';
            }
            case 'b' -> {
                ch = environment.read();
                return '\b';
            }
            case 't' -> {
                ch = environment.read();
                return '\t';
            }
            case '\\' -> {
                ch = environment.read();
                return '\\';
            }
            case '\"' -> {
                ch = environment.read();
                return '\"';
            }
            case '\'' -> {
                ch = environment.read();
                return '\'';
            }
            case 'u' -> {
                int unich = environment.convertUnicode();
                ch = environment.read();
                return unich;
            }
        }
        return '\\';
    }

    /**
     * Scan a string. The current character should be the opening " of the string.
     */
    private void scanString() {
        token = Token.STRINGVAL;
        count = 0;
        ch = environment.read();

        // Scan a String
        while (true) {
            switch (ch) {
                case EOF -> {
                    environment.error(pos, "err.eof.in.string");
                    stringValue = bufferString();
                    return;
                }
                case '\n' -> {
                    ch = environment.read();
                    environment.error(pos, "err.newline.in.string");
                    stringValue = bufferString();
                    return;
                }
                case '"' -> {
                    ch = environment.read();
                    stringValue = bufferString();
                    return;
                }
                case '\\' -> {
                    int c = scanEscapeChar();
                    if (c >= 0) {
                        putCh((char) c);
                    }
                }
                default -> {
                    putCh(ch);
                    ch = environment.read();
                }
            }
        }
    }

    /**
     * Scan an Identifier. The current character should be the first character of the
     * identifier.
     */
    private void scanIdentifier(char[] prefix) {
        int firstChar;
        count = 0;
        if (prefix != null) {
            for (; ; ) {
                for (char c : prefix) putCh(c);
                ch = environment.read();
                if (ch == '\\') {
                    ch = environment.read();
                    if (ch == 'u') {
                        ch = environment.convertUnicode();
                        if (!Character.isLetterOrDigit(ch)) {
                            prefix = new char[]{(char) ch};
                            continue;
                        }
                    } else if (escapingAllowed.test(ch)) {
                        prefix = new char[]{(char) ch};
                        continue;
                    }
                    int p = environment.getPosition();
                    environment.error(p, "err.invalid.escape.char");
                }
                break;
            }
        }
        firstChar = ch;
        boolean firstIteration = true;
        scanloop:
        while (true) {
            putCh(ch);
            ch = environment.read();

            // Check to see if the annotation marker is at
            // the front of the identifier.
            if (firstIteration && firstChar == '@') {
                // Maybe a type annotation
                if (ch == 'T') {  // type annotation
                    putCh(ch);
                    ch = environment.read();
                }

                // is either a runtime visible or invisible annotation
                if (ch == '+' || ch == '-') {  // regular annotation
                    // possible annotation -
                    // need to eat up the '@+' or '@-'
                    putCh(ch);
                    ch = environment.read();
                }
                idValue = bufferString();
                stringValue = idValue;
                token = Token.ANNOTATION;
                return;
            }
            firstIteration = false;
            if (!Character.isJavaIdentifierPart(ch) && !StringUtils.isOneOf(ch, '-', '[', ']', '(', ')', '<', '>')) {
                switch (ch) {
                    case '/': {// may be comment right after identifier
                        int c = environment.lookForward();
                        if ((c == '*') || (c == '/')) {
                            break scanloop; // yes, comment
                        }
                        break; // no, continue to parse identifier
                    }
                    case '\\':
                        ch = environment.read();
                        if (ch == 'u') {
                            ch = environment.convertUnicode();
                            if (!Character.isLetterOrDigit(ch)) {
                                break;
                            }
                        } else if (escapingAllowed.test(ch)) {
                            break;
                        }
                        int p = environment.getPosition();
                        environment.error(p, "err.invalid.escape.char");
                    default:
                        break scanloop;
                } // end switch
            }
        } // end scanloop
        idValue = bufferString();
        stringValue = idValue;
        token = keyword_token_ident(idValue);
        traceMethodInfoLn(format("token = %s value = '%s'", token, idValue));
    } // end scanIdentifier

    //==============================
    @SuppressWarnings("empty-statement")
    protected final void xscan() { // throws IOException {
        docComment = null;
        loop:
        for (; ; ) {
            pos = environment.getPosition();
            if (Character.isLetter(ch) || StringUtils.isOneOf(ch, '$', '_', '@', '[', ']', '(', ')', '<', '>')) {
                scanIdentifier(null);
                break;
            } else if (ch == EOF) {
                token = Token.EOF;
                break;
            } else if (ch == '\n' || ch == '\r' || ch == ' ' || ch == '\t' || ch == '\f') {
                ch = environment.read();
            } else if (ch == '/') {
                switch (ch = environment.read()) {
                    case '/':
                        // Parse a // comment
                        while (((ch = environment.read()) != EOF) && (ch != '\n')) ;
                        break;
                    case '*':
                        ch = environment.read();
                        if (ch == '*') {
                            docComment = scanDocComment();
                        } else {
                            skipComment();
                        }
                        break;
                    default:
                        token = Token.DIV;
                        break loop;
                }
            } else if (ch == '"') {
                scanString();
                break;
            } else if (ch == '-') {
                intValue = -1;
                token = Token.SIGN;
                ch = environment.read();
                break;
            } else if (ch == '+') {
                intValue = 1;
                ch = environment.read();
                token = Token.SIGN;
                break;
            } else if (ch == '0' || ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' ||
                    ch == '6' || ch == '7' || ch == '8' || ch == '9') {
                scanNumber();
                break;
            } else if (ch == '.') {
                switch (ch = environment.read()) {
                    case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        count = 0;
                        putCh('.');
                        scanReal();
                    }
                    default -> token = Token.FIELD;
                }
                break;
            } else if (ch == '{') {
                ch = environment.read();
                token = Token.LBRACE;
                break;
            } else if (ch == '}') {
                ch = environment.read();
                token = Token.RBRACE;
                break;
            } else if (ch == ',') {
                ch = environment.read();
                token = Token.COMMA;
                break;
            } else if (ch == ';') {
                ch = environment.read();
                token = Token.SEMICOLON;
                break;
            } else if (ch == ':') {
                ch = environment.read();
                token = Token.COLON;
                break;
            } else if (ch == '=') {
                if ((ch = environment.read()) == '=') {
                    ch = environment.read();
                    token = Token.EQ;
                    break;
                }
                token = Token.ASSIGN;
                break;
            } else if (ch == '\u001a') {// Our one concession to DOS.
                if ((ch = environment.read()) == EOF) {
                    token = Token.EOF;
                    break;
                }
                environment.warning(prevPos, "warn.funny.char", ch);
                ch = environment.read();
            } else if (ch == '#') {
                int c = environment.lookForward();
                if (c == '{') {
                    // '#' char denotes a "paramMethod name" token
                    ch = environment.read();
                    token = Token.PARAM_NAME;
                    break loop;
                }
                // otherwise, it is a normal cpref
                scanCPRef();
                break loop;
            } else if (ch == '\\') {
                ch = environment.read();
                if (ch == 'u') {
                    ch = environment.convertUnicode();
                    if (Character.isLetterOrDigit(ch) && !Character.isDigit(ch)) {
                        scanIdentifier(null);
                        break;
                    }
                } else if (escapingAllowed.test(ch)) {
                    scanIdentifier(new char[]{'\\', (char) ch});
                    break;
                }
//                    if ((ch = in.read()) == 'u') {
//                        ch = in.convertUnicode();
//                        if (isUCLetter(ch)) {
//                            scanIdentifier();
//                            break loop;
//                        }
//                    }

                environment.traceln("Funny char with code=" + ch + " at: " +
                        environment.lineNumber(pos) + "/" + (pos & ((1 << OFFSET_BITS) - 1)));
                environment.warning(pos, "warn.funny.char", ch);
                ch = environment.read();
            } else {
                environment.traceln("Funny char with code=" + ch + " at: " +
                        environment.lineNumber(pos) + "/" + (pos & ((1 << OFFSET_BITS) - 1)));
                environment.warning(pos, "warn.funny.char", ch);
                ch = environment.read();
            }
        }
    }

    protected void debugScan(String dbstr) {
        if (token == null) {
            environment.traceln(dbstr + "<<<NULL TOKEN>>>");
            return;
        }
        environment.trace(dbstr + token);
        switch (token) {
            case IDENT -> environment.traceln(" = '" + stringValue + "' {idValue = '" + idValue + "'}");
            case STRINGVAL -> environment.traceln(" = {stringValue}: \"" + stringValue + "\"");
            case INTVAL -> environment.traceln(" = {intValue}: " + intValue);
            case FLOATVAL -> environment.traceln(" = {floatValue}: " + floatValue);
            case DOUBLEVAL -> environment.traceln(" = {doubleValue}: " + doubleValue);
            default -> environment.traceln("");
        }
    }
}
