/*
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInputStream;
import java.io.IOException;

public class AttrData {

    private boolean corrupted = false;
    private int name_cpx;

    private int length;
    private byte[] data;
    final JdisEnvironment environment;

    public AttrData(JdisEnvironment environment) {
        this.environment = environment;
    }

    public void read(int name_cpx, int attrLength, DataInputStream in) throws IOException {
        this.name_cpx = name_cpx;
        this.length = attrLength;
        try {
            data = new byte[attrLength];
            in.readFully(data);
            environment.traceln("AttrData:#%d length=%d", name_cpx, attrLength);
        } catch (NegativeArraySizeException | ArrayIndexOutOfBoundsException ex) {
            corrupted = true;
            environment.traceln("Corrupted AttrData:#%d length=%d", name_cpx, attrLength);
        }
    }

    public boolean isCorrupted() {
        return corrupted;
    }

    public AttrData setNameCpx(int name_cpx) {
        this.name_cpx = name_cpx;
        return this;
    }

    public AttrData setLength(int length) {
        this.length = length;
        return this;
    }

    public int getNameCpx() {
        return name_cpx;
    }

    public int getLength() {
        return length;
    }
}
