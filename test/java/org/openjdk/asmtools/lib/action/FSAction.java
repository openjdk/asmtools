/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.asmtools.lib.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.fail;

public class FSAction {

    public static Function<String, Path> getTmpPath = subDir ->
            Path.of(System.getProperty("user.home"), "tmp", subDir);

    // -d <directory>        Specify where to place generated class files, otherwise <stdout>
    // -w <directory>        Specify where to place generated class files, without considering the classpath, otherwise <stdout>
    private boolean ignorePackage = false;
    private Path destDir = null;
    private boolean deleteOnExit = false;


    /**
     * @param destDir
     * @return this class instance
     */
    public FSAction setupDestDir(Path destDir) {
        if (destDir != null) {
            if (this.destDir == null) {
                this.destDir = createDestDir(destDir);
            } else if (!this.destDir.equals(destDir)) {
                if (!deleteOnExit) {
                    delDestDir();
                    this.destDir = createDestDir(destDir);
                } else {
                    this.destDir = createDestDir(destDir);
                    this.destDir.toFile().deleteOnExit();
                }
            }
        } else {
            // Deletes the previously set destDir and deactivates it.
            if (this.destDir != null && !deleteOnExit) {
                delDestDir();
            }
            this.destDir = null;
        }
        return this;
    }

    public FSAction setupDestDir() {
        this.destDir = createDestDir();
        return this;
    }

    public Path createDestDir() {
        if (this.destDir == null) {
            try {
                this.destDir = Files.createTempDirectory("asmtools-tests").toAbsolutePath();
                this.destDir.toFile().deleteOnExit();
                deleteOnExit = true;
            } catch (IOException e) {
                fail("Unable to create temporary directory");
            }
        }
        return this.destDir;
    }

    private Path createDestDir(Path destDir) {
        if (!Files.exists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException e) {
                fail("Unable to create destination directory %s".formatted(destDir.toString()));
            }
        }
        return destDir;
    }

    private FSAction delDestDir() {
        if (this.destDir != null && Files.exists(this.destDir)) {
            // Cleanup: Delete temporary directory and its contents
            try {
                Files.walk(this.destDir)
                        .map(Path::toFile)
                        .forEach(file -> {
                            System.out.println("Deleting: " + file);
                            file.delete();
                        });
            } catch (IOException e) {
                throw new RuntimeException("Can't clean up the dest dir: %s".formatted(destDir));
            }
        }
        return this;
    }

    public FSAction setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
        return this;
    }

    public Path getDestDir() {
        return destDir;
    }

    public FSAction setIgnorePackage(boolean value) {
        this.ignorePackage = value;
        return this;
    }

    public List<String> getDestDirParams() {
        return (this.destDir == null) ?
                List.of() :
                List.of(
                (this.ignorePackage ? "-w" : "-d"),
                this.destDir.toString());
    }
}
