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
package org.openjdk.asmtools.lib.transform;

import org.junit.jupiter.api.Assertions;
import org.openjdk.asmtools.lib.action.EAsmTools;
import org.openjdk.asmtools.common.FileUtils;
import org.openjdk.asmtools.common.inputs.ByteInput;
import org.openjdk.asmtools.common.inputs.ToolInput;
import org.openjdk.asmtools.common.outputs.ByteOutput;
import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.log.StderrLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.nio.file.StandardOpenOption.*;
import static org.openjdk.asmtools.lib.action.EAsmTools.*;
import static org.openjdk.asmtools.common.FileUtils.findFile;

public class TransformLoader extends ClassLoader {

    public enum TransformRules {
        CLASS_LOAD,
        JASM_TO_CLASS_LOAD,
        JCOD_TO_CLASS_LOAD,
        // 2 rules are tightened by the restriction -
        // a class, jasm and jacob files are placed in the same directory.
        CLASS_TO_JASM_TO_CLASS_LOAD,
        CLASS_TO_JCOD_TO_CLASS_LOAD }

    static {
        registerAsParallelCapable();
    }

    public TransformLoader setToolsOptions(EAsmTools tool, String... options) {
        if( options != null && options.length > 0 ) {
            toolsOptions.putIfAbsent(tool, options);
        }
        return this;
    }

    public TransformLoader clearOptions() {
        toolsOptions = new HashMap<>();
        return this;
    }

    Map<EAsmTools,String[]> toolsOptions = new HashMap<>();

    static String MSG_PREFIX = ResultChecker.OUT_LINE_PREFIXES_TO_IGNORE[0];
    private TransformRules transformRule = TransformRules.CLASS_LOAD;

    // List of class names that should be loaded in a general way without transformation
    private final List<String> excludeList = new ArrayList<>();

    // might affect a test result - Must be false once a development is done
    private boolean DEBUG = false;

    // Directory for dumping "problem" files created while transforming
    private String    dumpDir;

    /**
     * Specifies whether to delete an interim jcod/jasm file for rules CLASS_TO_JCOD_TO_CLASS_LOAD,
     * CLASS_TO_JASM_TO_CLASS_LOAD
     */
    boolean deleteInterimFile = false;

    // Should a class be loaded by a custom-ruled loader ?
    // Accept a class name and returns true if the class should be dedicated loaded.
    private Function<String, Boolean> transformFilter = name -> name.contains("org.openjdk.asmtools.transform.case");

    public TransformLoader setTransformFilter(Function<String, Boolean> transformFilter) {
        this.transformFilter = transformFilter;
        return this;
    }

    // Fill up the exclude list of class names that should be loaded in a general way without transformation
    public TransformLoader addToExcludeList(String... fileNames) {
        Collections.addAll(excludeList, fileNames);
        return this;
    }

    public TransformLoader clearExcludeList() {
        excludeList.clear();
        return this;
    }

    public TransformLoader setTransformRule(TransformRules transformRule) {
        this.transformRule = transformRule;
        return this;
    }

    public TransformLoader setDEBUG(boolean DEBUG) {
        this.DEBUG = DEBUG;
        if( dumpDir == null && DEBUG) {
            dumpDir = Paths.get("").toAbsolutePath().toString();
        }
        return this;
    }

    public TransformLoader setDeleteInterimFile(boolean deleteInterimFile) {
        this.deleteInterimFile = deleteInterimFile;
        return this;
    }

    // Directory where class files for loading classes are placed
    private String classDir;

    // Parent class loader for delegation class loading
    protected ClassLoader parentCL;

    public TransformLoader() {
        super();
        parentCL = getSystemClassLoader();
        setClassDir("");
    }

    public TransformLoader(ClassLoader cl) {
        super(cl);
        parentCL = cl;
        setClassDir("");
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedCode;
        println("Trying to load class: " + name + " using rule " + transformRule);
        try {
            // Simple criteria to define what classes should be loaded according to specified transform rule
            if (transformFilter.apply(name)) {
                synchronized (getClassLoadingLock(name)) {
                    loadedCode = findLoadedClass(name);
                    if (loadedCode == null) {
                        String fileName = name.replace('.', File.separatorChar);
                        if(  excludeList.contains(name) ) {
                            loadedCode = loadClassFromClassFile(name, checkFile(fileName + ".class"));
                        } else {
                            loadedCode = switch (transformRule) {
                                case JCOD_TO_CLASS_LOAD -> loadClassFromResourceJcodFile(name);
                                case JASM_TO_CLASS_LOAD -> loadClassFromResourceJasmFile(name);
                                case CLASS_TO_JASM_TO_CLASS_LOAD -> loadClassFromGeneratedJasmFile(name, checkFile(fileName + ".class"));
                                case CLASS_TO_JCOD_TO_CLASS_LOAD -> loadClassFromGeneratedJcodFile(name, checkFile(fileName + ".class"));
                                case CLASS_LOAD -> loadClassFromClassFile(name, checkFile(fileName + ".class"));
                            };
                        }
                    } else {
                        println("Object of the Class " + name + " found");
                    }
                    if (resolve) {
                        resolveClass(loadedCode);
                    }
                }
            } else {
                println("[Delegate " + name + " to default parent.]");
                loadedCode = parentCL.loadClass(name);
            }
        } catch (Exception d) {
            d.printStackTrace();
            throw new ClassNotFoundException();
        }

        if (loadedCode != null) {
            return loadedCode;
        } else {
            throw new ClassNotFoundException();
        }
    }

    public TransformLoader setClassDir(String classDir) {
        this.classDir = Paths.get(classDir).toAbsolutePath().toString();
        return this;
    }

    public TransformLoader setDumpDir(String dumpDir) {
        if( DEBUG ) {
            this.dumpDir = dumpDir;
        }
        return this;
    }


    /**
     * Transition: class file -> jcod file -> class file as byte array in memory -> load
     */
    protected Class<?> loadClassFromGeneratedJcodFile(String className, File classFile) throws ClassNotFoundException, IOException {
        File jcodFile = checkOrCreate(classFile, new File(classFile.toString().concat(JCODER.getFileExtension())));
        byte[] jcodFileBuf = FileUtils.getBinaryFile(jcodFile);
        Class<?> cl = loadJcodBytes(className, jcodFileBuf);
        if (deleteInterimFile && transformRule == TransformRules.CLASS_TO_JCOD_TO_CLASS_LOAD) {
            Files.delete(jcodFile.toPath());
        }
        println("[Loaded " + className + " from file " + jcodFile + " (" + jcodFileBuf.length + " bytes)]");
        return cl;
    }

    /**
     * Transition: Resources jcod file -> class file as byte array in memory -> load
     */
    private Class<?> loadClassFromResourceJcodFile(String className) throws IOException {
        String resourceName = String.format("/%s.class%s", className.replaceAll("\\.", "/"), JCODER.getFileExtension());
        byte[] jcodFileBuf = FileUtils.getResourceFile(resourceName);
        Class<?> cl = loadJcodBytes(className, jcodFileBuf);
        println("[Loaded " + className + " from resource " + resourceName + " (" + jcodFileBuf.length + " bytes)]");
        return cl;
    }

    /**
     * Transition: class file -> jasm file -> class file as byte array in memory -> load
     */
    protected Class<?> loadClassFromGeneratedJasmFile(String className, File classFile) throws ClassNotFoundException, IOException {
        File jasmFile = checkOrCreate(classFile, new File(classFile.toString().concat(JASM.getFileExtension())));
        byte[] jasmFileBuf = FileUtils.getBinaryFile(jasmFile);
        Class<?> cl = loadJasmBytes(className, jasmFileBuf);
        if (deleteInterimFile && transformRule == TransformRules.CLASS_TO_JASM_TO_CLASS_LOAD) {
            Files.delete(jasmFile.toPath());
        }
        println("[Loaded " + className + " from file " + jasmFile + " (" + jasmFileBuf.length + " bytes)]");
        return cl;
    }

    /**
     * Transition: Resources jasm file -> class file as byte array in memory -> load
     */
    private Class<?> loadClassFromResourceJasmFile(String className) throws IOException {
        String resourceName = String.format("/%s.class%s", className.replaceAll("\\.", "/"), JASM.getFileExtension());
        byte[] jasmFileBuf = FileUtils.getResourceFile(resourceName);
        Class<?> cl = loadJasmBytes(className, jasmFileBuf);
        println("[Loaded " + className + " from resource " + resourceName + " (" + jasmFileBuf.length + " bytes)]");
        return cl;
    }

    private Class<?> loadJcodBytes(String className, byte[] jcodFileBuf) {
        ToolInput toolInput = new ByteInput(jcodFileBuf);
        ByteOutput output = new ByteOutput();
        DualStreamToolOutput log = new StderrLog();
        org.openjdk.asmtools.jcoder.Main jcod = toolsOptions.containsKey(JCODER)
                ? new org.openjdk.asmtools.jcoder.Main(output, log, toolInput, toolsOptions.get(JCODER))
                : new org.openjdk.asmtools.jcoder.Main(output, log, toolInput);
        int i = jcod.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(1, output.getOutputs().size());
        byte[] buffer = output.getOutputs().get(0).getBody();
        Class<?> c = defineClass(className, buffer);
        return c;
    }

    private Class<?> loadJasmBytes(String className, byte[] jasmFileBuf) {
        ToolInput toolInput = new ByteInput(jasmFileBuf);
        ByteOutput output = new ByteOutput();
        DualStreamToolOutput log = new StderrLog();
        org.openjdk.asmtools.jasm.Main jasm = toolsOptions.containsKey(JASM)
                ? new org.openjdk.asmtools.jasm.Main(output, log, toolInput, toolsOptions.get(JASM))
                : new org.openjdk.asmtools.jasm.Main(output, log, toolInput);
        int i = jasm.compile();
        Assertions.assertEquals(0, i);
        Assertions.assertEquals(1, output.getOutputs().size());
        byte[] buffer = output.getOutputs().get(0).getBody();
        Class<?> c = defineClass(className, buffer);
        return c;

    }

    private void createJcodFile(File classFile, File jcodFile) throws ClassNotFoundException, IOException {
        final ToolInput toolInput = new ByteInput(FileUtils.getBinaryFile(classFile));
        ByteOutput output = new ByteOutput();
        org.openjdk.asmtools.jdec.Main decoder = toolsOptions.containsKey(JDEC)
                ? new org.openjdk.asmtools.jdec.Main(output, new StderrLog(), toolInput, toolsOptions.get(JDEC))
                : new org.openjdk.asmtools.jdec.Main(output, new StderrLog(), toolInput);
        decoder.decode();
        byte[] buffer = output.getOutputs().get(0).getBody();
        Files.write(jcodFile.toPath(), buffer, WRITE, CREATE, TRUNCATE_EXISTING);
    }

    private void createJasmFile(File classFile, File jasmFile) throws ClassNotFoundException, IOException {
        final ToolInput toolInput = new ByteInput(FileUtils.getBinaryFile(classFile));
        ByteOutput output = new ByteOutput();
        org.openjdk.asmtools.jdis.Main compiler = toolsOptions.containsKey(JDIS)
                ? new org.openjdk.asmtools.jdis.Main(output, new StderrLog(), toolInput, toolsOptions.get(JDIS))
                : new org.openjdk.asmtools.jdis.Main(output, new StderrLog(), toolInput);
        compiler.disasm();
        byte[] buffer = output.getOutputs().get(0).getBody();
        Files.write(jasmFile.toPath(), buffer, WRITE, CREATE, TRUNCATE_EXISTING);
    }


    protected Class<?> loadClassFromClassFile(String name, File file) throws ClassNotFoundException {
        byte[] buffer = FileUtils.getBinaryFile(file);
        long byteCount = buffer.length;
        Class<?> c = defineClass(name, buffer);
        println("[Loaded " + name + " from " + file + " (" + byteCount + " bytes)]");
        return c;
    }

    private Class<?> defineClass(String name, byte[] buffer) {
        Class<?> c;
        long byteCount = buffer.length;
        try {
            c = defineClass(name, buffer, 0, (int) byteCount);
        } catch (ClassCircularityError e) {
            printError("ClassCircularityError is caught!");
            throw e;
        } catch (LinkageError e) {
            println("Linkage error during defining class \"" + name + "\": ");
            printError( e.getClass().getName() + ": " + e.getMessage());
            // dump binaries
            if( DEBUG ) {
                int idx = name.lastIndexOf('.');
                Path dumpFile = Path.of(dumpDir, name.substring(idx == -1 ? 0 : idx+1) + ".class.dump");
                try {
                    Files.write(dumpFile, buffer, CREATE, WRITE, TRUNCATE_EXISTING);
                } catch (IOException ex) {
                    printError("Cannot write ato dump file \"" + dumpFile + "\": " + ex.getMessage());
                }
            }
            throw e;
        }
        return c;
    }

    private File checkFile(String fileName) throws FileNotFoundException {
        File file = new File(classDir + File.separator + fileName);
        if (file.exists()) {
            return file;
        }
        Optional<Path> filePath = findFile(classDir, fileName, this::println);
        if (filePath.isEmpty()) {
            printError("Can't find file: " + file);
            throw new java.io.FileNotFoundException();
        }
        return filePath.get().toFile();
    }

    private File checkOrCreate(File classFile, File toolFile) throws ClassNotFoundException, IOException {
        if (!toolFile.exists() || deleteInterimFile) {
            if (toolFile.getName().endsWith(JCODER.getFileExtension()))
                createJcodFile(classFile, toolFile);
            else if (toolFile.getName().endsWith(JASM.getFileExtension()))
                createJasmFile(classFile, toolFile);
            else
                throw new RuntimeException("A tool is not defined to create the file " + toolFile);
        }
        return toolFile;
    }

    /**
     * Print error message with the prefix to filter out while analysing System error log
     */
    private void printError(String s) {
        System.err.println( (DEBUG ? MSG_PREFIX : "") + s);
        System.err.flush();
    }

    /**
     * Print debug message with the prefix to filter out while analysing System output log
     */
    private void println(String s) {
        if (DEBUG) {
            System.out.printf("%s %s: %s%n", MSG_PREFIX, Thread.currentThread().getName(), s);
            System.out.flush();
        }
    }
}
