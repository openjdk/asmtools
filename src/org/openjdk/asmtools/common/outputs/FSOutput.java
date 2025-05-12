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
package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * A concrete implementation of {@link NamedToolOutput} that writes output to a file system.
 *
 * @author Leonid Kuskov
 */
public class FSOutput extends NamedToolOutput {

    private FSDestination destination;
    private File dir;
    private File file;
    private FileOutputStream fos;
    private PrintWriter pw;

    private static final String fileSeparator = FileSystems.getDefault().getSeparator();

    public FSOutput() {
    }

    @Override
    public boolean isReady() {
        return pw != null;
    }

    public FSOutput setFile(File dir, String file) {
        if (this.dir == null)
            this.dir = dir;
        destination = FSDestination.FILE;
        setDestinationFileName(file);
        return this;
    }

    public FSOutput setDir(File dir) {
        this.dir = dir;
        if (destination == null) {
            destination = FSDestination.DIR;
        }
        return this;
    }

    public File getDir() {
        return dir;
    }

    @Override
    public String toString() {
        String str = super.toString();
        if (dir != null && file != null) {
            str.concat(dir != null ? dir.toString() : "").
                    concat(fileSeparator).
                    concat(file != null ? file.toString() : "");
        }
        return str;
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

    /**
     * Starts writing a new class file with the specified fully qualified name and optional file extension.
     *
     * @param fullyQualifiedName the fully qualified name of the class
     * @param fileExtension      the optional file extension
     * @param environment        the current environment
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void startClass(String fullyQualifiedName, Optional<String> fileExtension, Environment environment) throws IOException {
        super.startClass(fullyQualifiedName, fileExtension, environment);
        String packageName, fileName;
        int index = fullyQualifiedName.lastIndexOf(fileSeparator);
        if (index != -1) {
            packageName = environment.isIgnorePackage() ? "" : fullyQualifiedName.substring(0, index);
            fileName = fullyQualifiedName.substring(index + 1);
        } else {
            packageName = "";
            fileName = fullyQualifiedName;
        }
        /**
         * The destinationFileName is used to form the filename of the output.
         * 1. File FILENAME or class file CLASSNAME takes the highest priority. This filename cannot be overridden.
         * 2. Public class CLASSNAME { }– class name is CLASSNAME, and this CLASSNAME will be used to generate the filename (i.e., CLASSNAME.class).
         * 3. this_class – The filename will be CLASSNAME.class, but the class name will be this_class.
         *
         * Also, if the -f option is used and the number of processed class files is more than 1,
         * then destinationFileName will only apply to the first input file only.
         * For example, if you run jdis -w . -f FILE input.class input2.class, then in the output directory,
         * the files FILE.jasm and input2.jasm will be generated.
         */
        String destinationFileName = this.getDestinationFileName();
        if (destinationFileName != null) {
            fileName = destinationFileName + fileExtension.orElse("");
        } else {
            fileName = fileName + fileExtension.orElse("");
        }
        if (dir == null) {
            environment.traceln("writing to %s %s".formatted(packageName, fileName));
            file = new File(packageName, fileName);
        } else {
            file = Paths.get(dir.getPath(), packageName, fileName).toFile();
            environment.traceln(() -> "writing -d %s = \"%s\"".formatted(dir.getPath(), file.getAbsolutePath()));
            File outDir = new File(file.getParent());
            if (!outDir.exists() && !outDir.mkdirs()) {
                environment.error("err.cannot.create", outDir.getPath());
                return;
            }
        }
        fos = new FileOutputStream(file);
        pw = new PrintWriter(new OutputStreamWriter(fos));
    }

    @Override
    public void finishClass(String fullyQualifiedName) throws IOException {
        super.finishClass(fullyQualifiedName);
        flush();
        try {
            if (pw != null) {
                pw.close();
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

    }

    @Override
    public void flush() {
        if (fos != null) {
            try {
                fos.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(fos));
    }

    @Override
    public String getName() {
        return ( file !=null  ) ? file.toString() : "file stream";
    }

    public enum FSDestination {
        FILE, DIR;
    }
}
