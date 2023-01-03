package org.openjdk.asmtools;

import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.PrintWriterOutput;
import org.openjdk.asmtools.common.outputs.log.SingleDualOutputStreamOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class ThreeStringWriters {
    private final ByteArrayOutputStream toolBos = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorBos = new ByteArrayOutputStream();
    private final ByteArrayOutputStream loggerBos = new ByteArrayOutputStream();
    private final PrintWriter toolOutput = new PrintWriter(toolBos);
    private final PrintWriter errorOutput = new PrintWriter(errorBos);
    private final PrintWriter loggerOutput = new PrintWriter(loggerBos);

    public void flush(){
        toolOutput.flush();
        errorOutput.flush();
        loggerOutput.flush();
    }

    public ToolOutput getToolOutputWrapper() {
        return new PrintWriterOutput(toolOutput);
    }
    public PrintWriter getToolOutput() {
        return toolOutput;
    }

    public PrintWriter getErrorOutput() {
        return errorOutput;
    }

    public PrintWriter getLoggerOutput() {
        return loggerOutput;
    }

    public String getLoggerBos() {
        return loggerBos.toString();
    }

    public String getErrorBos() {
        return errorBos.toString();
    }

    public String getToolBos() {
        return toolBos.toString();
    }

    public DualStreamToolOutput getLoggers() {
        return new SingleDualOutputStreamOutput(getErrorOutput());
    }
}
