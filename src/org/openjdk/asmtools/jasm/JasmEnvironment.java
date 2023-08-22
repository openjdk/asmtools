/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.common.inputs.TextInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.openjdk.asmtools.common.CompilerConstants.EOF;
import static org.openjdk.asmtools.common.CompilerConstants.OFFSET_BITS;

public class JasmEnvironment extends Environment<CompilerLogger> {

    InputFile inputFile;

    /**
     * @param builder the jasm environment builder
     */
    private JasmEnvironment(Builder<JasmEnvironment, CompilerLogger> builder) {
        super(builder);
    }

    @Override
    public void setInputFile(ToolInput inputFileName) throws IOException, URISyntaxException {
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
    public void warning(int where, String id, Object... args) {
        getLogger().warning(where, id, args);
    }

    @Override
    public void error(int where, String id, Object... args) {
        getLogger().error(where, id, args);
    }

    @Override
    public void warning(String id, Object... args) {
        getLogger().warning(NOWHERE, id, args);
    }

    @Override
    public void error(String id, Object... args) {
        getLogger().error(NOWHERE, id, args);
    }

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
     *
     * @param id   id of a string resource in I18NResourceBundle
     * @param args arguments referenced by the format specifiers in the resource string
     * @throws Error exception
     */
    public void throwErrorException(String id, Object... args) throws Error {
        error(id, args);
        throw new Error();
    }

    /**
     * Throws an error that is associated with scanner position in an input file
     *
     * @param where position in an input file
     * @param id    id of a string resource in I18NResourceBundle
     * @param args  arguments referenced by the format specifiers in the resource string
     * @throws Error exception
     */
    public void throwErrorException(int where, String id, Object... args) throws Error {
        error(where, id, args);
        throw new Error();
    }


    /**
     * @param printTotals whether to print the total line: N warning(s), K error(s)
     * @return 0 if there are no errors otherwise a count of errors
     */
    public int flush(boolean printTotals) {
        return super.getLogger().flush(printTotals);
    }

    public int getPosition() {
        return inputFile == null ? 0 : inputFile.position;
    }

    public int read() {
        return inputFile.readUTF();
    }

    public int lookForward() {
        return inputFile.lookForwardUTF();
    }

    public int convertUnicode() {
        return inputFile.convertUnicodeUTF();
    }

    static class JasmBuilder extends Environment.Builder<JasmEnvironment, CompilerLogger> {

        public JasmBuilder(ToolOutput toolOutput, DualStreamToolOutput logger) {
            super(toolOutput, new CompilerLogger("jasm", JasmEnvironment.class, logger));
        }

        @Override
        public JasmEnvironment build() {
            return new JasmEnvironment(this);
        }
    }

    private class InputFile extends TextInput {
        private int linepos = 1;

        InputFile(DataInputStream dataInputStream) throws IOException {
            super(dataInputStream);
            charPos = 0;
        }

        int lookForwardUTF() {
            try {
                return strData.charAt(charPos);
            } catch (StringIndexOutOfBoundsException e) {
                return EOF;
            }
        }

        @Override
        public int readUTF() {
            char ch;
            position = (linepos << OFFSET_BITS) | charPos;
            try {
                ch = strData.charAt(charPos);
                charPos++;
                // parse special characters
                switch (ch) {
                    case '\n' -> linepos++;
                    case '\r' -> {
                        if (strData.charAt(charPos) == '\n') {
                            charPos++;
                        }
                        linepos++;
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
                return EOF;
            }
            return ch;
        }

        int convertUnicodeUTF() {
            char ch;
            try {
                while ((ch = strData.charAt(charPos)) == 'u') {
                    charPos++;
                }
                int d = 0;
                for (int i = 0; i < 4; i++) {
                    switch (ch) {
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> d = (d << 4) + ch - '0';
                        case 'a', 'b', 'c', 'd', 'e', 'f' -> d = (d << 4) + 10 + ch - 'a';
                        case 'A', 'B', 'C', 'D', 'E', 'F' -> d = (d << 4) + 10 + ch - 'A';
                        default -> {
                            error(position, "invalid.escape.char");
                            return d;
                        }
                    }
                    ch = strData.charAt(++charPos);
                }
                return d;
            } catch (StringIndexOutOfBoundsException e) {
                error(position, "err.invalid.escape.char");
                return EOF;
            }
        }
    }
}
