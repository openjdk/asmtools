package org.openjdk.asmtools.common.structure;

import org.openjdk.asmtools.jdis.ClassData;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public interface ToolInput {

    String getFileName();

    void provide(ClassData classData) throws IOException;

    public static class FileInput implements  ToolInput {
        private final String file;

        public FileInput(String file) {
            this.file = file;
        }

        @Override
        public String getFileName() {
            return file;
        }

        @Override
        public void provide(ClassData classData) throws IOException {
            classData.read(file);
        }

        @Override
        public String toString() {
            return getFileName();
        }
    }

    public static class StdinInput implements  ToolInput {

        @Override
        public String getFileName() {
            //get parent is used
            return "stdin/in";
        }

        @Override
        public void provide(ClassData classData) throws IOException {
            try (DataInputStream dis = new DataInputStream(System.in)) {
                classData.read(dis, Paths.get(getFileName()));
            }
        }

        @Override
        public String toString() {
            return getFileName();
        }
    }
}
