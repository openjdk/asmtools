package org.openjdk.asmtools.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;


public interface ToolOutput {

    String getCurrentClassName();

    void startClass(String fqn) throws IOException;

    void finishClass(String fqn) throws IOException;

    void addClassOutputListener(ClassProgressListener l);

    void removeClassOutputListener(ClassProgressListener l);

    void println(String line);
    void print(String line);
    void print(char line);

    public interface ClassProgressListener {

        public void classStarted(String fqn);

        public void classEnded(String fqn);

    }


    public static abstract class ObservableToolOutput implements ToolOutput {

        protected ArrayList<ClassProgressListener> classProgressListeners = new ArrayList<>();
        protected String currentFqn = null;

        @Override
        public String getCurrentClassName() {
            return currentFqn;
        }

        @Override
        public void addClassOutputListener(ClassProgressListener l) {
            classProgressListeners.add(l);
        }

        @Override
        public void removeClassOutputListener(ClassProgressListener l) {
            if (!classProgressListeners.remove(l)) {
                throw new RuntimeException("ClassProgressListener " + l + " not registered");
            }
        }

        @Override
        public void startClass(String fqn) throws IOException {
            currentFqn = fqn;
            for (ClassProgressListener classProgressListener : classProgressListeners) {
                classProgressListener.classEnded(fqn);
            }
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            try {
                for (ClassProgressListener classProgressListener : classProgressListeners) {
                    classProgressListener.classEnded(fqn);
                }
            } finally {
                currentFqn = null;
            }
        }
    }


    public static class DirOutput extends ObservableToolOutput {

        private final String dir;
        private final ClassProgressListener classProgressListener;

        public DirOutput(String dir) {
            this.dir = dir;
            classProgressListener = new ClassProgressListener() {

                @Override
                public void classStarted(String fqn) {
                    //mkdir
                    //fileopen

                }

                @Override
                public void classEnded(String fqn) {
                    //fileclose
                }
            };
        }


        @Override
        public String toString() {
            return super.toString() + " to " + dir;
        }

        @Override
        public void println(String line) {
            throw new RuntimeException("Not yet implemented");
        }
        @Override
        public void print(String line) {
            throw new RuntimeException("Not yet implemented");
        }
        @Override
        public void print(char line) {
            throw new RuntimeException("Not yet implemented");
        }
    }

    public static class OutputStreamOutput extends ObservableToolOutput {

        private PrintStream os;

        public OutputStreamOutput(PrintStream os) {
            //although it is usually System.out, it is set from Environment, or custom
            this.os = os;
        }

        /**
         * One can chane the stream as action to new class
         *
         * @param os
         */
        public void setOutputStream(PrintStream os) {
            this.os = os;
        }

        @Override
        public void println(String line) {
            throw new RuntimeException("Not yet implemented");
        }
        @Override
        public void print(String line) {
            throw new RuntimeException("Not yet implemented");
        }
        @Override
        public void print(char line) {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public void finishClass(String fqn) throws IOException {
            try {
                super.finishClass(fqn);
            } finally {
                os.flush();
            }
        }


    }

}

