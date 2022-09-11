package org.openjdk.asmtools.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;


public interface ToolOutput {

    String getCurrentClassName();

    void startClass(String fqn) throws IOException;

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
    }

    /**
     * Historically, the output loggers for compilers had two stderrs, one to sdout and secon to stderr.
     * That should be removed, in favour of just dualstream tool output, printing output to stdout and log into stderr
     */
    public abstract class NamedToolOutput implements ToolOutput {
        private String fqn;

        @Override
        public String getCurrentClassName() {
            return fqn;
        }

        @Override
        public void startClass(String fqn) throws IOException {
            this.fqn = fqn;
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            this.fqn = null;
        }
    }


    public abstract class NamedDualStreamToolOutput implements DualStreamToolOutput {
        private String fqn;

        @Override
        public String getCurrentClassName() {
            return fqn;
        }

        @Override
        public void startClass(String fqn) throws IOException {
            this.fqn = fqn;
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            this.fqn = null;
        }
    }


    public static class DirOutput extends NamedToolOutput {

        private final String dir;

        public DirOutput(String dir) {
            this.dir = dir;
        }

        @Override
        public String toString() {
            return super.toString() + " to " + dir;
        }

        @Override
        public void printlns(String line) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public void prints(String line) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public void prints(char line) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public void startClass(String fqn) throws IOException {
            super.startClass(fqn);
            //mkdir
            //open file?

        }

        @Override
        public void finishClass(String fqn) throws IOException {
            super.finishClass(fqn);
        }

        @Override
        public void flush() {
            //todo flush to file
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
    }

    public static class EscapedPrintStreamOutput extends PrintWriterOutput {

        public EscapedPrintStreamOutput(OutputStream os) {
            super(new uEscWriter(os));
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
}

