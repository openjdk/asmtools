package org.openjdk.asmtools.common.inputs;

public class StdinInput extends StreamInput {

    public StdinInput() {
        super(System.in);
    }

    @Override
    public String getFileName() {
        //get parent is used
        return "stdin/stdin";
    }
}
