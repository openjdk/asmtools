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
package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.Optional;

public class DirOutput extends NamedToolOutput {

    private final File dir;
    private File outfile;
    private FileOutputStream fos;
    private PrintWriter pw;

    public DirOutput(File dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        return super.toString() + " to " + dir;
    }

    @Override
    public void printlns(String line) {
        pw.println(line);
    }

    @Override
    public void prints(String line) {
        pw.print(line);
    }

    @Override
    public void prints(char line) {
        pw.print(line);
    }

    @Override
    public void startClass(String fqn, Optional<String> fileExtension, Environment environment) throws IOException {
        super.startClass(fqn, fileExtension, environment);
        final String fileSeparator = FileSystems.getDefault().getSeparator();
        if (dir == null) {
            int startOfName = fqn.lastIndexOf(fileSeparator);
            if (startOfName != -1) {
                fqn = fqn.substring(startOfName + 1);
            }
            outfile = new File(fqn + fileExtension.orElseGet(() -> ""));
        } else {
            environment.traceln("writing -d " + dir.getPath());
            fqn = fqn.replace("/", fileSeparator);
             outfile = new File(dir, fqn + fileExtension.orElseGet(() -> ""));
            File outDir = new File(outfile.getParent());
            if (!outDir.exists() && !outDir.mkdirs()) {
                environment.error("err.cannot.write", outDir.getPath());
                return;
            }
        }
        fos = new FileOutputStream(outfile);
        pw = new PrintWriter(new OutputStreamWriter(fos));
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        super.finishClass(fqn);
        flush();
        try {
            pw.close();
        } finally {
            fos.close();
        }

    }

    @Override
    public void flush() {
        try {
            fos.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(fos));
    }
}
