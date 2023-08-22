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

/**
 * Indexer a position starting from 0 of a constant cell in the Constant Pool.
 * Also, the class is used to hold uninitialized (isSet == false) and initialized (isSet == true) indexes,counters:
 * max_stack, max_locals, Trap.start_pc, Trap.end_pc
 */
public class Indexer {

    public static final int NotSet = -1;
    protected int cpIndex;

    Indexer() {
        cpIndex = NotSet;
    }

    Indexer(int cpIndex) {
        this.cpIndex = cpIndex;
    }

    public int hashCode() { return isSet() ? cpIndex : 0; }

    boolean isSet() {  return cpIndex != NotSet; }

    boolean inRange(int index){
        return index >=0 && index < cpIndex;
    }

    // Alias for max_stack, max_locals
    public int value() {
        return cpIndex;
    }

}
