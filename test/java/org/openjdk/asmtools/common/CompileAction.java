package org.openjdk.asmtools.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class CompileAction {

    private final File destDir;

    public CompileAction() throws IOException {
        destDir = Files.createTempDirectory("compile").toFile();
        destDir.deleteOnExit();
    }

    public CompileAction(File destDir) {
        this.destDir = destDir;
    }

    public void jasm(List<String> files) {
        action("jasm", files);
    }

    public void jcoder(List<String> files) {
        action("jcoder", files);
    }


    private void action(String toolName, List<String> files) {
        if (files.isEmpty())
            fail(toolName + ": no files");
        List<String> toolArgs = new ArrayList<>();
        toolArgs.add("-d");
        toolArgs.add(destDir.getPath());
        toolArgs.addAll(files);
        try {
            String toolClassName = "org.openjdk.asmtools." + toolName + ".Main";
            Class<?> toolClass = Class.forName(toolClassName);
            Constructor<?> constr = toolClass.getConstructor(PrintStream.class, String.class);
            PrintStream ps = new PrintStream(System.out);
            Object tool = constr.newInstance(ps, toolName);
            Method m = toolClass.getMethod("compile", String[].class);
            Object r = m.invoke(tool, new Object[]{toolArgs.toArray(new String[0])});
            if (r instanceof Boolean) {
                boolean ok = (Boolean) r;
                if (!ok) {
                    fail(toolName + " failed");
                }
                System.out.println(toolName + " OK");
            } else
                fail("unexpected result from " + toolName + ": " + r.toString());
        } catch (ClassNotFoundException e) {
            fail("can't find " + toolName);
        } catch (ReflectiveOperationException t) {
            fail("error invoking " + toolName + ": " + t);
        }
    }
}
