package org.openjdk.asmtools;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.regex.Pattern;

public class ClassPathClassWork {

    protected static Class clazz;
    protected static String classFile;
    protected static Pattern className;
    protected static Pattern packageName;

    public static void initClassData(Class testsClass) {
        clazz = testsClass;
        classFile = "./target/classes/" + clazz.getName().replace('.', '/') + ".class";
        Assertions.assertTrue(new File(classFile).exists());
        className  = Pattern.compile("public .*class .*" + clazz.getSimpleName() + " extends .*");
        packageName = Pattern.compile("package "+clazz.getPackageName() + ";");
    }

}
