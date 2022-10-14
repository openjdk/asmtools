package org.openjdk.asmtools;

public class TestedHelloWorld {

    private static final int SOME_PRIMTIVE_CONSTANT=666;
    private static int somePrimtiveField = 999;

    private static final TestedHelloWorld SOME_OBJECT_CONSTANT=new TestedHelloWorld();
    private static TestedHelloWorld someObjectField = new TestedHelloWorld();

    private String privateMethod() {
        return "hello1";
    }

    protected String protectedMethod() {
        return "hello2";
    }

    public String publicMethod() {
        return "hello3";
    }

    private static String privateUtilityMethod() {
        return "hello4";
    }

    public static String publicUtilityMethod() {
        return "hello5";
    }

}
