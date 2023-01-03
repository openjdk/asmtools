package org.openjdk.asmtools.common.inputs;

import org.openjdk.asmtools.common.Environment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ByteInput implements ToolInput {

    //compilers passes input more then one times, so saving it for reuse;
    protected byte[] bytes;

    public ByteInput(final byte[] bytes) {
        this.bytes = bytes;
    }

    protected ByteInput() {

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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), "utf-8"))) {
            while (true) {
                String l = br.readLine();
                if (l == null) {
                    break;
                }
                r.add(l);
            }
        }
        ;
        return r;
    }
}
