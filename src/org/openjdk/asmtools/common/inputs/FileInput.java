/*
 * Copyright (c) 2023, Oracle, Red Hat  and/or theirs affiliates. All rights reserved.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Optional;

public class FileInput implements ToolInput {

    private boolean detailedInput = false;
    private final String fileName;
    private MessageDigest md = null;
    private CapacityInputStream cis = null;

    public FileInput(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getName() {
        return fileName;
    }

    public Collection<String> readAllLines() throws IOException {
        return Files.readAllLines(Paths.get(getName()));
    }

    public FileInput setDetailedInput(boolean detailedInput) {
        this.detailedInput = detailedInput;
        return this;
    }

    @Override
    public MessageDigest getMessageDigest() {
        return md;
    }

    @Override
    public int getSize() {
        return cis != null ? cis.size() : 0;
    }

    @Override
    public DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException {
        try {
            FileInputStream fis = new FileInputStream(this.getName());
            if (detailedInput) {
                cis = new CapacityInputStream(fis);
                md = MessageDigest.getInstance("SHA-256");
                DigestInputStream dis = new DigestInputStream(cis, md);
                return new DataInputStream(dis);
            } else {
                return new DataInputStream(fis);
            }
        } catch (IOException ex) {
            if (this.getName().matches("^[A-Za-z]+:.*")) {
                try {
                    final URI uri = new URI(this.getName());
                    final URL url = uri.toURL();
                    final URLConnection conn = url.openConnection();
                    conn.setUseCaches(false);
                    return new DataInputStream(conn.getInputStream());
                } catch (URISyntaxException | IOException exception) {
                    if (logger.isPresent()) {
                        logger.get().error("err.cannot.read", this.getName());
                    }
                    throw exception;
                }
            } else {
                throw ex;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    private static class CapacityInputStream extends FilterInputStream {
        CapacityInputStream(InputStream in) {
            super(in);
        }

        int size() {
            return size;
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            int n = super.read(buf, offset, length);
            if (n > 0)
                size += n;
            return n;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            size += 1;
            return b;
        }

        private int size;
    }
}
