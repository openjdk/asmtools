package org.openjdk.asmtools.common.outputs;

public class StdoutOutput extends EscapedPrintStreamOutput {

    public StdoutOutput() {
        super(System.out);
    }

}
