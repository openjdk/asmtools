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
package org.openjdk.asmtools.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class uEscWriter extends Writer {

    static final char[] hexTable = "0123456789ABCDEF".toCharArray();
    OutputStream out;
    byte[] tmpl;

    public uEscWriter(OutputStream out) {
        this.out = out;
        tmpl = new byte[6];
        tmpl[0] = (byte) '\\';
        tmpl[1] = (byte) 'u';
    }


    @Override
    public synchronized void write(char[] cc, int ofs, int len) throws IOException {
        byte[] b = String.copyValueOf(cc, ofs, len).getBytes(StandardCharsets.UTF_8);
        out.write(b, ofs, b.length);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
