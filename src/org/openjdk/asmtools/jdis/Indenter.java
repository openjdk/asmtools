/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.asmtools.jasm.TableFormatModel;

import java.io.IOException;
import java.util.*;

import static java.lang.System.lineSeparator;
import static org.openjdk.asmtools.jdis.Options.PrintOption.*;

public abstract class Indenter implements Printable {
    public static final int UNDEFINED = -1;

    public static final int INDENT_STEP = 2;
    public static final int INDENT_OFFSET = 2;
    public static final String INDENT_CHAR = " ";
    public static final String INDENT_STRING = INDENT_CHAR.repeat(INDENT_STEP);

    // Global formatting strings
    public static final String ARGUMENT_DELIMITER = "^";
    public static final String LINE_SPLITTER = "⾀";
    public static final String NO_BSM_ARGUMENTS = "{}";
    public static final String NO_BSM_ARGUMENTS_REGEX = "\\{\\}";
    public static final String REPLACEMENT_NO_BSM_ARGUMENTS = "<!>";

    // Global numbers
    public static final int PROGRAM_COUNTER_PLACEHOLDER_LENGTH = 7;
    public static final int INSTR_PREFIX_LENGTH = 7;
    public static final int STACKMAP_TYPE_PLACEHOLDER_LENGTH = 18;
    public static final int OPERAND_PLACEHOLDER_LENGTH = 17;
    public static int TABLE_PADDING = OPERAND_PLACEHOLDER_LENGTH + INSTR_PREFIX_LENGTH + 1;
    public static final int COMMENT_PADDING = (Options.contains(TABLE)) ? 16 : 20;
    public static final int INITIAL_COMMENT_OFFSET = 0;     // Initial offset that will be dynamically updated

    public static final int CIRCULAR_COMMENT_OFFSET = 25;

    protected final boolean tableFormat = Options.contains(TABLE);
    protected final boolean sysInfo = Options.contains(SYSINFO);
    protected final boolean detailedOutput = Options.contains(DETAILED_Output);
    protected final boolean extraDetailedOutput = Options.contains(EXTRA_DETAILED_Output);
    protected final boolean bestEffort = Options.contains(BEST_EFFORT);

    // Discard printing attributes
    protected final boolean dropSourceFile = Options.contains(DROP_Source);
    protected final boolean dropClasses = Options.contains(DROP_Classes);
    protected final boolean dropSignatures = Options.contains(DROP_Signatures);
    protected final boolean dropCharacterRange = Options.contains(DROP_CharacterRange);

    // Extra printing instruction
    protected final boolean printLocalVariables = Options.contains(LOCAL_VARIABLE_All, LOCAL_VARIABLE_Vars);
    protected final boolean printLocalVariableTypes = Options.contains(LOCAL_VARIABLE_All, LOCAL_VARIABLE_Types);
    protected final boolean printLineNumber = Options.contains(LINE_NUMBER_TABLE_Numbers, LINE_NUMBER_TABLE_Lines, LINE_NUMBER_TABLE_Table, TABLE);

    // internal references
    protected final boolean printCPIndex = Options.contains(CP_INDEX);
    protected final boolean skipComments = Options.contains(NO_COMMENTS);
    protected final boolean printProgramCounter = Options.contains(PRINT_BCI);
    protected final boolean printLabelAsIdentifiers = Options.contains(LABELS) && !printProgramCounter;
    protected final boolean printConstantPool = Options.contains(CONSTANT_POOL);
    protected final boolean printSourceLines = Options.contains(LINE_NUMBER_TABLE_Lines);
    protected final boolean printLineTable = Options.contains(LINE_NUMBER_TABLE_Table);
    protected final boolean printLineTableLines = Options.contains(LINE_NUMBER_TABLE_Lines);
    protected final boolean printLineTableNumbers = Options.contains(LINE_NUMBER_TABLE_Numbers);
    protected final boolean printHEX = Options.contains(HEX);
    //
    protected boolean printable = true;
    // indicated that an entity has a size in a collection
    protected boolean hasSize = false;
    protected TableFormatModel.Token tableToken = TableFormatModel.Token.NOT_SUPPORTED;
    //
    // the maximum size of the same elements in the collection to calculate printing bounds
    // applies only to entities that implement Measurable interface.
    protected int maxSize = 0;
    protected boolean maxSizeCalculated = false;
    //
    protected ToolOutput toolOutput;
    //
    private final String LabelPrefix = printLabelAsIdentifiers ? "L" : "";
    //
    protected int commentOffset = INITIAL_COMMENT_OFFSET;
    private int length, offset, step;
    private String fillString;

    /**
     * If a table format is supported and the tool option TABLE is set, prints an object as a table entry.
     */
    public void print() throws IOException {
        if (isTableOutput()) {
            tablePrint();
        } else {
            jasmPrint();
        }
    }

    public boolean isTableOutput() {
        return this.tableFormatSupported() && tableFormat;
    }

    @Override
    public boolean tableFormatSupported() {
        return tableToken.isExtendedPrintingSupported();
    }

    protected String getTitle() {
        throw new NotImplementedException(this.getClass().getName());
    }

    protected void tablePrint() throws IOException {
        throw new NotImplementedException(this.getClass().getName());
    }

    protected void jasmPrint() throws IOException {
        throw new NotImplementedException(this.getClass().getName());
    }

    protected void tablePrint(int index, int size) throws IOException {
        throw new NotImplementedException(this.getClass().getName());
    }

    protected void jasmPrint(int index, int size) throws IOException {
        throw new NotImplementedException(this.getClass().getName());
    }

    public void print(int index, int size) throws IOException {
        if (isTableOutput()) {
            tablePrint(index, size);
        } else {
            jasmPrint(index, size);
        }
    }


    public Indenter(ToolOutput toolOutput) {
        this();
        this.toolOutput = toolOutput;
    }

    protected Indenter() {
        this.length = 0;
        this.step = INDENT_STEP;
        this.offset = INDENT_OFFSET;
        this.fillString = INDENT_CHAR;
    }

    protected String getLabelPrefix() {
        return LabelPrefix;
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
        String str = Indent(s);
        toolOutput.prints(str);
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

    public Indenter printIndentPadLeft(String str, int totalWidth) {
        toolOutput.prints(IndentPadLeft(str, totalWidth));
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

    public Indenter println(boolean isPrint) {
        if (isPrint) {
            toolOutput.printlns("");
        }
        return this;
    }

    public Indenter println(String format, Object... args) {
        toolOutput.printlns(new Formatter().format(format, args).toString());
        return this;
    }

    public Indenter incIndent() {
        return incIndent(1);
    }

    public Indenter decIndent() {
        return decIndent(1);
    }

    public Indenter incIndent(int count) {
        length += step * count;
        return this;
    }

    public Indenter decIndent(int count) {
        length -= step * count;
        if (length < 0) {
            length = 0;
        }
        return this;
    }

    public Indenter setTheSame(Indenter that) {
        this.length = that.length;
        this.offset = that.offset;
        this.step = that.step;
        this.commentOffset = that.commentOffset;
        this.fillString = that.fillString;
        return this;
    }

    public Indenter initIndent(int initialOffset) {
        this.length = 0;
        this.step = INDENT_STEP;
        this.offset = initialOffset;
        this.fillString = INDENT_CHAR;
        return this;
    }

    public int getIndentSize() {
        return this.offset + this.length;
    }

    public String nCopies(int n) {
        // create a string made up of n copies of string fillString
        return String.join("", Collections.nCopies(n < 0 ? 0 : n, this.fillString));
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

    /**
     * Calculates offsets for Class's attributes:
     * <p>
     * 12AAAAAAAAAAAAAAAAAAAAAAAAAAACCCCCCCCCCCCCCCCC
     * SourceFile                 #126;             // TestMethods0.java
     * 12       - Indent
     * AAA.A   - getPrintAttributeKeyPadding()
     * CCC.C   - getPrintAttributeCommentPadding()
     *
     * @return
     */
    protected int getPrintAttributeKeyPadding() {
        int instructionOffset = (printProgramCounter) ? PROGRAM_COUNTER_PLACEHOLDER_LENGTH : INSTR_PREFIX_LENGTH;
        int attributeOffset = (printProgramCounter) ? instructionOffset : (instructionOffset - getIndentStep());
        return instructionOffset + attributeOffset + OPERAND_PLACEHOLDER_LENGTH - getIndentSize() * 2;
    }

    protected int getPrintAttributeCommentPadding() {
        return getCommentOffset() - getPrintAttributeKeyPadding();
    }

    public Indenter setHasSize(boolean hasSize) {
        this.hasSize = hasSize;
        return this;
    }

    protected final static Map<Integer, List<Integer>> InvokeDynamicBreakPositions = Map.of(
            0, List.of(2, 3),
            1, List.of(3),
            2, List.of(3));
    protected final static Map<Integer, List<Integer>> LdwBreakPositions = Map.of(
            0, List.of(3),
            1, List.of(3),
            2, List.of(3));
    protected final static Map<Integer, List<Integer>> BootstrapMethodBreakPositions = Map.of(0, List.of(2, 3));
    protected final static Map<Integer, List<Integer>> BootstrapArgumentsBreakPositions = Map.of(0, List.of(3));

    /**
     * Formats invokedynamic/ldc dynamic operand line and Bootstrap arguments
     *
     * @param str            non-formatted operand line
     * @param offset         indent for new lines
     * @param prefix         prefix placed upfront new lines
     * @param breakPositions numbers where after ":" a lineSeparator is added to wrap a very long operand lines
     * @return formatted operand line
     */
    protected String formatOperandLine(String str, int offset, String prefix, Map<Integer, List<Integer>> breakPositions) {
        boolean noArgs = str.contains(NO_BSM_ARGUMENTS);
        if (noArgs) {
            str = str.replaceAll(NO_BSM_ARGUMENTS_REGEX, REPLACEMENT_NO_BSM_ARGUMENTS);
        }
        StringTokenizer st = new StringTokenizer(str, ":\"{}\\" + ARGUMENT_DELIMITER + LINE_SPLITTER, true);
        StringBuilder sb = new StringBuilder(80);
        boolean processTokens = true;
        String prevToken = "";
        int nItems = 0, nLevel = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            List<Integer> breaks = breakPositions.getOrDefault(nLevel, Collections.emptyList());
            switch (token) {
                case ":":
                    sb.append(token);
                    if (processTokens) {
                        nItems++;
                        if (breaks.contains(nItems)) {
                            sb.append(lineSeparator()).append(nCopies(offset)).append(prefix).
                                    append(nCopies(getIndentStep() * nLevel));
                        }
                    }
                    break;
                case "}":
                    if (processTokens) {
                        nLevel = (nLevel == 0) ? nLevel : nLevel - 1;
                        nItems = 0;
                        sb.append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel)).append(token);
                    } else
                        sb.append(token);
                    break;
                case "{":
                    if (processTokens) {
                        nLevel++;
                        nItems = 0;
                        sb.append(" {").append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                    } else {
                        sb.append(token);
                    }
                    break;
                case "\"":
                    if (!prevToken.equals("\\"))
                        processTokens = !processTokens;
                    sb.append(token);
                    break;
                case ARGUMENT_DELIMITER:
                    if (processTokens) {
                        sb.append(',').append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                        nItems = 0;
                    } else {
                        sb.append(ARGUMENT_DELIMITER);
                    }
                    break;
                case LINE_SPLITTER:
                    if (processTokens) {
                        sb.append(lineSeparator()).append(nCopies(offset)).
                                append(prefix).append(nCopies(getIndentStep() * nLevel));
                    } else {
                        sb.append(ARGUMENT_DELIMITER);
                    }
                    break;
                default:
                    sb.append(token);
                    break;
            }
            prevToken = token;
        }
        str = sb.toString();
        if (noArgs) {
            str = str.replaceAll(REPLACEMENT_NO_BSM_ARGUMENTS, NO_BSM_ARGUMENTS);
        }
        return str;
    }

    public static class NotImplementedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NotImplementedException(String where) {
            super("".formatted(where));
        }
    }
}
