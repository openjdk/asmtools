/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.CompilerLogger;
import org.openjdk.asmtools.common.EMessageKind;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.util.I18NResourceBundle;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import static org.openjdk.asmtools.common.CompilerConstants.EOF;
import static org.openjdk.asmtools.common.CompilerConstants.OFFSETBITS;

public class JasmEnvironment extends Environment<CompilerLogger>  {

    InputFile inputFile;

    private JasmEnvironment(Builder<JasmEnvironment, CompilerLogger> builder, I18NResourceBundle i18n) {
        super(builder, i18n);
    }

    @Override
    public void setInputFile(String inputFileName) throws IOException, URISyntaxException {
        try {
            // content of the jasm input file
            super.setInputFile(inputFileName);
            this.inputFile = new InputFile(getDataInputStream());
        } catch (IOException ioe) {
            error("err.cannot.read", inputFileName);
            throw ioe;
        }
    }

    // proxy methods
    @Override
    public void warning(int where, String id, Object... args) { getLogger().warning(where, id, args); }
    @Override
    public void error(int where, String id, Object... args) { getLogger().error(where, id, args); }

    @Override
    public void warning(String id, Object... args) { getLogger().warning(NOWHERE, id, args); }

    @Override
    public void error(String id, Object... args) { getLogger().error(NOWHERE, id, args); }


    // Jasm specific methods
    public long getErrorCount() {
        return getLogger().getCount(EMessageKind.ERROR);
    }

    public boolean hasMessages() {
        return !super.getLogger().noMessages();
    }

    // get line number by scanner position
    public int lineNumber(int where) {
        return getLogger().lineNumber(where);
    }

    /**
     * Throws an error that is not associated with scanner position in an input file
     * @param id id of a string resource in I18NResourceBundle
     * @param args arguments referenced by the format specifiers in the resource string
     * @throws Error exception
     */
    public void throwErrorException(String id, Object... args) throws Error {
        error(id, args);
        throw new Error();
    }

    /**
     * Throws an error that is associated with scanner position in an input file
     * @param where position in an input file
     * @param id id of a string resource in I18NResourceBundle
     * @param args arguments referenced by the format specifiers in the resource string
     * @throws Error exception
     */
    public void throwErrorException(int where, String id, Object... args) throws Error {
        error(where, id, args);
        throw new Error();
    }


    /**
     * @param printTotals whether to print the total line: N warning(s), K error(s)
     * @return 0 if there are no errors otherwise a numner of errors
     */
    public int flush(boolean printTotals) { return super.getLogger().flush(printTotals); }

    public int getPosition() {
        return inputFile == null ? 0 : inputFile.pos;
    }

    public int read() {
        return inputFile.read();
    }

    public int lookForward() {
        return inputFile.lookForward();
    }

    public int convertUnicode() {
        return inputFile.convertUnicode();
    }

    static class JasmBuilder extends Environment.Builder<JasmEnvironment, CompilerLogger> {

        public JasmBuilder(PrintWriter errorLogger, PrintWriter outputLogger) {
            super("jasm", new CompilerLogger(errorLogger, outputLogger));
        }

        @Override
        public JasmEnvironment build() {
            return new JasmEnvironment(this, I18NResourceBundle.getBundleForClass(this.getClass()) );
        }
    }

    class InputFile {
        private final byte[] data;
        public int pos;
        private int bytepos = 0;
        private int linepos = 1;

        InputFile(DataInputStream dataInputStream) throws IOException {
            data = new byte[dataInputStream.available()];
            dataInputStream.read(data);
            dataInputStream.close();
        }

        int lookForward() {
            try {
                return data[bytepos];
            } catch (ArrayIndexOutOfBoundsException e) {
                return EOF;
            }
        }

        int read() {
            int c;
            pos = (linepos << OFFSETBITS) | bytepos;
            try {
                c = data[bytepos];
            } catch (ArrayIndexOutOfBoundsException e) {
                return EOF;
            }
            bytepos++;
            // parse special characters
            switch (c) {
                case '\n' -> linepos++;
                case '\r' -> {
                    if (lookForward() == '\n') {
                        bytepos++;
                    }
                    linepos++;
                }
            }
            return c;
        }

        int convertUnicode() {
            int c;
            try {
                while ((c = data[bytepos]) == 'u') {
                    bytepos++;
                }
                int d = 0;
                for (int i = 0; i < 4; i++) {
                    switch (c) {
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> d = (d << 4) + c - '0';
                        case 'a', 'b', 'c', 'd', 'e', 'f' -> d = (d << 4) + 10 + c - 'a';
                        case 'A', 'B', 'C', 'D', 'E', 'F' -> d = (d << 4) + 10 + c - 'A';
                        default -> {
                            error(pos, "invalid.escape.char");
                            return d;
                        }
                    }
                    ++bytepos;
                    c = data[bytepos];
                }
                return d;
            } catch (ArrayIndexOutOfBoundsException e) {
                error(pos, "invalid.escape.char");
                return EOF;
            }
        }
    }
}
