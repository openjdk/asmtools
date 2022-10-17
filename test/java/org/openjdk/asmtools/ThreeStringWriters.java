package org.openjdk.asmtools;

import org.openjdk.asmtools.common.ToolOutput;

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
        return new ToolOutput.PrintWriterOutput(toolOutput);
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

    public ToolOutput.DualStreamToolOutput getLoggers() {
        return new ToolOutput.SingleDualOutputStreamOutput(getErrorOutput());
    }
}