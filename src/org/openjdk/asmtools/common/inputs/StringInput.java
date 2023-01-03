package org.openjdk.asmtools.common.inputs;

import java.nio.charset.StandardCharsets;

public class StringInput extends ByteInput {

    public StringInput(final String bytes) {
        super(bytes.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getFileName() {
        //get parent is used
        return "string/string";
    }

}
