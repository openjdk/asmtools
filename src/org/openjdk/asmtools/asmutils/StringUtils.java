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
package org.openjdk.asmtools.asmutils;

import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.openjdk.asmtools.Main.sharedI18n;

/**
 * Utility class for string operations related to ASM tools.
 * <p>
 * This class provides methods for removing comments from strings,
 * converting UTF-8 strings to printable formats, and handling character
 * encoding in a way that is suitable for assembly tools.
 */
public class StringUtils {

    public static final char[] hexTable = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * Removes Java comments from String
     * Spaces ahead of comments will be removed; alternatively, if no spaces precede the comment,
     * the space after it will be deleted.
     *
     * @param str string that contains comments
     * @return string without comments
     */
    public static String removeCommentFrom(String str) {
        // firstly delete // comment if exists
        int idx = str.indexOf("//");
        if (idx != -1) {
            str = str.substring(0, idx--);
            if (str.charAt(idx) == ' ') {
                str = str.substring(0, idx);
            }
        }
        // remove /* some comment */ comments
        String[] list = str.split("\\/\\*.*?\\*\\/");
        if (list.length > 1) {
            // comments found
            str = "";
            for (int i = 0; i < list.length; i++) {
                idx = list[i].length() - 1;
                if (list[i].charAt(idx) == ' ') {
                    str += list[i].substring(0, idx);
                } else {
                    str += list[i];
                    idx = i + 1;
                    if (idx < list.length && list[idx].charAt(0) == ' ') {
                        list[idx] = list[idx].substring(1);
                    }
                }
            }
        }
        return str;
    }

    /**
     * Converts CONSTANT_Utf8_info string to a printable string for jdis/jdec.
     *
     * @param utf8            UTF8 string taken from within ConstantPool of a class file
     * @param enclosingString strings to enclose output string
     * @return output string for jcod/jasm
     */
    public static String Utf8ToString(String utf8, String... enclosingString) {
        final String leftBracket = enclosingString.length > 0 ? enclosingString[0] : "";
        final String rightBracket = enclosingString.length > 1 ? enclosingString[1] : leftBracket;
        StringBuilder sb = new StringBuilder(leftBracket);
        for (int k = 0; k < utf8.length(); k++) {
            sb.append(charToUtf8String(utf8.charAt(k)));
        }
        return sb.append(rightBracket).toString();
    }

    /**
     * Converts CONSTANT_Utf8_info string to a printable list of strings
     * @param utf8      UTF8 string taken from within ConstantPool of a class file
     * @param strLength the maximum length of each string in the resulting list
     * @return list of strings for jcod/jasm
     */
    public static List<String> Utf8ToStringList(String utf8, int strLength) {
        List<String> list = new ArrayList<>();
        String s = "";
        for (int k = 0; k < utf8.length(); k++) {
            if (s.length() >= strLength) {
                list.add(s);
                s = "";
            }
            s += charToUtf8String(utf8.charAt(k));
        }
        if (!s.isEmpty()) {
            list.add(s);
        }
        return list;
    }

    /**
     * Converts a character to its UTF-8 string representation.
     * <p>
     * Control characters and special characters are escaped using the appropriate escape sequences.
     * Other characters are returned as is.
     *
     * @param c the character to convert
     * @return the UTF-8 string representation of the character
     */
    public static String charToUtf8String(char c) {
        return switch (c) {
            case '\t' -> "\\".concat("t");
            case '\n' -> "\\".concat("n");
            case '\r' -> "\\".concat("r");
            case '\b' -> "\\".concat("b");
            case '\f' -> "\\".concat("f");
            case '\"' -> "\\".concat("\"");
            case '\'' -> "\\".concat("\'");
            case '\\' -> "\\".concat("\\");
            default -> Character.isISOControl(c) ? String.format("\\u%04x", (int) c) : Character.toString(c);
        };
    }


    /**
     * Checks that ch is in the list
     *
     * @param i    char for testing
     * @param list of chars
     * @return true if char ch found in the list
     */
    public static boolean isOneOf(int i, char... list) {
        char ch = (char) i;
        for (char c : list) {
            if (c == ch)
                return true;
        }
        return false;
    }

    /**
     * Reads the set of bytes if all bytes are printable then they will be printed
     * as the string "String"; otherwise the byte array 0x0F 0xB6 0x00 0x11;
     * The result is the list of lines for printing.
     *
     * @param in            input stream to get bytes for printing
     * @param length        number of bytes
     * @param CHARS_IN_LINE max chars in line prepared for printing
     * @return list of lines for printing
     * @throws IOException exception might happen while reading DataInputStream
     */
    public static List<String> readUtf8String(DataInputStream in, int length, int CHARS_IN_LINE) throws IOException {
        final int BYTES_IN_LINE = CHARS_IN_LINE / 6 + 1;
        final List<String> list = new ArrayList<>();
        List<StringBuilder> byteLines = new ArrayList() {{
            add(new StringBuilder());
        }};
        StringBuilder sb = byteLines.get(0);
        byte[] buffer;
        String utfString = null;
        int count = 0;
        try {
            buffer = new byte[length + 2];
            buffer[0] = (byte) (length >> 8);
            buffer[1] = (byte) length;
            for (int i = 0; i < length; i++) {
                byte b = in.readByte();
                buffer[i + 2] = b;
                count++;
                sb.append("0x").append(hexTable[(b >> 4) & 0xF]).append(hexTable[b & 0xF]);
                if (i % BYTES_IN_LINE == BYTES_IN_LINE - 1) {
                    byteLines.add(sb = new StringBuilder());
                } else if (i + 1 != length) {
                    sb.append(" ");
                }
            }
            if (count > 0) {
                try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer))) {
                    utfString = DataInputStream.readUTF(dis);
                } catch (UTFDataFormatException utfDataFormatException) {
                    byteLines.add(0, new StringBuilder(format("// == %s ==", sharedI18n.getString("main.error.wrong.utf8"))));
                } catch (IOException ignored) {
                    /*ignored*/
                }
            }
        } catch (NegativeArraySizeException negativeArraySizeException) {
            throw new IOException("Requested array size exceeds VM limit", negativeArraySizeException);
        } finally {
            if (count > 0) {
                List<String> utf8Lines = getPrintable(utfString, CHARS_IN_LINE);
                if (utf8Lines != null) {
                    utf8Lines.stream().
                            forEach(s -> list.add(s.startsWith("// ==") ? s : format("\"%s\";", s)));
                } else {
                    byteLines.stream().map(s -> s.toString()).
                            forEach(s -> list.add(s.startsWith("// ==") ? s : format("%s;", s)));
                }
            }
        }
        return list;
    }

    /**
     * @param buffer array of bytes
     * @return null if buffer contains at least one of non-printable bytes
     * otherwise the list of strings encoded in the buffer
     */
    public static List<String> getPrintable(byte[] buffer, final int CHARS_IN_LINE) {
        List<String> list = null;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer))) {
            String str = dis.readUTF();
            list = getPrintable(str, CHARS_IN_LINE);
        } catch (IOException e) {/*ignore*/}
        return list;
    }

    /**
     * @param rawString String
     * @return null if buffer contains at least one of non-printable bytes
     * otherwise the list of strings split by CHARS_IN_LINE
     */
    public static List<String> getPrintable(String rawString, final int CHARS_IN_LINE) {
        List<String> list = null;
        if (rawString != null) {
            String cleanedString = removeNonPrintableAscii(rawString);
            if (cleanedString != null && !cleanedString.isEmpty() && !cleanedString.chars().anyMatch(c -> !isPrintableChar((char) c))) {
                list = Utf8ToStringList(rawString, CHARS_IN_LINE);
            }
        }
        return list;
    }

    /**
     * Removes non-printable ASCII characters from the given input string.
     * <p>
     * Non-printable ASCII characters are those with ASCII values outside the range of 32 (space) to 126 (tilde).
     *
     * @param input the input string to be processed
     * @return a new string with non-printable ASCII characters removed, or null if the input is null
     */
    public static String removeNonPrintableAscii(String input) {
        if (input == null) return null;
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 32 && c <= 126) {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts a list of strings to a formatted string representation.
     * The output is enclosed in parentheses and elements are separated by commas.
     *
     * @param list the list of strings to convert
     * @return a formatted string representation of the list
     */
    public static String ListToString(List<String> list) {
        return ListToString(list, "(,)");
    }

    /**
     * Converts a list of strings to a formatted string representation.
     * The output is enclosed in square brackets and elements are separated by a specified delimiter.
     *
     * @param list       the list of strings to convert
     * @param separators a string containing the prefix, delimiter, and postfix characters
     *                  (e.g., "[,]"), where:
     *                  - first character is the prefix,
     *                  - second character is the delimiter,
     *                  - third character is the postfix.
     * @return a formatted string representation of the list
     */
    public static String ListToString(List<String> list, String separators) {
        int l = separators.length();
        String prefix = separators != null && l > 0 ? separators.substring(0, 1) : "[";
        String delim = separators != null && l > 1 ? separators.substring(1, 2) : ",";
        String postfix = separators != null && l > 2 ? separators.substring(2, 3) : "]";
        return prefix.concat(" ") + String.join(delim.concat(" "), list) + " ".concat(postfix);
    }

    /**
     * Converts an array of integers to a hex string representation.
     * Each integer is converted to its hexadecimal format and joined with commas.
     *
     * @param array the array of integers to convert
     * @return a string representation of the array in hexadecimal format
     */
    public static String mapToHexString(int[] array) {
        return format("{%s}",
                Arrays.stream(array).mapToObj(HexUtils::toHex).collect(Collectors.joining(", ")));
    }

    public static String repeat(String str, int count) {
        return count <= 0 ? "" : new String(new char[count]).replace("\0", str);
    }

    /**
     * Checks if the character is printable.
     * A character is considered printable if it is not a control character,
     * is not undefined, and belongs to a valid Unicode block that is not SPECIALS.
     *
     * @param c the character to check
     * @return true if the character is printable, false otherwise
     */
    public static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    public static BiFunction<String, List<String>, Boolean> endWith = (str, list) -> {
        for (String suffix : list) {
            if (str.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    };

    public static BiFunction<String, List<String>, Boolean> contains = (str, list) -> {
        for (String substr : list) {
            if (str.contains(substr)) {
                return true;
            }
        }
        return false;
    };
}
