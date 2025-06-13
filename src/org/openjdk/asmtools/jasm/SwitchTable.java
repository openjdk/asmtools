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


import java.io.IOException;
import java.util.ArrayList;

/**
 *  tableswitch
 *      <0-3 byte pad>
 *      defaultbyte1
 *      defaultbyte2
 *      defaultbyte3
 *      defaultbyte4
 *      lowbyte1
 *      lowbyte2
 *      lowbyte3
 *      lowbyte4
 *      highbyte1
 *      highbyte2
 *      highbyte3
 *      highbyte4
 *      jump offsets...
 *
 *  tableswitch
 *   default:   <default_offset> u4
 *   low:       <low_value>      u4
 *   high:      <high_value>     u4
 *   jump offsets:
 *          <offset_1>
 *          <offset_2>
 *     ...
 *          <offset_n>
 *  or
 *  lookupswitch
 *      <0-3 byte pad>
 *      defaultbyte1
 *      defaultbyte2
 *      defaultbyte3
 *      defaultbyte4
 *      npairs1
 *      npairs2
 *      npairs3
 *      npairs4
 *      match-offset pairs...
 *
 *  lookupswitch
 *   default:       <default_offset>        u4
 *   match_count:   <number_of_cases>       u4
 *          match_1:    <value_1> <case_offset_1>
 *          match_2:    <value_2> <case_offset_2>
 *          ...
 *          match_n:    <value_n> <case_offset_n>
 * */
class SwitchTable {

    Indexer defLabel = null;
    ArrayList<Indexer> labels = new ArrayList<>();
    ArrayList<Integer> keys = new ArrayList<>();

    // for tableswitch:
    Indexer[] resLabels;
    int high, low;

    int pc, pad;
    JasmEnvironment environment;

    SwitchTable(JasmEnvironment environment) {
        this.environment = environment;
    }

    void addEntry(int key, Indexer label) {
        keys.add(key);
        labels.add(label);
    }

    // for lookupswitch:
    int calcLookupSwitch(int pc) {
        this.pc = pc;
        pad = ((3 - pc) & 0x3);
        int len = 1 + pad + (keys.size() + 1) * 8;
        if (defLabel == null) {
            defLabel = new Indexer(pc + len);
        }
        return len;
    }

    void writeLookupSwitch(CheckedDataOutputStream out) throws IOException {
        environment.traceln(() -> "  writeLookupSwitch: pc=" + pc + " pad=" + pad + " deflabel=" + defLabel.cpIndex);
        int k;
        for (k = 0; k < pad; k++) {
            out.writeByte(0);
        }
        out.writeInt(defLabel.cpIndex - pc);
        out.writeInt(keys.size());
        for (k = 0; k < keys.size(); k++) {
            out.writeInt(keys.get(k));
            out.writeInt((labels.get(k)).cpIndex - pc);
        }
    }

    int recalcTableSwitch(int pc) {
        int k;
        int numpairs = keys.size();
        int high1 = Integer.MIN_VALUE, low1 = Integer.MAX_VALUE;
        int numslots = 0;
        if (numpairs > 0) {
            for (k = 0; k < numpairs; k++) {
                int key = keys.get(k);
                if (key > high1) {
                    high1 = key;
                }
                if (key < low1) {
                    low1 = key;
                }
            }
            numslots = high1 - low1 + 1;
        }
        environment.traceln("  recalcTableSwitch: low=%d high=%d".formatted(low1, high1));
        this.pc = pc;
        pad = ((3 - pc) & 0x3);
        int len = 1 + pad + (numslots + 3) * 4;
        if (defLabel == null) {
            defLabel = new Indexer(pc + len);
        }
        Indexer[] resLabels1 = new Indexer[numslots];
        for (k = 0; k < numslots; k++) {
            resLabels1[k] = defLabel;
        }
        for (k = 0; k < numpairs; k++) {
            environment.traceln("   keys.data[%d]=%s".formatted(k, keys.get(k)));
            resLabels1[keys.get(k) - low1] = labels.get(k);
        }
        this.resLabels = resLabels1;
        this.labels = null;
        this.keys = null;
        this.high = high1;
        this.low = low1;
        return len;
    }

    void writeTableSwitch(CheckedDataOutputStream out) throws IOException {
        int k;
        for (k = 0; k < pad; k++) {
            out.writeByte(0);
        }
        out.writeInt(defLabel.cpIndex - pc);
        out.writeInt(low);
        out.writeInt(high);
        for (k = 0; k < resLabels.length; k++) {
            out.writeInt(resLabels[k].cpIndex - pc);
        }
    }
}
