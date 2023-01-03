package org.openjdk.asmtools.common.outputs.log;

import java.io.PrintStream;
import java.io.PrintWriter;

public class SingleDualOutputStreamOutput extends DualOutputStreamOutput {
    public SingleDualOutputStreamOutput() {
        this(System.err);
    }

    public SingleDualOutputStreamOutput(PrintWriter er) {
        super(er, er);
    }

    public SingleDualOutputStreamOutput(PrintStream er) {
        super(er, er);
    }
}
