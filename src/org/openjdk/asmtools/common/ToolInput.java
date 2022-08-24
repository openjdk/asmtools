package org.openjdk.asmtools.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public interface ToolInput {

    String getFileName();

    DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException;

    Collection<String> readAllLines() throws IOException;

    public static class FileInput implements  ToolInput {
        private final String file;

        public FileInput(String file) {
            this.file = file;
        }

        @Override
        public String getFileName() {
            return file;
        }

        public Collection<String> readAllLines() throws IOException {
            return Files.readAllLines(Paths.get(getFileName()));
        }

        @Override
        public DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException {
            try {
                return new DataInputStream(new FileInputStream(this.getFileName()));
            } catch (IOException ex) {
                if (this.getFileName().matches("^[A-Za-z]+:.*")) {
                    try {
                        final URI uri = new URI(this.getFileName());
                        final URL url = uri.toURL();
                        final URLConnection conn = url.openConnection();
                        conn.setUseCaches(false);
                        return new DataInputStream(conn.getInputStream());
                    } catch (URISyntaxException | IOException exception) {
                        if (logger.isPresent()){
                            logger.get().error("err.cannot.read", this.getFileName());
                        }
                        throw exception;
                    }
                } else {
                    throw ex;
                }
            }
        }

        @Override
        public String toString() {
            return getFileName();
        }
    }

    public static class ByteInput implements  ToolInput {

        //compilers passes input more then one times, so saving it for reuse;
        protected byte[] bytes;

        public ByteInput(final byte[] bytes) {
            this.bytes = bytes;
        }

        protected ByteInput() {

        }

        public ByteInput(final String bytes) {
            this.bytes = bytes.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String getFileName() {
            //get parent is used
            return "bytes/bytes";
        }

        @Override
        public String toString() {
            return getFileName();
        }

        protected void init() {

        }

        @Override
        public DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException {
            init();
            return new DataInputStream(new ByteArrayInputStream(bytes));
        }

        @Override
        public Collection<String> readAllLines() throws IOException {
            init();
            ArrayList r = new ArrayList();
            try(BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), "utf-8"))){
                while(true){
                    String l = br.readLine();
                    if (l==null){
                        break;
                    }
                    r.add(l);
                }
            };
            return r;
        }
    }

    public static class StreamInput extends ByteInput {

        private final InputStream originalStream;

        public StreamInput(InputStream is) {
            originalStream = is;
        }

        @Override
        protected void init() {
            if (bytes == null){
                bytes=drainIs(originalStream);
            }
        }

        public static byte[] drainIs(InputStream is) {
            try {
                byte[] buffer = new byte[32 * 1024];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bytesRead;
                while ((bytesRead = is.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public String getFileName() {
            //get parent is used
            return "stream/stream";
        }
    }

    public static class StdinInput extends StreamInput {

        public StdinInput() {
            super(System.in);
        }

        @Override
        public String getFileName() {
            //get parent is used
            return "stdin/stdin";
        }
    }
}
