package org.openjdk.asmtools.common.inputs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class StreamInput extends ByteInput {

    private final InputStream originalStream;

    public StreamInput(InputStream is) {
        originalStream = is;
    }

    @Override
    protected void init() {
        if (bytes == null) {
            bytes = drainIs(originalStream);
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
