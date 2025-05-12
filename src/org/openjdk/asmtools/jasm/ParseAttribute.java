/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.asmtools.common.SyntaxError;
import org.openjdk.asmtools.common.structure.StackMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.openjdk.asmtools.common.structure.StackMap.EntryType.*;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.*;
import static org.openjdk.asmtools.jasm.StackMapData.UNDEFINED;

public class ParseAttribute extends ParseBase {

    private static final String LocalVariableTable_HEADER = START.parseKey() + LENGTH.parseKey() + SLOT.parseKey() + NAME.parseKey() + DESCRIPTOR.parseKey();
    private static final String LocalVariableTypeTable_HEADER = START.parseKey() + LENGTH.parseKey() + SLOT.parseKey() + NAME.parseKey() + SIGNATURE.parseKey();
    private static final int LocalVariableTable_HEADER_LENGTH = LocalVariableTable_HEADER.length();
    private static final int LocalVariableTypeTable_HEADER_LENGTH = LocalVariableTypeTable_HEADER.length();

    protected ParseAttribute(Parser parentParser) {
        super.init(parentParser);
    }

    /**
     * Parse either localVariableTable or localVariableTypeTable according to the boolean parameter isTypeTable
     *
     * @param isTypeTable defines which localVariableTypeTable or localVariableTable is parsed
     * @return list of local_variable_table[i] or local_variable_type_table[i] entries
     */
    public List<LocalVariableData> parseLocalVariableTable(boolean isTypeTable) {
        String attributeName = isTypeTable ? "LocalVariableTypeTable" : "LocalVariableTable";
        String parseKey = isTypeTable ? SIGNATURE.parseKey() : DESCRIPTOR.parseKey();
        String header = isTypeTable ? LocalVariableTypeTable_HEADER : LocalVariableTable_HEADER;
        int headerLength = isTypeTable ? LocalVariableTypeTable_HEADER_LENGTH : LocalVariableTable_HEADER_LENGTH;

        ArrayList<LocalVariableData> list = new ArrayList<>();
        scanner.scan();
        if (scanner.token == COLON) {
            scanner.scan();
        }
        String buffer = "";
        short slot = -1, start_pc = -1, length = -1;
        ConstCell nameCell = null, descriptorCell = null;

        while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
            switch (scanner.token) {
                case START, LENGTH, SLOT, NAME, DESCRIPTOR, SIGNATURE -> {
                    if (buffer.length() == headerLength || buffer.contains(scanner.stringValue)) {
                        environment.throwErrorException(scanner.pos, "err.header.locvars", parseKey);
                    }
                    buffer += scanner.stringValue;
                    if (!header.startsWith(buffer)) {
                        environment.throwErrorException(scanner.pos, "err.header.locvars", parseKey);
                    }
                }
                case INTVAL -> {
                    if (buffer.length() != headerLength) {
                        environment.throwErrorException(scanner.pos, "err.header.expected.locvars", parseKey);
                    }
                    if (start_pc == -1) {
                        start_pc = (short) scanner.intValue;
                    } else if (length == -1) {
                        length = (short) scanner.intValue;
                    } else if (slot == -1) {
                        slot = (short) scanner.intValue;
                    } else {
                        if (nameCell == null) {
                            environment.throwErrorException(scanner.pos, "err.expected.locvars",
                                    attributeName, "\"" + NAME.parseKey() + "\"");
                        }
                        if (descriptorCell == null) {
                            environment.throwErrorException(scanner.pos, "err.expected.locvars",
                                    attributeName, "\"" + parseKey + "\"");
                        }
                    }
                }
                case STRINGVAL -> {
                    verifyPosition(buffer, headerLength, attributeName, parseKey, start_pc, length, slot);
                    String str = scanner.stringValue;
                    if (nameCell == null) {
                        nameCell = parser.pool.findUTF8Cell(str);
                    } else if (descriptorCell == null) {
                        descriptorCell = parser.pool.findUTF8Cell(str);
                        list.add(new LocalVariableData(start_pc, length, slot, nameCell, descriptorCell));
                        // next new line
                        slot = start_pc = length = -1;
                        nameCell = descriptorCell = null;
                    } else {
                        environment.throwErrorException(scanner.pos, "err.header.expected.locvars", parseKey);
                    }
                }
                case CPINDEX -> {
                    // processing class references: Name, [Descriptor|Signature]
                    verifyPosition(buffer, headerLength, attributeName, parseKey, start_pc, length, slot);
                    String str = scanner.stringValue;
                    if (nameCell == null) {
                        nameCell = parser.pool.findUTF8Cell(str);
                    } else if (descriptorCell == null) {
                        descriptorCell = parser.pool.findUTF8Cell(str);
                        list.add(new LocalVariableData(start_pc, length, slot, nameCell, descriptorCell));
                        // next new line
                        slot = start_pc = length = -1;
                        nameCell = descriptorCell = null;
                    } else {
                        environment.throwErrorException(scanner.pos, "err.header.expected.locvars", parseKey);
                    }
                }
                default -> {
                    if (buffer.length() != headerLength) {
                        environment.throwErrorException(scanner.pos, "err.header.locvars",
                                parseKey);
                    }
                    if (slot != -1 || start_pc != -1 || length != -1 || nameCell != null || descriptorCell != null) {
                        environment.throwErrorException(scanner.pos, "err.expected.locvars",
                                attributeName, " either the line {start_pc length slot \"name\" \"descriptor\"} or " +
                                        "{start_pc length slot name_index descriptor_index}");
                    }
                    return list;
                }
            }
            scanner.scan();
        }
        return list;
    }

    private void verifyPosition(String buffer, int headerLength,
                                String attributeName, String parseKey,
                                short start_pc, short length, short slot) {
        if (buffer.length() != headerLength) {
            environment.throwErrorException(scanner.pos, "err.header.locvars", parseKey);
        }
        if (start_pc == -1) {
            environment.throwErrorException(scanner.pos, "err.expected.locvars", attributeName, "\"start_pc\"");
        } else if (length == -1) {
            environment.throwErrorException(scanner.pos, "err.expected.locvars", attributeName, "\"length\"");
        } else if (slot == -1) {
            environment.throwErrorException(scanner.pos, "err.expected.locvars", attributeName, "\"slot\"");
        }
    }

    public List<StackMapData> parseStackMap() throws SyntaxError, IOException {
        if (scanner.token == COLON) {
            // ignore
            scanner.scan();
        }
        ArrayList<StackMapData> list = new ArrayList<>();
        int numEntries = 0;
        StackMap.EntryType entryType = UNKNOWN_TYPE;
        StackMapData stackMapData = null;

        while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
            switch (scanner.token) {
                case NUMBEROFENTRIES -> {               // number_of_entries
                    scanner.scan();
                    scanner.expect(ASSIGN);
                    if (scanner.token != INTVAL) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "\"number of entries\"");
                    }
                    numEntries = scanner.intValue;
                }
                case BYTECODEOFFSET -> {                // BCI
                    if (stackMapData != null) {
                        list.add(stackMapData);
                    }
                    scanner.scan();
                    scanner.expect(ASSIGN);
                    if (scanner.token != INTVAL) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "The offset value");
                    }
                    stackMapData = new StackMapData(environment, false);
                    stackMapData.setPC(scanner.intValue);
                }
                case LOCALSMAP -> {                     // locals_map
                    if (stackMapData == null) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "\"%s\"".formatted(BYTECODEOFFSET.parseKey()));
                    } else {
                        if (stackMapData.localsMap != null && stackMapData.localsMap.size() > 0) {
                            environment.warning(scanner.pos, "warn.stackmap.redeclared",
                                    "\"%s\"".formatted(LOCALSMAP.parseKey()));
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        if (scanner.token == IDENT && scanner.stringValue.equals(LSQBRACKET.parseKey() + RSQBRACKET.parseKey())) {
                            // empty list
                            stackMapData.localsMap = new DataVector();
                        } else {
                            scanner.expectIdentContent(LSQBRACKET);
                            DataVector localsMap = new DataVector();
                            stackMapData.localsMap = localsMap;
                            while (scanner.token != EOF) {
                                if (scanner.token == SEMICOLON) {
                                    scanner.scan();
                                    scanner.expectIdentContent(RSQBRACKET);
                                    break;
                                }
                                // list can be empty due to some issues
                                if (scanner.token == IDENT && scanner.stringValue.equals(RSQBRACKET.parseKey())) {
                                    // LocalsMap could be empty:  environment.warning(scanner.pos, "warm.locals_map.empty");
                                    break;
                                }
                                parser.parseMapItem(localsMap);
                                if (scanner.token != JasmTokens.Token.COMMA) {
                                    if (scanner.token != SEMICOLON) {
                                        environment.throwErrorException(scanner.pos, "err.token.expected", "\"" +
                                                SEMICOLON.parseKey() + "\"");
                                    }
                                    continue;
                                }
                                scanner.scan();
                            }
                            continue;
                        }
                    }
                }
                case STACKMAP -> {
                    if (stackMapData == null) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "\"%s\"".formatted(BYTECODEOFFSET.parseKey()));
                    } else {

                        if (stackMapData.stackMap != null && stackMapData.stackMap.size() > 0) {
                            environment.warning(scanner.pos, "warn.stackmap.redeclared",
                                    "\"%s\"".formatted(STACKMAP.parseKey()));
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        if (scanner.token == IDENT && scanner.stringValue.equals(LSQBRACKET.parseKey() + RSQBRACKET.parseKey())) {
                            // empty list
                            stackMapData.stackMap = new DataVector();

                        } else {
                            scanner.expectIdentContent(LSQBRACKET);
                            DataVector stackMap = new DataVector();
                            stackMapData.stackMap = stackMap;
                            while (scanner.token != EOF) {
                                if (scanner.token == SEMICOLON) {
                                    scanner.scan();
                                    scanner.expectIdentContent(RSQBRACKET);
                                    break;
                                }
                                // the list can be empty due to some issues
                                if (scanner.token == IDENT && scanner.stringValue.equals(RSQBRACKET.parseKey())) {
                                    // StackMap could be empty: environment.warning(scanner.pos, "warm.stack_map.empty");
                                    break;
                                }
                                parser.parseMapItem(stackMap);
                                if (scanner.token != JasmTokens.Token.COMMA) {
                                    if (scanner.token != SEMICOLON) {
                                        environment.throwErrorException(scanner.pos, "err.token.expected", "\"" +
                                                SEMICOLON.parseKey() + "\"");
                                    }
                                    continue;
                                }
                                scanner.scan();
                            }
                            continue;
                        }
                    }
                }
            }
            scanner.scan();
        }
        if (stackMapData != null) {
            list.add(stackMapData);
        }
        return list;
    }

    public List<StackMapData> parseStackMapTable() throws SyntaxError, IOException {
        int wrapLevel = 0;
        int numEntries = 0;    // - might be used to check that the header's number matches the actual number of records.
        ArrayList<StackMapData> list = new ArrayList<>();
        int stackFrameTypeValue = UNDEFINED;
        StackMap.EntryType entryType = UNKNOWN_TYPE;
        StackMapData stackMapData = null;

        scanner.scan();
        if (scanner.token == COLON) {
            // ignore
            scanner.scan();
        }
        while (scanner.token != EOF) {
            switch (scanner.token) {
                case LBRACE -> {
                    if (wrapLevel == 0) {
                        environment.throwErrorException(scanner.pos, "err.larvar.frame.expected");
                    }
                }
                case RBRACE -> {
                    if (wrapLevel == 0) {
//                        if (entryType == EARLY_LARVAL) {
//                            environment.throwErrorException(scanner.pos, "err.base.frame.expected");
//                        }
                        return list;
                    }
                    wrapLevel--;
                }
                case NUMBEROFENTRIES -> {               // number_of_entries
                    scanner.scan();
                    scanner.expect(ASSIGN);
                    if (scanner.token != INTVAL) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "\"number of entries\"");
                    }
                    numEntries = scanner.intValue;
                }
                case FRAMETYPE, ENTRYTYPE -> {                   // frame_type, entry_type
                    scanner.scan();
                    scanner.expect(ASSIGN);
                    if (scanner.token == INTVAL) {
                        stackFrameTypeValue = scanner.intValue;
                        entryType = StackMap.EntryType.getByTag(stackFrameTypeValue);
                    }
                    if (entryType == UNKNOWN_TYPE) {
                        environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                "An integer within the range of [0 to 255]");
                    }
                    if (entryType == EARLY_LARVAL) {
                        if (wrapLevel > 2) {        // limit of enclosure
                            environment.throwErrorException(scanner.pos, "err.base.frame.expected");
                        }
                        wrapLevel++;
                    }

                    if (stackMapData == null) {
                        stackMapData = new StackMapData(environment, parser.curCodeAttr.isTypeCheckingVerifier());
                    } else {
                        JasmTokens.Token expectedToken = stackMapData.checkIntegrity();
                        if (expectedToken == null) {
                            list.add(stackMapData);
                            stackMapData = new StackMapData(environment, parser.curCodeAttr.isTypeCheckingVerifier());
                        } else {
                            if (expectedToken == STACKMAP || stackMapData.getFrameType() == FULL_FRAME) {
                                stackMapData.stackMap = new DataVector<>();
                                list.add(stackMapData);
                                stackMapData = new StackMapData(environment, parser.curCodeAttr.isTypeCheckingVerifier());
                                environment.warning(scanner.pos, "warn.stackmap.expected",
                                        "\"%s\"".formatted(expectedToken.parseKey()));
//                            } else if (expectedToken == UNSETFIELDS) {
//                                // missing means empty unset_fields
//                                stackMapData.unsetFields = new DataVector<>();
//                                list.add(stackMapData);
//                                stackMapData = new StackMapData(environment, parser.curCodeAttr.isTypeCheckingVerifier());
                        } else {
                                environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                        "\"%s\"".formatted(expectedToken.parseKey()));
                            }
                        }
                    }
                    stackMapData.setStackFrameType(stackFrameTypeValue);
                    if (stackMapData.checkIntegrity() == null) {
                        list.add(stackMapData);
                        stackMapData = null;
                    }
                    continue;
                }
                case OFFSETDELTA -> {                  // offset_delta
                    if (stackMapData == null || stackMapData.getFrameType() == SAME_FRAME) {
                        if (wrapLevel > 0) {
                            environment.throwErrorException(scanner.pos, "err.token.expected", RBRACE.parseKey());
                        } else {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(FRAMETYPE.parseKey()));
                        }
                    } else {
                        JasmTokens.Token expectedToken = stackMapData.checkIntegrity();
                        if (expectedToken != OFFSETDELTA) {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(expectedToken.parseKey()));
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        if (scanner.token != INTVAL) {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "The offset_delta value");
                        }
                        stackMapData.setOffset(scanner.intValue);
                        if (stackMapData.checkIntegrity() == null) {
                            list.add(stackMapData);
                            stackMapData = null;
                        }
                    }
                }
                case LOCALSMAP -> {                     // locals_map
                    if (stackMapData == null || stackMapData.getFrameType() == SAME_FRAME) {
                        if (wrapLevel > 0) {
                            environment.throwErrorException(scanner.pos, "err.token.expected", RBRACE.parseKey());
                        } else {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(FRAMETYPE.parseKey()));
                        }
                    } else {
                        JasmTokens.Token expectedToken = stackMapData.checkIntegrity();
                        if (expectedToken != LOCALSMAP) {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(expectedToken.parseKey()));
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        stackMapData.localsMap = new DataVector();
                        // The scanner identifies an empty statement ([] in locals_map    = []) as an identifier.
                        if (scanner.token == IDENT && scanner.stringValue.equals(LSQBRACKET.parseKey() + RSQBRACKET.parseKey())) {
                            scanner.scan();
                        } else {
                            scanner.expectIdentContent(LSQBRACKET);
                            while (scanner.token != EOF) {
                                if (scanner.token == SEMICOLON) {
                                    scanner.scan();
                                    scanner.expectIdentContent(RSQBRACKET);
                                    break;
                                }
                                // list can be empty due to some issues
                                if (scanner.token == IDENT && scanner.stringValue.equals(RSQBRACKET.parseKey())) {
                                    // LocalsMap could be empty:  environment.warning(scanner.pos, "warm.locals_map.empty");
                                    break;
                                }
                                parser.parseMapItem(stackMapData.localsMap);
                                if (scanner.token != JasmTokens.Token.COMMA) {
                                    if (scanner.token != SEMICOLON) {
                                        environment.throwErrorException(scanner.pos, "err.token.expected", "\"" +
                                                SEMICOLON.parseKey() + "\"");
                                    }
                                    continue;
                                }
                                scanner.scan();
                            }
                        }
                        if (stackMapData.checkIntegrity() == null) {
                            list.add(stackMapData);
                            stackMapData = null;
                        }
                        continue;
                    }
                }
                case STACKMAP -> {
                    if (stackMapData == null || stackMapData.getFrameType() == SAME_FRAME) {
                        if (wrapLevel > 0) {
                            environment.throwErrorException(scanner.pos, "err.token.expected", RBRACE.parseKey());
                        } else {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(FRAMETYPE.parseKey()));
                        }
                    } else {
                        JasmTokens.Token expectedToken = stackMapData.checkIntegrity();
                        if (expectedToken != STACKMAP) {
                            if (expectedToken != LOCALSMAP || stackMapData.getFrameType() != FULL_FRAME) {
                                environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                        "\"%s\"".formatted(expectedToken.parseKey()));
                            }
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        stackMapData.stackMap = new DataVector();
                        // The scanner identifies an empty statement ([] in stack_map    = []) as an identifier.
                        if (scanner.token == IDENT && scanner.stringValue.equals(LSQBRACKET.parseKey() + RSQBRACKET.parseKey())) {
                            scanner.scan();
                        } else {
                            scanner.expectIdentContent(LSQBRACKET);
                            while (scanner.token != EOF) {
                                if (scanner.token == SEMICOLON) {
                                    scanner.scan();
                                    scanner.expectIdentContent(RSQBRACKET);
                                    break;
                                }
                                // the list can be empty due to some issues
                                if (scanner.token == IDENT && scanner.stringValue.equals(RSQBRACKET.parseKey())) {
                                    // StackMap could be empty: environment.warning(scanner.pos, "warm.stack_map.empty");
                                    break;
                                }
                                parser.parseMapItem(stackMapData.stackMap);
                                if (scanner.token != JasmTokens.Token.COMMA) {
                                    if (scanner.token != SEMICOLON) {
                                        environment.throwErrorException(scanner.pos, "err.token.expected", "\"" +
                                                SEMICOLON.parseKey() + "\"");
                                    }
                                    continue;
                                }
                                scanner.scan();
                            }
                        }
                        if (stackMapData.checkIntegrity() == null) {
                            list.add(stackMapData);
                            stackMapData = null;
                        }
                        continue;
                    }
                }
                case UNSETFIELDS -> {
                    if (stackMapData == null || stackMapData.getFrameType() == SAME_FRAME) {
                        if (wrapLevel > 0) {
                            environment.throwErrorException(scanner.pos, "err.token.expected", RBRACE.parseKey());
                        } else {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(FRAMETYPE.parseKey()));
                        }
                    } else {
                        JasmTokens.Token expectedToken = stackMapData.checkIntegrity();
                        if (expectedToken != UNSETFIELDS) {
                            environment.throwErrorException(scanner.pos, "err.stackmap.expected",
                                    "\"%s\"".formatted(expectedToken.parseKey()));
                        }
                        scanner.scan();
                        scanner.expect(ASSIGN);
                        stackMapData.unsetFields = new DataVector();
                        // The scanner identifies an empty statement ([ ] in unset_fields    = [ ]) as an identifier.
                        if (scanner.token == IDENT && scanner.stringValue.equals(LSQBRACKET.parseKey() + RSQBRACKET.parseKey())) {
                            scanner.scan();
                        } else {
                            scanner.expectIdentContent(LSQBRACKET);
                            while (scanner.token != EOF) {
                                if (scanner.token == SEMICOLON) {
                                    scanner.scan();
                                    scanner.expectIdentContent(RSQBRACKET);
                                    break;
                                }
                                // the list can be empty due to some issues
                                if (scanner.token == IDENT && scanner.stringValue.equals(RSQBRACKET.parseKey())) {
                                    // StackMap could be empty: environment.warning(scanner.pos, "warm.stack_map.empty");
                                    break;
                                }
                                parser.parseNameAndType(stackMapData.unsetFields);
                                if (scanner.token != JasmTokens.Token.COMMA) {
                                    if (scanner.token != SEMICOLON) {
                                        environment.throwErrorException(scanner.pos, "err.token.expected", SEMICOLON.parseKey());
                                    }
                                    continue;
                                }
                                scanner.scan();
                            }
                        }
                        if (stackMapData.checkIntegrity() == null) {
                            list.add(stackMapData);
                            stackMapData = null;
                        }
                        continue;
                    }
                }
//                default -> {
//                    if (wrapLevel == 0) {
//                        environment.throwErrorException(scanner.pos, "err.token.expected", RBRACE.parseKey());
//                    }
//                }
            }
            scanner.scan();
        }
        return list;
    }

    public List<LineNumberData> parseLineTable() {
        ArrayList<LineNumberData> list = new ArrayList<>();
        scanner.scan();
        if (scanner.token == COLON) {
            // ignore
            scanner.scan();
        }
        int start_pc = -1, line_number = -1;
        boolean newLine = true, colonFound = false;
        while ((scanner.token != EOF) && (scanner.token != RBRACE)) {
            switch (scanner.token) {
                case COLON -> {
                    if (newLine) {
                        environment.throwErrorException(scanner.pos, "err.expected.linetable",
                                "line keyword");
                    }
                    if (line_number == -1) {
                        environment.throwErrorException(scanner.pos, "err.expected.linetable",
                                "line_number:");
                    } else if (colonFound) {
                        if (start_pc == -1)
                            environment.throwErrorException(scanner.pos, "err.expected.linetable",
                                    "start_pc");
                        else
                            environment.throwErrorException(scanner.pos, "err.expected.linetable",
                                    "line keyword");
                    }
                    colonFound = true;
                }
                case LINE -> {
                    if (!newLine) {
                        environment.throwErrorException(scanner.pos, "err.expected.linetable",
                                "line  line_number:  start_pc");
                    }
                    newLine = false;
                }
                case INTVAL -> {
                    if (line_number == -1)
                        line_number = scanner.intValue;
                    else {
                        start_pc = scanner.intValue;
                        list.add(new LineNumberData(start_pc, line_number));
                        newLine = true;
                        colonFound = false;
                        line_number = -1;
                        start_pc = -1;
                    }
                }
                default -> {
                    if (line_number != -1 || start_pc != -1 || !newLine) {
                        environment.throwErrorException(scanner.pos, "err.expected.linetable", "line  line_number:  start_pc");
                    }
                    return list;
                }
            }
            scanner.scan();
        }
        return list;
    }
}
