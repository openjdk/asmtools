/*
 * Copyright (c) 2023, 2025, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
package org.openjdk.asmtools.common.inputs;

import org.openjdk.asmtools.common.Environment;

import java.io.*;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ByteInput implements ToolInput {

    private boolean detailedInput = false;
    private MessageDigest md = null;

    //compilers passes input more than one times, so saving it for reuse;
    protected byte[] bytes;

    public ByteInput(final byte[] bytes) {
        this.bytes = bytes;
    }

    protected ByteInput() {
    }

    public byte[] getBytes() {
        return bytes;
    }

    public ByteInput setDetailedInput(boolean detailedInput) {
        this.detailedInput = detailedInput;
        return this;
    }

    @Override
    public MessageDigest getMessageDigest() {
        return md;
    }

    @Override
    public int getSize() {
        return bytes.length;
    }

    @Override
    public String getName() {
        //get parent is used
        return "bytes/bytes";
    }

    @Override
    public String toString() {
        return getName();
    }

    protected void init() {

    }

    @Override
    public DataInputStream getDataInputStream(Optional<Environment> logger) {
        init();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            if (detailedInput) {
                md = MessageDigest.getInstance("SHA-256");
                DigestInputStream dis = new DigestInputStream(bais, md);
                return new DataInputStream(dis);
            } else {
                return new DataInputStream(bais);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> readAllLines() throws IOException {
        init();
        ArrayList resultingLines = new ArrayList();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), "utf-8"))) {
            while (true) {
                String l = br.readLine();
                if (l == null) {
                    break;
                }
                resultingLines.add(l);
            }
        }
        return resultingLines;
    }
}
