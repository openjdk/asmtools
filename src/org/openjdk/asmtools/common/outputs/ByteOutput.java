package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class ByteOutput extends NamedToolOutput {
    private final ArrayList<NamedBinary> outputs = new ArrayList<>();
    private ByteArrayOutputStream currentClass;

    public ArrayList<NamedBinary> getOutputs() {
        return outputs;
    }

    @Override
    public String toString() {
        return outputs.stream().map(a -> a.toString()).collect(Collectors.joining("\n"));
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(currentClass);
    }


    @Override
    public void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException {
        super.startClass(fqn, suffix, logger);
        currentClass = new ByteArrayOutputStream(1024);
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        if (!getCurrentClassName().equals(fqn)) {
            throw new RuntimeException("Ended different class - " + fqn + " - then started - " + super.fqn);
        }
        outputs.add(new NamedBinary(fqn, currentClass.toByteArray()));
        super.fqn = null;
        currentClass = null;

    }

    @Override
    public void printlns(String line) {
        try {
            currentClass.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void prints(String line) {
        try {
            currentClass.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void prints(char line) {
        currentClass.write(line);
    }

    @Override
    public void flush() {

    }

    public class NamedBinary {
        private final String fqn;
        private final byte[] body;

        public NamedBinary(String fqn, byte[] body) {
            this.fqn = fqn;
            this.body = body;
        }

        public String getFqn() {
            return fqn;
        }

        public byte[] getBody() {
            return body;
        }

        @Override
        public String toString() {
            return fqn + ": " + body.length + "b";
        }
    }
}
