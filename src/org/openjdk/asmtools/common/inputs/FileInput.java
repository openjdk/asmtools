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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

public class FileInput implements ToolInput {
    private final String file;

    public FileInput(String file) {
        this.file = file;
    }

    @Override
    public String getFileName() {
        return file;
    }

    public Collection<String> readAllLines() throws IOException {
        return Files.readAllLines(Paths.get(getFileName()));
    }

    @Override
    public DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException {
        try {
            return new DataInputStream(new FileInputStream(this.getFileName()));
        } catch (IOException ex) {
            if (this.getFileName().matches("^[A-Za-z]+:.*")) {
                try {
                    final URI uri = new URI(this.getFileName());
                    final URL url = uri.toURL();
                    final URLConnection conn = url.openConnection();
                    conn.setUseCaches(false);
                    return new DataInputStream(conn.getInputStream());
                } catch (URISyntaxException | IOException exception) {
                    if (logger.isPresent()) {
                        logger.get().error("err.cannot.read", this.getFileName());
                    }
                    throw exception;
                }
            } else {
                throw ex;
            }
        }
    }

    @Override
    public String toString() {
        return getFileName();
    }
}
