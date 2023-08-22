/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.asmtools.jasm.JasmTokens.*;
import static org.openjdk.asmtools.jasm.ClassFileConst.*;
import static org.openjdk.asmtools.jasm.OpcodeTables.*;
import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Instruction Parser
 *
 * ParserInstr is a parser class owned by Parser.java. It is primarily responsible for
 * parsing instruction byte codes.
 */
public class ParserInstr extends ParseBase {

    /**
     * local handle for the constant parser - needed for parsing constants during
     * instruction construction.
     */
    private final ParserCP instructionParser;

    /**
     * Constructor
     * @param parser    parent, main parser
     * @param cpParser  constant pool parser
     */
    protected ParserInstr(Parser parser, ParserCP cpParser) throws IOException {
        super.init(parser);
        this.instructionParser = cpParser;
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
        int mnenoc_pos;
        for (;;) { // read labels
            if (scanner.token != Token.IDENT) {
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

        scanner.debugScan(" --IIIII---[ParserInstr:[parseInstr]:  (Pos: " + mnenoc_pos + ") mnemocode: '" + opcode.parseKey() + "' ");

        switch (opcodeType) {
            case NORMAL:
                switch (opcode) {

                    // pseudo-instructions:
                    case opc_bytecode:
                        for (;;) {
                            parser.curCodeAttr.addInstr(mnenoc_pos, Opcode.opc_bytecode, parser.parseUInt(1), null);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_try:
                        for (;;) {
                            parser.curCodeAttr.beginTrap(scanner.pos, parser.parseIdent());
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_endtry:
                        for (;;) {
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
                        for (;;) {
                            parser.parseLocVarDef();
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_endvar:
                        for (;;) {
                            parser.parseLocVarEnd();
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_locals_map:
                        stackMapData = parser.curCodeAttr.getStackMap();
                        if (stackMapData.localsMap != null) {
                            environment.error(scanner.pos, "err.localsmap.repeated");
                        }
                        DataVector localsMap = new DataVector();
                        stackMapData.localsMap = localsMap;
                        if (scanner.token == Token.SEMICOLON) {
                            return;  // empty locals_map allowed
                        }
                        for (;;) {
                            parser.parseMapItem(localsMap);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_stack_map:
                        stackMapData = parser.curCodeAttr.getStackMap();
                        if (stackMapData.stackMap != null) {
                            environment.error(scanner.pos, "err.stackmap.repeated");
                        }
                        DataVector stackMap = new DataVector();
                        stackMapData.stackMap = stackMap;
                        if (scanner.token == Token.SEMICOLON) {
                            return;  // empty stack_map allowed
                        }
                        for (;;) {
                            parser.parseMapItem(stackMap);
                            if (scanner.token != Token.COMMA) {
                                return;
                            }
                            scanner.scan();
                        }
                    case opc_stack_frame_type:
                        stackMapData = parser.curCodeAttr.getStackMap();
                        if (stackMapData.isSet()) {
                            environment.error(scanner.pos, "err.stackmaptable.repeated");
                        }
                        stackMapData.setScannerPosition(scanner.pos).setStackFrameType(parser.parseIdent());
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
                    case opc_aconst_init:  // Valhalla
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
                    case opc_withfield:     // Valhalla
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_FIELDREF);
                        break;
                    case opc_invokevirtual:
                        arg = instructionParser.parseConstRef(ConstType.CONSTANT_METHODREF);
                        break;
                    case opc_invokestatic:
                    case opc_invokespecial:
                        ConstType ctype01  = ConstType.CONSTANT_METHODREF;
                        ConstType ctype02  = ConstType.CONSTANT_INTERFACEMETHODREF;
                        if(Modifier.isInterface(this.parser.classData.access)) {
                            ctype01  = ConstType.CONSTANT_INTERFACEMETHODREF;
                            ctype02  = ConstType.CONSTANT_METHODREF;
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
                    arg2 = parser.parseInt(opcode.parseKey(),2);
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
            while (numpairs < 1000) {
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
                } // end switch
            } // while (numpairs<1000)
            environment.error("err.long.switchtable", "1000");
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
