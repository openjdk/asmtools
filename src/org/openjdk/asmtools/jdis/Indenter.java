/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.jdis;

import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.function.Supplier;

import static org.openjdk.asmtools.jdis.Options.PR.*;

public class Indenter implements Printable {

    public static final int INDENT_STEP = 2;
    public static final int INDENT_OFFSET = 2;
    public static final String INDENT_STRING =  " ";

    // Global formatting strings
    public static final String ARGUMENT_DELIMITER = "^";
    public static final String LINE_SPLITTER = "⾀";

    // Global numbers
    public static final int PROGRAM_COUNTER_PLACEHOLDER_LENGTH = 7;
    public static final int INSTR_PREFIX_LENGTH = 7;
    public static final int STACKMAP_TYPE_PLACEHOLDER_LENGTH = 17;
    public static final int OPERAND_PLACEHOLDER_LENGTH = 17;
    public static final int COMMENT_PADDING = 16;
    public static final int COMMENT_OFFSET = 0;     // Initial offset that will be dynamically updated

    // internal references
    protected final boolean printCPIndex = Options.contains(CPX);
    protected final boolean skipComments = Options.contains(NC);
    protected final boolean printProgramCounter = Options.contains(PC);
    protected final boolean printLabelAsIdentifiers = Options.contains(LABS) && !printProgramCounter;
    protected final boolean printConstantPool = Options.contains(CP);
    protected final boolean printSourceLines = Options.contains(SRC);
    protected final boolean printLocalVars = Options.contains(VAR);
    protected final boolean printLineTable = Options.contains(LNT);
    protected final boolean printHEX = Options.contains(HEX);
    //
    protected ToolOutput toolOutput;
    //
    private int commentOffset = COMMENT_OFFSET;
    private int length, offset, step;
    private String fillString;

    public void print() throws IOException {
        throw new RuntimeException("not yet implemented");
    }

    public Indenter(ToolOutput toolOutput) {
        this();
        this.toolOutput = toolOutput;
    }

    protected Indenter() {
        this.length = 0;
        this.step = INDENT_STEP;
        this.offset = INDENT_OFFSET;
        this.fillString = INDENT_STRING;
    }

    public Indenter printIndentLn(String s) {
        toolOutput.printlns(Indent(s));
        return this;
    }

    public Indenter printIndentLn() {
        toolOutput.printlns("");
        return this;
    }

    public int getIndentStep() {
        return this.step;
    }

    public Indenter printIndentLn(String format, Object... args) {
        toolOutput.printlns(Indent(new Formatter().format(format, args).toString()));
        return this;
    }

    public Indenter printIndent(String format, Object... args) {
        toolOutput.prints(Indent(new Formatter().format(format, args).toString()));
        return this;
    }

    public Indenter printIndent(String s) {
        toolOutput.prints(Indent(s));
        return this;
    }

    public Indenter printIndent() {
        toolOutput.prints(getIndentString());
        return this;
    }

    public Indenter printPadRight(String s, int totalWidth) {
        toolOutput.prints(PadRight(s, totalWidth));
        return this;
    }

    public Indenter printPadLeft(String s, int totalWidth) {
        toolOutput.prints(PadLeft(s, totalWidth));
        return this;
    }

    public Indenter printIndentPadRight(String str, int totalWidth) {
        toolOutput.prints(IndentPadRight(str, totalWidth));
        return this;
    }

    public Indenter print(String s) {
        toolOutput.prints(s);
        return this;
    }

    public Indenter print(String format, Object... args) {
        toolOutput.prints(new Formatter().format(format, args).toString());
        return this;
    }

    public Indenter println(String s) {
        toolOutput.printlns(s);
        return this;
    }

    public Indenter println() {
        toolOutput.printlns("");
        return this;
    }

    public Indenter println(Supplier<Boolean> isPrint) {
        if(isPrint.get()) {
            toolOutput.printlns("");
        }
        return this;
    }


    public Indenter println(String format, Object... args) {
        toolOutput.printlns(new Formatter().format(format, args).toString());
        return this;
    }

    public Indenter incIndent() {
        length += step;
        return this;
    }

    public Indenter decIndent() {
        length -= step;
        if (length < 0) {
            length = 0;
        }
        return this;
    }

    public Indenter setTheSame(Indenter that) {
        this.length = that.length;
        this.offset = that.offset;
        this.step = that.step;
        this.fillString = that.fillString;
        return this;
    }

    public Indenter resetIndent() {
        return initIndent(INDENT_OFFSET);
    }

    public Indenter initIndent(int initialOffset) {
        this.length = 0;
        this.step = INDENT_STEP;
        this.offset = initialOffset;
        this.fillString = INDENT_STRING;
        return this;
    }

    public int getIndentSize() {
        return this.offset + this.length;
    }

    public String nCopies(int n) {
        // create a string made up of n copies of string fillString
        return String.join("", Collections.nCopies(n, this.fillString));
    }

    /**
     * Creates indent string based on current indent size.
     */
    public String getIndentString() {
        return this.nCopies(this.getIndentSize());
    }

    /**
     * Formats input string by adding indent string and padding spaces from the left.
     * "[indent][PaddingSpaces][string]"
     * -----totalWidth-------
     */
    public String IndentPadLeft(String str, int totalWidth) {
        return this.getIndentString() + PadLeft(str, totalWidth);
    }

    /**
     * Formats input string by adding indent string and padding spaces from the left.
     * "[indent][string][PaddingSpaces]"
     * -----totalWidth-------
     */
    public String IndentPadRight(String str, int totalWidth) {
        return this.getIndentString() + PadRight(str, totalWidth);
    }

    /**
     * Formats input string by adding indent string and padding spaces from the left.
     * "[PaddingSpaces][string]"
     * -----totalWidth-------
     */
    public String PadLeft(String str, int totalWidth) {
        if (totalWidth > 0) {
            int count = totalWidth - str.length();
            if (count > 0) {
                str = nCopies(count) + str;
            }
        }
        return str;
    }

    public String PadRight(String str, int totalWidth) {
        if (totalWidth > 0) {
            int count = totalWidth - str.length();
            if (count > 0) {
                str = str + nCopies(count);
            }
        }
        return str;
    }

    public String padRight(String value, int width, char pad) {
        if (value.length() >= width)
            return value;
        char[] buf = new char[width];
        Arrays.fill(buf, value.length(), width, pad);
        value.getChars(0, value.length(), buf, 0);
        return new String(buf);
    }

    public String padLeft(String value, int width, char pad) {
        if (value.length() >= width)
            return value;
        char[] buf = new char[width];
        int padLen = width - value.length();
        Arrays.fill(buf, 0, padLen, pad);
        value.getChars(0, value.length(), buf, padLen);
        return new String(buf);
    }

    public String Indent(String str) {
        return this.getIndentString() + str;
    }

    public String enlargedIndent(String str, int shift) {
        this.offset += shift;
        str = Indent(str);
        this.offset -= shift;
        return str;
    }

    public String enlargedIndent(int shift) {
        return enlargedIndent("", shift);
    }

    /**
     * @return the common offset of comments for printing methods
     */
    public int getCommentOffset() {
        return commentOffset;
    }

    public Indenter setCommentOffset(int commentOffset) {
        this.commentOffset = commentOffset;
        return this;
    }

    /**
     * @return the common offset of the indent
     */
    public int getOffset() {
        return offset;
    }

    public Indenter setOffset(int offset) {
        this.offset = offset;
        return this;
    }
}
