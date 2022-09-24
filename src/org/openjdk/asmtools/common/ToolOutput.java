package org.openjdk.asmtools.common;

import java.io.BufferedOutputStream;
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
import java.nio.file.FileSystems;
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

    public static interface DualStreamToolOutput extends ToolOutput {
        void printlne(String line);

        void printe(String line);

        void printe(char line);

        void stacktrace(Throwable ex);

        ToolOutput getSToolObject();
        ToolOutput getEToolObject();

        @Override
        default DataOutputStream getDataOutputStream() throws FileNotFoundException {
            throw new NotImplementedException("Not going to happen");
        }
    }

    /**
     * Historically, the output loggers for compilers had two stderrs, one to sdout and secon to stderr.
     * That should be removed, in favour of just dualstream tool output, printing output to stdout and log into stderr
     */
    public abstract class NamedToolOutput implements ToolOutput {
        private String fqn;
        private Optional<String> suffix;
        private Environment environment;

        @Override
        public String getCurrentClassName() {
            return fqn;
        }

        @Override
        public void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException {
            this.fqn = fqn;
            this.suffix = suffix;
            this.environment = logger;
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            this.fqn = null;
        }
    }


    public abstract class NamedDualStreamToolOutput implements DualStreamToolOutput {
        private String fqn;
        private Optional<String> suffix;
        private Environment environment;

        @Override
        public String getCurrentClassName() {
            return fqn;
        }

        @Override
        public void startClass(String fqn, Optional<String> suffix, Environment logger) throws IOException {
            this.fqn = fqn;
            this.suffix = suffix;
            this.environment = logger;
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            this.fqn = null;
        }
    }


    public static class DirOutput extends NamedToolOutput {

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
            //todo flush to file
        }

        @Override
        public DataOutputStream getDataOutputStream() throws FileNotFoundException {
            return new DataOutputStream(new BufferedOutputStream(fos));
        }
    }

    public static class PrintWriterOutput extends NamedToolOutput {

        protected PrintWriter os;

        public PrintWriterOutput(OutputStream os) {
            //although it is usually System.out, it is set from Environment, or custom
            this.os = new PrintWriter(os, true);
        }
        public PrintWriterOutput(Writer os) {
            //although it is usually System.out, it is set from Environment, or custom
            this.os = new PrintWriter(os, true);
        }

        @Override
        public void printlns(String line) {
            os.println(line);
        }

        @Override
        public void prints(String line) {
            os.print(line);
        }

        @Override
        public void prints(char line) {
            os.print(line);
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            super.finishClass(fqn);
            os.flush();
        }

        @Override
        public void flush() {
            os.flush();
        }

        @Override
        public DataOutputStream getDataOutputStream() throws FileNotFoundException {
            throw  new  NotImplementedException("Use EscapedPrintStreamOutput");
        }


    }

    public static class EscapedPrintStreamOutput extends PrintWriterOutput {

        private final OutputStream originalStream;

        public EscapedPrintStreamOutput(OutputStream os) {
            super(new uEscWriter(os));
            this.originalStream = os;
        }

        @Override
        public DataOutputStream getDataOutputStream() throws FileNotFoundException {
            return new DataOutputStream(new BufferedOutputStream(originalStream));
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            super.finishClass(fqn);
            originalStream.flush();
        }
    }

    public static class DualOutputStreamOutput extends NamedDualStreamToolOutput {

        protected PrintWriter os;
        protected PrintWriter es;


        public DualOutputStreamOutput() {
            this(System.out, System.err);
        }

        //todo, remove once tests asdapts
        public DualOutputStreamOutput(PrintWriter os, PrintWriter er) {
            this.os = os;
            this.es = er;
        }
        public DualOutputStreamOutput(PrintStream os, PrintStream er) {
            //although it is usually System.out, it is set from Environment, or custom
            this.os = new PrintWriter(os, true);
            //although it is usually System.err, it is set from Environment, or custom
            this.es = new PrintWriter(er, true);
        }

        @Override
        public void printlns(String line) {
            os.println(line);
        }

        @Override
        public void prints(String line) {
            os.print(line);
        }

        @Override
        public void prints(char line) {
            os.print(line);
        }

        @Override
        public void printlne(String line) {
            es.println(line);
        }

        @Override
        public void printe(String line) {
            es.print(line);
        }

        @Override
        public void printe(char line) {
            es.print(line);
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            super.finishClass(fqn);
            try {
                os.flush();
            } finally {
                es.flush();
            }
        }

        @Override
        public void stacktrace(Throwable ex) {
            ex.printStackTrace(es);
        }

        @Override
        public ToolOutput getSToolObject() {
            return new PrintWriterOutput(os);
        }

        @Override
        public ToolOutput getEToolObject() {
            return new PrintWriterOutput(es);
        }

        @Override
        public void flush() {
            this.os.flush();
            this.es.flush();
        }
    }

    public static class SingleDualOutputStreamOutput extends DualOutputStreamOutput {
        public SingleDualOutputStreamOutput() {
            this(System.err);
        }

        public SingleDualOutputStreamOutput(PrintWriter er) {
            super(er, er);
        }

        public SingleDualOutputStreamOutput(PrintStream er) {
            super(er, er);
        }
    }
}

