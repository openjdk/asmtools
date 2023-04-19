/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileUtils {

    public static File getResourceFilePath(Class<?> cls, String relativePath) {
        return new File(cls.getResource(relativePath).getFile());
    }

    public static byte[] getResourceFile(String s) throws IOException {
        byte[] bytes;
        try (InputStream is = FileUtils.class.getResourceAsStream(s)) {
            bytes = is.readAllBytes();
            Assertions.assertNotNull(bytes);
        } catch (Exception ex) {
            System.err.println("Can't get resource file " + s);
            throw ex;
        }
        return bytes;
    }

    public static byte[] getBinaryFile(File file) throws ClassNotFoundException {
        long byteCount = file.length();
        byte[] bytes = new byte[(int) byteCount];
        try {
            FileInputStream f = new FileInputStream(file);
            f.read(bytes);
            f.close();
        } catch (Exception e) {
            throw new ClassNotFoundException();
        }
        Assertions.assertNotNull(bytes);
        return bytes;
    }

    public static String getStringFile(File file) throws ClassNotFoundException {
        byte[] bytes = getBinaryFile(file);
        String str = new String(bytes, StandardCharsets.UTF_8);
        Assertions.assertNotNull(str);
        return str;
    }

    public static Optional<Path> findFile(String root, String fileName, Consumer<String> printer) {
        try (
                Stream<Path> stream = Files.find(Paths.get(root), 20,
                        (path, attr) -> path.toString().endsWith(fileName))) {
            return stream.findAny();
        } catch (IOException e) {
            printer.accept(e.getMessage());
        }
        return Optional.empty();
    }
}
