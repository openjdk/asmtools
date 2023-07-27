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
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110EOF301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.asmtools.jcoder;

import org.openjdk.asmtools.common.CompilerLogger;
import org.openjdk.asmtools.common.EMessageKind;
import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.inputs.TextInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ToolOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.structure.CFVersion;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.openjdk.asmtools.common.CompilerConstants.*;

public class JcoderEnvironment extends Environment<CompilerLogger> {

    final CFVersion cfv;
    InputFile inputFile;

    /**
     * @param builder the jcoder environment builder
     */
    private JcoderEnvironment(Builder<JcoderEnvironment, CompilerLogger> builder) {
        super(builder);
        cfv = new CFVersion();
    }

    @Override
    public void setInputFile(ToolInput inputFileName) throws IOException, URISyntaxException {
        try {
            // content of the jcod input file
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

    // Jcoder specific methods
    public long getErrorCount() {
        return getLogger().getCount(EMessageKind.ERROR);
    }

    public boolean hasMessages() {
        return !super.getLogger().noMessages();
    }

    /**
     * @param printTotals whether to print the total line: N warning(s), K error(s)
     * @return 0 if there are no errors otherwise a number of errors
     */
    public int flush(boolean printTotals) {
        return super.getLogger().flush(printTotals);
    }

    public int getPosition() {
        return inputFile == null ? 0 : inputFile.position;
    }

    public int read() throws IOException {
        return inputFile.readUTF();
    }

    static class JcoderBuilder extends Builder<JcoderEnvironment, CompilerLogger> {

        public JcoderBuilder(ToolOutput toolOutput, DualStreamToolOutput log) {
            super(toolOutput, new CompilerLogger("jcoder", JcoderEnvironment.class, log));
        }

        @Override
        public JcoderEnvironment build() {
            return new JcoderEnvironment(this);
        }
    }

    class InputFile extends TextInput {

        private int pushBack = EOF;
        //
        private int strPos = 0;

        InputFile(DataInputStream dataInputStream) throws IOException {
            super(dataInputStream);
            charPos = LINE_INC;
        }

        private int getChar() {
            try {
                return strData.charAt(strPos++);
            } catch (StringIndexOutOfBoundsException e) {
                return EOF;
            }
        }

        @Override
        public int readUTF() {
            position = charPos;
            charPos += OFFSET_INC;
            int c = pushBack;
            if (c == EOF) {
                c = getChar();
            } else {
                pushBack = EOF;
            }

            // parse special characters
            switch (c) {
                case BACKSLASH -> {
                    // BACKSLASH is a special code indicating a pushback of a backslash that
                    // definitely isn't the start of a unicode sequence.
                    return '\\';
                }
                case '\\' -> {
                    if ((c = getChar()) != 'u') {
                        pushBack = (c == '\\' ? BACKSLASH : c);
                        return '\\';
                    }
                    // we have a unicode sequence
                    charPos += OFFSET_INC;
                    while ((c = getChar()) == 'u') {
                        charPos += OFFSET_INC;
                    }

                    // unicode escape sequence
                    int d = 0;
                    for (int i = 0; i < 4; i++, charPos += OFFSET_INC, c = getChar()) {
                        switch (c) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> d = (d << 4) + c - '0';
                            case 'a', 'b', 'c', 'd', 'e', 'f' -> d = (d << 4) + 10 + c - 'a';
                            case 'A', 'B', 'C', 'D', 'E', 'F' -> d = (d << 4) + 10 + c - 'A';
                            default -> {
                                error(position, "err.invalid.escape.char");
                                pushBack = c;
                                return d;
                            }
                        }
                    }
                    pushBack = c;
                    return d;
                }
                case '\n' -> {
                    charPos += LINE_INC;
                    return '\n';
                }
                case '\r' -> {
                    if ((c = getChar()) != '\n') {
                        pushBack = c;
                    } else {
                        charPos += OFFSET_INC;
                    }
                    charPos += LINE_INC;
                    return '\n';
                }
                default -> {
                    return c;
                }
            }
        }
    }
}
