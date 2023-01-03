package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;
import org.openjdk.asmtools.common.NotImplementedException;
import org.openjdk.asmtools.common.uEscWriter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;


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

