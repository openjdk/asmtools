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
package org.openjdk.asmtools.lib.helper;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class BruteForceHelper {

    public static HashSet<String> excludeSet = new HashSet<>() {{
        add("SourceDebugExtensionNegative01.class");
        add("SourceDebugExtensionNegative02.class");
        add("SourceDebugExtensionNegative03.class");
        add("LoadableDescriptorsAttributeTest$X.class");
        add("InvalidSourceDebugExtension.class");
        add("CondyNestedResolution.class");
    }};

    public static final String FRESHLY_BUILT_ASMTOOLS = "target/classes";

    private final ClassProvider classProvider;

    private final Map<File, ThreeStringWriters> failedJdis = new HashMap<>();
    private final Map<File, ThreeStringWriters> passedJdis = new HashMap<>();
    private final Map<File, ThreeStringWriters> failedJasm = new HashMap<>();
    private final Map<File, ThreeStringWriters> passedJasm = new HashMap<>();
    private final Map<File, ThreeStringWriters> failedLoad = new HashMap<>();
    private final Map<File, ThreeStringWriters> passedLoad = new HashMap<>();
    private final File compileDir;

    public BruteForceHelper() throws IOException {
        this(FRESHLY_BUILT_ASMTOOLS);
    }

    public BruteForceHelper(String dir) throws IOException {
        this(new SearchingClassProvider(new File(dir)));
    }

    public BruteForceHelper(ClassProvider cp) throws IOException {
        compileDir = Files.createTempDirectory("JdisJasmWorks").toFile();
        compileDir.deleteOnExit();
        classProvider = cp;
    }

    public void work(AsmToolsExecutable diasm, AsmToolsExecutable asm) throws IOException {
        List<File> classes = classProvider.getClasses();
        tryAll(classes, failedJdis, passedJdis, diasm);
        diasm.ensure(classes, failedJdis);

        tryAll(classes, failedJasm, passedJasm, asm);
        asm.ensure(classes, failedJasm);

        AsmToolsExecutable loadClass = new AsmToolsExecutable() {
            @Override
            public int run(ThreeStringWriters outs, File clazz) throws IOException {
                try {
                    URL url = compileDir.toURI().toURL();
                    URL[] urls = new URL[]{url};
                    URLClassLoader cl = new URLClassLoader(urls);
                    String origFile = clazz.toString();
                    String fqn = origFile.substring(origFile.indexOf("org/openjdk")).
                            replaceFirst("\\.class$", "").
                            replaceAll("/", ".");
                    cl.loadClass(fqn);
                    return 0;
                } catch (Exception e) {
                    e.printStackTrace(outs.getToolOutput());
                    return 1;
                }
            }

            @Override
            public void ensure(List<File> all, Map<File, ThreeStringWriters> failures) {
                Assertions.assertEquals(0, failedLoad.size(), "from " + classes.size() +
                        " failed to produce valid bytecode " + failedLoad.size() + ": " + keySetToString(failedLoad, getClassesRoot()));
            }
        };
        tryAll(classes, failedLoad, passedLoad, loadClass);
    }

    public static String keySetToString(Map<File, ThreeStringWriters> failedJdis, File classesRoot) {
        return failedJdis.keySet().stream().map(f -> f.getAbsolutePath().replaceFirst(classesRoot.getAbsolutePath(), "")).collect(Collectors.joining(", "));
    }

    private void tryAll(List<File> classes, Map<File, ThreeStringWriters> failed, Map<File, ThreeStringWriters> passed, AsmToolsExecutable ex) throws IOException {
        for (File clazz : classes) {
            ThreeStringWriters outs = new ThreeStringWriters();
            int i = ex.run(outs, clazz);
            outs.flush();
            if (i != 0) {
                Object o = failed.put(clazz, outs);
                Assertions.assertNull(o, " duplicated class - " + o);
                System.out.printf("File %s failed\n", clazz);
                continue;
            }
            Object o = passed.put(clazz, outs);
            Assertions.assertNull(o, " duplicated class - " + o);
        }
        for (Map.Entry<File, ThreeStringWriters> failure : failed.entrySet().stream().sorted(new Comparator<Map.Entry<File, ThreeStringWriters>>() {
            @Override
            public int compare(Map.Entry<File, ThreeStringWriters> t0, Map.Entry<File, ThreeStringWriters> t1) {
                return t0.getKey().compareTo(t1.getKey());
            }
        }).collect(Collectors.toList())) {
            System.err.println(failure.getKey());
            System.err.println(failure.getValue().getErrorBos());
            System.err.println(failure.getValue().getLoggerBos());
            System.err.println(failure.getValue().getToolBos());
        }
    }

    private static List<File> findClasses(File classesRoot) throws IOException {
        List<File> classes = new ArrayList<>();
        Files.walkFileTree(classesRoot.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                if (path.toString().endsWith(".class") && !excludeSet.contains(path.toFile().getName())) {
                    classes.add(path.toFile().getAbsoluteFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Assertions.assertNotEquals(0, classes.size(), "There must be more then 0 class compiled in " + classesRoot + " before running this tests!");
        Collections.sort(classes);
        return classes;
    }

    public String getDecompiledClass(File clazz) {
        return passedJdis.get(clazz).getToolBos();
    }

    public File getCompileDir() {
        return compileDir;
    }

    public File getClassesRoot() {
        return classProvider.getClassesRoot();
    }

    public static void createMetadata(ThreeStringWriters outs, File clazz, File savedAsm, File compileDir, File classesRoot) {
        outs.getToolOutput().println("Orig: " + clazz.getAbsolutePath());
        outs.getToolOutput().println("To: " + compileDir.getAbsolutePath());
        outs.getToolOutput().println("Base: " + classesRoot.getAbsolutePath());
        outs.getToolOutput().println("From: " + savedAsm.getAbsolutePath());
    }

    public static File saveDecompiledCode(String body, String tmpPRefix) throws IOException {
        File savedFresh = File.createTempFile(tmpPRefix, ".java");
        Files.writeString(savedFresh.toPath(), body);
        savedFresh.deleteOnExit();
        return savedFresh;
    }

    public static class SearchingClassProvider implements ClassProvider {
        private final File root;

        public SearchingClassProvider(File root) {
            this.root = root.getAbsoluteFile();
        }

        @Override
        public File getClassesRoot() {
            return root;
        }

        @Override
        public List<File> getClasses() throws IOException {
            return findClasses(root);
        }
    }

    public interface ClassProvider {

        File getClassesRoot();

        List<File> getClasses() throws IOException;
    }

    public interface AsmToolsExecutable {

        int run(ThreeStringWriters out, File clazz) throws IOException;

        void ensure(List<File> all, Map<File, ThreeStringWriters> failures);
    }

}
