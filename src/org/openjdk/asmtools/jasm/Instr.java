/*
 * Copyright (c) 1996, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Optional;

import static org.openjdk.asmtools.jasm.OpcodeTables.Opcode;
import static org.openjdk.asmtools.jasm.OpcodeTables.OpcodeType;

//
class Instr {

    // environment is needed to fix CP references and show error(s)/message(s)
    private final JasmEnvironment environment;
    private final ConstantPool pool;
    private int pos;
    //
    Instr next = null;
    int pc;
    Opcode opc;
    Indexer arg;
    Object arg2; // second or unusual argument

    public Instr(ConstantPool pool, JasmEnvironment environment) {
        this.environment = environment;
        this.pool = pool;
    }

    public Instr set(int pc, int pos, Opcode opc, Indexer arg, Object arg2) {
        this.pc = pc;
        this.pos = pos;
        this.opc = opc;
        this.arg = arg;
        this.arg2 = arg2;
        return this;
    }

    private Indexer fixReference(Indexer arg) {
        if( arg != null && arg instanceof ConstCell<?>) {
            ConstCell<?> cell = (ConstCell<?>) arg;
            if( cell.ref == null || arg.cpIndex == 0) { // Corner case cell[0] has value but its reference is wrong
                // For negative testing: when instruction refers to a wrong Constant Pool cell
                // asm just shows a warning.
                environment.warning(pos - String.valueOf(arg.cpIndex).length()-1,
                        "warn.instr.wrong.arg", opc.parseKey(), arg.cpIndex);
                return arg;
            }
            if( !arg.isSet()) {
                Optional<ConstCell<?>> optionalCell = pool.getItemizedCell((ConstCell<?>) arg);
                if (optionalCell.isPresent()) {
                    arg = optionalCell.get();
                } else {
                    environment.throwErrorException(pos - String.valueOf(arg.cpIndex).length()-1,
                            "err.instr.wrong.arg", opc.parseKey(), arg.cpIndex);
                }
            }
        }
        return arg;
    }

    public void write(CheckedDataOutputStream out) throws IOException {
        OpcodeType type = opc.type();
        arg = fixReference(arg);
        switch (type) {
            case NORMAL: {
                if (opc == Opcode.opc_bytecode) {
                    out.writeByte(arg.cpIndex);
                    return;
                }
                out.writeByte(opc.value());
                int opcLen = opc.length();
                if (opcLen == 1) {
                    return;
                }

                switch (opc) {
                    case opc_tableswitch:
                        ((SwitchTable) arg2).writeTableSwitch(out);
                        return;
                    case opc_lookupswitch:
                        ((SwitchTable) arg2).writeLookupSwitch(out);
                        return;
                }

                int iarg = 0;
                try {
                    iarg = arg.cpIndex;
                } catch (NullPointerException e) {
                    environment.throwErrorException("err.instr.null.arg", opc.parseKey());
                }
//env.traceln("instr:"+opcNamesTab[opc]+" len="+opcLen+" arg:"+iarg);
                switch (opc) {
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
                        iarg = iarg - pc;
                        break;
                    case opc_iinc:
                        iarg = (iarg << 8) | (((Indexer) arg2).cpIndex & 0xFF);
                        break;
                    case opc_invokeinterface:
                        iarg = ((iarg << 8) | (((Indexer) arg2).cpIndex & 0xFF)) << 8;
                        break;
                    case opc_invokedynamic: // JSR-292
                        iarg = (iarg << 16);
                        break;
                    case opc_ldc:
                        if ((iarg & 0xFFFFFF00) != 0) {
                            environment.throwErrorException("err.instr.arg.long", opc.parseKey(), iarg);
                        }
                        break;
                }
                switch (opcLen) {
                    case 1:
                        return;
                    case 2:
                        out.writeByte(iarg);
                        return;
                    case 3:
                        out.writeShort(iarg);
                        return;
                    case 4: // opc_multianewarray only
                        out.writeShort(iarg);
                        iarg = ((Indexer) arg2).cpIndex;
                        out.writeByte(iarg);
                        return;
                    case 5:
                        out.writeInt(iarg);
                        return;
                    default:
                        environment.throwErrorException("err.instr.opc.len", opc.parseKey(), opcLen);
                }
            }
            case WIDE:
                out.writeByte(Opcode.opc_wide.value());
                out.writeByte(opc.value() & 0xFF);
                out.writeShort(arg.cpIndex);
                if (opc == Opcode.opc_iinc_w) {
                    out.writeShort(((Indexer) arg2).cpIndex);
                }
                return;
            case PRIVELEGED:
            case NONPRIVELEGED:
                out.writeByte(opc.value() >> 8);
                out.writeByte(opc.value() & 0xFF);
                return;
            default:
                environment.throwErrorException("err.instr.opc.unknown", opc.parseKey());
        } // end writeSpecCode

    }
} // end Instr

