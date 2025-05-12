package org.openjdk.asmtools.lib.helper;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

public class ClassPathClassWork {

    protected static Class clazz;
    protected static String classFile;
    protected static Pattern className;
    protected static Pattern packageName;

    public static void initMainClassData(Class testsClass) {
        initGenericClassData(testsClass,"classes");
    }

    public static void initTestClassData(Class testsClass) {
        initGenericClassData(testsClass,"test-classes");
    }

    private static void initGenericClassData(Class testsClass, String subdir) {
        clazz = testsClass;
        classFile = "./target/"+subdir+"/" + clazz.getName().replace('.', '/') + ".class";
        Assertions.assertTrue(new File(classFile).exists(), "\"%s\" not found".formatted(Paths.get(classFile).toFile().getAbsolutePath()));
        className  = Pattern.compile("public .*class .*" + clazz.getSimpleName() + " extends .*");
        packageName = Pattern.compile("package "+clazz.getPackageName() + ";");
    }

    protected String getFile(String fileResource) {
        File resourceDir = new File(Objects.requireNonNull(this.getClass().getResource(fileResource)).getFile()).getParentFile();
        return resourceDir + File.separator + new File(fileResource).getName();
    }
}
