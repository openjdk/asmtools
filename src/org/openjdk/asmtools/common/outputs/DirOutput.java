package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.Environment;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.Optional;

public class DirOutput extends NamedToolOutput {

    private final File dir;
    private File outfile;
    private FileOutputStream fos;
    private PrintWriter pw;

    public DirOutput(File dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        return super.toString() + " to " + dir;
    }

    @Override
    public void printlns(String line) {
        pw.println(line);
    }

    @Override
    public void prints(String line) {
        pw.print(line);
    }

    @Override
    public void prints(char line) {
        pw.print(line);
    }

    @Override
    public void startClass(String fqn, Optional<String> fileExtension, Environment environment) throws IOException {
        super.startClass(fqn, fileExtension, environment);
        final String fileSeparator = FileSystems.getDefault().getSeparator();
        if (dir == null) {
            int startOfName = fqn.lastIndexOf(fileSeparator);
            if (startOfName != -1) {
                fqn = fqn.substring(startOfName + 1);
            }
            outfile = new File(fqn + fileExtension.orElseGet(() -> ""));
        } else {
            environment.traceln("writing -d " + dir.getPath());
            if (!fileSeparator.equals("/")) {
                fqn = fqn.replace("/", fileSeparator);
            }
            outfile = new File(dir, fqn + fileExtension.orElseGet(() -> ""));
            File outDir = new File(outfile.getParent());
            if (!outDir.exists() && !outDir.mkdirs()) {
                environment.error("err.cannot.write", outDir.getPath());
                return;
            }
        }
        fos = new FileOutputStream(outfile);
        pw = new PrintWriter(new OutputStreamWriter(fos));
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        super.finishClass(fqn);
        flush();
        try {
            pw.close();
        } finally {
            fos.close();
        }

    }

    @Override
    public void flush() {
        try {
            fos.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        return new DataOutputStream(new BufferedOutputStream(fos));
    }
}
