package org.openjdk.asmtools.common.outputs.log;

import org.openjdk.asmtools.common.NotImplementedException;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;

public interface DualStreamToolOutput extends ToolOutput {
    void printlne(String line);

    void printe(String line);

    void printe(char line);

    void stacktrace(Throwable ex);

    ToolOutput getSToolObject();

    ToolOutput getEToolObject();

    @Override
    default DataOutputStream getDataOutputStream() throws FileNotFoundException {
        throw new NotImplementedException("Not going to happen");
    }
}
