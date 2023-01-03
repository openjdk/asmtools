package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.uEscWriter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class EscapedPrintStreamOutput extends PrintWriterOutput {

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
