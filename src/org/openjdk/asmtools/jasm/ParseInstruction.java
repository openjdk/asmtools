/*
 * Copyright (c) 1996, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.openjdk.asmtools.common.structure.StackMap.EntryType.EARLY_LARVAL;
import static org.openjdk.asmtools.jasm.ClassFileConst.ConstType;
import static org.openjdk.asmtools.jasm.JasmTokens.Token;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.ENTRYTYPE;
import static org.openjdk.asmtools.jasm.JasmTokens.Token.FRAMETYPE;
import static org.openjdk.asmtools.jasm.OpcodeTables.*;
import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode.*;

/**
 * Instruction Parser
 * <p>
 * ParserInstr is a parser class owned by Parser.java. It is primarily responsible for
 * parsing instruction byte codes.
 */
public class ParseInstruction extends ParseBase {

    /**
     * local handle for the constant parser - needed for parsing constants during
     * instruction construction.
     */
    private final ParseConstPool instructionParser;

    /**
     * Constructor
     *
     * @param parser   parent, main parser
     * @param cpParser constant pool parser
     */
    protected ParseInstruction(Parser parser, ParseConstPool cpParser) throws IOException {
        super.init(parser);
        this.instructionParser = cpParser;
    }

    private boolean isInstruction(Token token) {
        return (token == Token.IDENT || token.in(Token.LOCALSMAP, Token.STACKMAP, Token.UNSETFIELDS));
    }

    /**
     * Parse an instruction.
     */
    protected void parseInstr() throws SyntaxError, IOException {
        // ignore possible line numbers after java disassembler
        if (scanner.token == Token.INTVAL) {
            scanner.scan();
        }
        // ignore possible numeric labels after java disassembler
        if (scanner.token == Token.INTVAL) {
            scanner.scan();
        }
        if (scanner.token == Token.COLON) {
            scanner.scan();
        }

        String mnemocode;
        long mnenoc_pos;
        for (; ; ) { // read labels
            if (!isInstruction(scanner.token)) {
                return;
            }

            mnemocode = scanner.idValue;
            mnenoc_pos = scanner.pos;
            scanner.scan();
            if (scanner.token != Token.COLON) {
                break;
            }
            // actually it was a label
            scanner.scan();
            parser.curCodeAttr.LabelDef(mnenoc_pos, mnemocode);
        }

        Opcode opcode = OpcodeTables.opcode(mnemocode);
        if (opcode == null) {
            environment.error(mnenoc_pos, "err.wrong.mnemocode", mnemocode);
            throw new SyntaxError();
        }
        OpcodeType opcodeType = opcode.type();

        Indexer arg = null;
        Object arg2 = null;
        StackMapData stackMapData;
        scanner.debugScan(mnenoc_pos, "parseInstr: MnemoCode \'%s\'".formatted(opcode.parseKey()));
        switch (opcodeType) {
            case NORMAL:
                switch (opcode) {

                    // pseudo-instructions:
                    case opc_bytecode:
                        for (; ; ) {
                            parser.curCodeAttr.addInstr(mnenoc_pos, Opcode.opc_bytecode, parser.parseUInt(1), null);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_try:
                        for (; ; ) {
                            parser.curCodeAttr.beginTrap(scanner.pos, parser.parseIdent());
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_endtry:
                        for (; ; ) {
                            parser.curCodeAttr.endTrap(scanner.pos, parser.parseIdent());
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_catch:
                        parser.curCodeAttr.trapHandler(scanner.pos, parser.parseIdent(),
                                instructionParser.parseConstRef(ConstType.CONSTANT_CLASS));
                        return;
                    case opc_var:
                        for (; ; ) {
                            parser.parseLocVarDef(opc_var);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_endvar:
                        for (; ; ) {
                            parser.parseLocVarEnd(opc_var);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_type:
                        for (; ; ) {
                            parser.parseLocVarDef(opc_type);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_endtype:
                        for (; ; ) {
                            parser.parseLocVarEnd(opc_type);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }

                    case opc_locals_map:
                        stackMapData = parser.curCodeAttr.getStackMapTable();
                        if (stackMapData.localsMap != null) {
                            environment.error(scanner.pos, "err.stackmap.entity.repeated", opc_locals_map.parseKey());
                        }
                        DataVector localsMap = new DataVector();
                        stackMapData.localsMap = localsMap;
                        stackMapData.setScannerPosition(scanner.pos);
                        if (scanner.token == Token.SEMICOLON) {
                            return;  // empty locals_map allowed
                        }
                        for (; ; ) {
                            parser.parseMapItem(localsMap);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_stack_map:
                        stackMapData = parser.curCodeAttr.getStackMapTable();
                        if (stackMapData.stackMap != null) {
                            environment.error(scanner.pos, "err.stackmap.entity.repeated", opc_stack_map.parseKey());
                        }
                        DataVector stackMap = new DataVector();
                        stackMapData.stackMap = stackMap;
                        stackMapData.setScannerPosition(scanner.pos);
                        if (scanner.token == Token.SEMICOLON) {
                            return;  // empty stack_map allowed
                        }
                        for (; ; ) {
                            parser.parseMapItem(stackMap);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_unset_fields:
                        stackMapData = parser.curCodeAttr.getStackMapTable();
                        if (!stackMapData.isWrapper()) {
                            environment.error(scanner.pos, "err.stackmap.map.eligible",
                                    opc_unset_fields.parseKey(),
                                    EARLY_LARVAL.tagName());
                        }
                        if (stackMapData.unsetFields != null) {
                            environment.error(scanner.pos, "err.stackmap.entity.repeated", opc_unset_fields.parseKey());
                        }
                        DataVector unsetFields = new DataVector();
                        stackMapData.unsetFields = unsetFields;
                        stackMapData.setScannerPosition(scanner.pos);
                        if (scanner.token == Token.SEMICOLON) {
                            // empty unset_fields allowed
                            scanner.scan();
                            scanner.expectOneOf(List.of(opc_stack_map_entry.parseKey(), opc_stack_frame_type.parseKey()),
                                    FRAMETYPE, ENTRYTYPE);
                        } else {
                            for (; ; ) {
                                parser.parseNameAndType(unsetFields);
                                if (scanner.token != Token.COMMA) {
                                    scanner.expect(Token.SEMICOLON);
                                    scanner.expectOneOf(List.of(opc_stack_map_entry.parseKey(), opc_stack_frame_type.parseKey()),
                                            FRAMETYPE, ENTRYTYPE);
                                    break;
                                }
                                scanner.scan();
                            }
                        }
                        opcode = opc_frame_type;
                        scanner.scan();
                        // continue to the next case: opc_stack_frame_type, opc_stack_map_entry, opc_frame_type, opc_entry_type
                        // StackMapTable Attribute (Since 7.0)
                    case opc_stack_frame_type, opc_stack_map_entry,
                         opc_frame_type, opc_entry_type:
                        stackMapData = parser.curCodeAttr.getStackMapTable();
                        if (stackMapData.isFrameTypeSet()) {
                            if (stackMapData.isWrapper()) {
                                stackMapData = parser.curCodeAttr.getNextStackMapTable();
                            } else {
                                environment.error(scanner.pos, "err.stackmaptable.repeated");
                            }
                        }
                        stackMapData.setScannerPosition(scanner.pos).setStackFrameTypeByName(parser.parseIdent());
                        return;
                    // StackMap Attribute (Java 6.0)
                    case opc_stack_map_frame:                                       // stack_map_frame
                        stackMapData = parser.curCodeAttr.getStackMapTable();
                        if (stackMapData.isFrameTypeSet()) {
                            environment.error(scanner.pos, "err.stackmaptable.repeated");
                        }
                        stackMapData.setScannerPosition(scanner.pos);
                        return;
                    // normal instructions:
                    case opc_aload:
                    case opc_astore:
                    case opc_fload:
                    case opc_fstore:
                    case opc_iload:
                    case opc_istore:
                    case opc_lload:
                    case opc_lstore:
                    case opc_dload:
                    case opc_dstore:
                    case opc_ret:
                    case opc_aload_w:
                    case opc_astore_w:
                    case opc_fload_w:
                    case opc_fstore_w:
                    case opc_iload_w:
                    case opc_istore_w:
                    case opc_lload_w:
                    case opc_lstore_w:
                    case opc_dload_w:
                    case opc_dstore_w:
                    case opc_ret_w:
                        // loc var
                        arg = parser.parseLocVarRef();
                        break;
                    case opc_iinc: // loc var, const
                        arg = parser.parseLocVarRef();
                        scanner.expect(Token.COMMA);
                        arg2 = parser.parseInt(opcode.parseKey(), 1);
                        break;
                    case opc_tableswitch:
                    case opc_lookupswitch:
                        arg2 = parseSwitchTable();
                        break;
                    case opc_newarray: {
                        int type;
                        if (scanner.token == Token.INTVAL) {
                            type = scanner.intValue;
                        } else if ((type = ClassFileConst.basicTypeValue(scanner.idValue)) == -1) {
                            environment.error(scanner.pos, "err.array.type.expected");
                            throw new SyntaxError();
                        }
                        scanner.scan();
                        arg = new Indexer(type);
                        break;
                    }
                    case opc_new:
                    case opc_anewarray:
                    case opc_instanceof:
                    case opc_checkcast:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_CLASS);
                        break;
                    case opc_bipush:
                        arg = parser.parseInt(opcode.parseKey(), 1);
                        break;
                    case opc_sipush:
                        arg = parser.parseInt(opcode.parseKey(), 2);
                        break;
                    case opc_ldc:
                    case opc_ldc_w:
                    case opc_ldc2_w:
                        arg = instructionParser.parseConstRef(null);
                        break;
                    case opc_putstatic:
                    case opc_getstatic:
                    case opc_putfield:
                    case opc_getfield:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_FIELDREF);
                        break;
                    case opc_invokevirtual:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_METHODREF);
                        break;
                    case opc_invokestatic:
                    case opc_invokespecial:
                        ConstType ctype01 = ConstType.CONSTANT_METHODREF;
                        ConstType ctype02 = ConstType.CONSTANT_INTERFACEMETHODREF;
                        if (Modifier.isInterface(this.parser.classData.access)) {
                            ctype01 = ConstType.CONSTANT_INTERFACEMETHODREF;
                            ctype02 = ConstType.CONSTANT_METHODREF;
                        }
                        arg = instructionParser.parseConstRef(ctype01, ctype02);
                        break;
                    case opc_jsr:
                    case opc_goto:
                    case opc_ifeq:
                    case opc_ifge:
                    case opc_ifgt:
                    case opc_ifle:
                    case opc_iflt:
                    case opc_ifne:
                    case opc_if_icmpeq:
                    case opc_if_icmpne:
                    case opc_if_icmpge:
                    case opc_if_icmpgt:
                    case opc_if_icmple:
                    case opc_if_icmplt:
                    case opc_if_acmpeq:
                    case opc_if_acmpne:
                    case opc_ifnull:
                    case opc_ifnonnull:
                    case opc_jsr_w:
                    case opc_goto_w:
                        arg = parseLabelRef();
                        break;
                    case opc_invokeinterface:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_INTERFACEMETHODREF);
                        scanner.expect(Token.COMMA);
                        arg2 = parser.parseUInt(1);
                        break;
                    case opc_invokedynamic:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_INVOKEDYNAMIC);
                        break;

                    case opc_multianewarray:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_CLASS);
                        scanner.expect(Token.COMMA);
                        arg2 = parser.parseUInt(1);
                        break;
                    case opc_wide:
                    case opc_nonpriv:
                    case opc_priv:
                        int opc2 = (opcode.value() << 8) | parser.parseUInt(1).cpIndex;
                        opcode = opcode(opc2);
                        break;
                }
                break;
            case WIDE:
                arg = parser.parseLocVarRef();
                if (opcode == Opcode.opc_iinc_w) { // loc var, const
                    scanner.expect(Token.COMMA);
                    arg2 = parser.parseInt(opcode.parseKey(), 2);
                }
                break;
            case NONPRIVELEGED:
            case PRIVELEGED:
                break;
            default:
                environment.error(scanner.prevPos, "err.wrong.mnemocode", mnemocode);
                throw new SyntaxError();
        }
        parser.curCodeAttr.addInstr(mnenoc_pos, opcode, arg, arg2);
    } //end parseInstr

    /**
     * Parse a Switch Table. return value: SwitchTable.
     */
    protected SwitchTable parseSwitchTable() throws SyntaxError, IOException {
        scanner.expect(Token.LBRACE);
        Indexer label;
        int numpairs = 0, key;
        SwitchTable table = new SwitchTable(environment);
        tableScan:
        {
            while (numpairs < MAX_LOOKUPSWITCH_PAIRS_COUNT) {
                switch (scanner.token) {
                    case INTVAL:
                        key = scanner.intValue * scanner.sign;
                        scanner.scan();
                        scanner.expect(Token.COLON);
                        table.addEntry(key, parseLabelRef());
                        numpairs++;
                        if (scanner.token != Token.SEMICOLON) {
                            break tableScan;
                        }
                        scanner.scan();
                        break;
                    case DEFAULT:
                        scanner.scan();
                        scanner.expect(Token.COLON);
                        if (table.defLabel != null) {
                            environment.error("err.default.redecl");
                        }
                        table.defLabel = parseLabelRef();
                        if (scanner.token != Token.SEMICOLON) {
                            break tableScan;
                        }
                        scanner.scan();
                        break;
                    default:
                        break tableScan;
                }
            }
            environment.error("err.long.switchtable", MAX_LOOKUPSWITCH_PAIRS_COUNT);
        } // end tableScan
        scanner.expect(Token.RBRACE);
        return table;
    } // end parseSwitchTable

    /**
     * Parse a label instruction argument
     */
    protected Indexer parseLabelRef() throws SyntaxError, IOException {
        switch (scanner.token) {
            case INTVAL: {
                int v = scanner.intValue * scanner.sign;
                scanner.scan();
                return new Indexer(v);
            }
            case IDENT: {
                String label = scanner.stringValue;
                scanner.scan();
                return parser.curCodeAttr.LabelRef(label);
            }
        }
        environment.error("err.label.expected");
        throw new SyntaxError();
    }
}
