package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class TextOutput extends NamedToolOutput {
    private final ArrayList<NamedSource> outputs = new ArrayList<>();
    private StringBuilder currentClass;

    public ArrayList<NamedSource> getOutputs() {
        return outputs;
    }

    @Override
    public String toString() {
        return outputs.stream().map(a -> a.toString()).collect(Collectors.joining("\n"));
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return null; //If you are here, you probbaly wanted ToolOutput.ByteOutput for assmbled binary output
    }

    @Override
    public void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException {
        super.startClass(fqn, suffix, logger);
        currentClass = new StringBuilder();
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        if (!getCurrentClassName().equals(fqn)) {
            throw new RuntimeException("Ended different class - " + fqn + " - then started - " + super.fqn);
        }
        outputs.add(new NamedSource(fqn, currentClass.toString()));
        super.fqn = null;
        currentClass = null;

    }

    @Override
    public void printlns(String line) {
        currentClass.append(line).append("\n");
    }

    @Override
    public void prints(String line) {
        currentClass.append(line);
    }

    @Override
    public void prints(char line) {
        currentClass.append(line);
    }

    @Override
    public void flush() {

    }

    public class NamedSource {
        private final String fqn;
        private final String body;

        public NamedSource(String fqn, String body) {
            this.fqn = fqn;
            this.body = body;
        }

        public String getFqn() {
            return fqn;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "/**********\n" + fqn + "\n**********/\n" + body + "\n/*end of " + fqn + "*/";
        }
    }
}
