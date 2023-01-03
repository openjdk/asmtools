package org.openjdk.asmtools.common.inputs;

import org.openjdk.asmtools.common.Environment;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

public class FileInput implements ToolInput {
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
                    if (logger.isPresent()) {
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
