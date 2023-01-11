package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;



public interface ToolOutput {

    DataOutputStream getDataOutputStream() throws FileNotFoundException;

    String getCurrentClassName();

    void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException;

    void finishClass(String fqn) throws IOException;

    void printlns(String line);

    void prints(String line);

    void prints(char line);

    void flush();


    public static String exToString(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(out, true, StandardCharsets.UTF_8));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}

