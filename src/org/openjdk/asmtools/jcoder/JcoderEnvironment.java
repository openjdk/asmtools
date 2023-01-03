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
package org.openjdk.asmtools.jcoder;

import org.openjdk.asmtools.common.*;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static org.openjdk.asmtools.common.CompilerConstants.OFFSETBITS;

public class JcoderEnvironment extends Environment<CompilerLogger> {

    InputFile inputFile;

    /**
     * @param builder the jcoder environment builder
     */
    private JcoderEnvironment(Builder<JcoderEnvironment, CompilerLogger> builder) {
        super(builder);
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
        return inputFile == null ? 0 : inputFile.pos;
    }

    public int read() throws IOException {
        return inputFile.read();
    }

    public void close() {
        try {
            inputFile.close();
        } catch (IOException ioe) {
            printException(ioe);
        }
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

    class InputFile {
        // The increment for each character.
        static final int OFFSETINC = 1;
        // The increment for each line.
        static final int LINEINC = 1 << OFFSETBITS;
        InputStream in;
        int pos;
        private int chPos;
        private int pushBack = -1;

        InputFile(DataInputStream dataInputStream) throws IOException {
            this.in = new BufferedInputStream(dataInputStream);
            chPos = LINEINC;
        }

        public void close() throws IOException {
            in.close();
        }

        public int read() throws IOException {
            pos = chPos;
            chPos += OFFSETINC;

            int c = pushBack;
            if (c == -1) {
                c = in.read();
            } else {
                pushBack = -1;
            }

            // parse special characters
            switch (c) {
                case -2:
                    // -2 is a special code indicating a pushback of a backslash that
                    // definitely isn't the start of a unicode sequence.
                    return '\\';

                case '\\':
                    if ((c = in.read()) != 'u') {
                        pushBack = (c == '\\' ? -2 : c);
                        return '\\';
                    }
                    // we have a unicode sequence
                    chPos += OFFSETINC;
                    while ((c = in.read()) == 'u') {
                        chPos += OFFSETINC;
                    }

                    // unicode escape sequence
                    int d = 0;
                    for (int i = 0; i < 4; i++, chPos += OFFSETINC, c = in.read()) {
                        switch (c) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> d = (d << 4) + c - '0';
                            case 'a', 'b', 'c', 'd', 'e', 'f' -> d = (d << 4) + 10 + c - 'a';
                            case 'A', 'B', 'C', 'D', 'E', 'F' -> d = (d << 4) + 10 + c - 'A';
                            default -> {
                                error(pos, "invalid.escape.char");
                                pushBack = c;
                                return d;
                            }
                        }
                    }
                    pushBack = c;
                    return d;

                case '\n':
                    chPos += LINEINC;
                    return '\n';

                case '\r':
                    if ((c = in.read()) != '\n') {
                        pushBack = c;
                    } else {
                        chPos += OFFSETINC;
                    }
                    chPos += LINEINC;
                    return '\n';

                default:
                    return c;
            }
        }
    }
}
