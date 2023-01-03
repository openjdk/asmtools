package org.openjdk.asmtools.common.outputs;

import org.openjdk.asmtools.common.NotImplementedException;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class PrintWriterOutput extends NamedToolOutput {

    protected PrintWriter os;

    public PrintWriterOutput(OutputStream os) {
        //although it is usually System.out, it is set from Environment, or custom
        this.os = new PrintWriter(os, true);
    }

    public PrintWriterOutput(Writer os) {
        //although it is usually System.out, it is set from Environment, or custom
        this.os = new PrintWriter(os, true);
    }

    @Override
    public void printlns(String line) {
        os.println(line);
    }

    @Override
    public void prints(String line) {
        os.print(line);
    }

    @Override
    public void prints(char line) {
        os.print(line);
    }

    @Override
    public void finishClass(String fqn) throws IOException {
        super.finishClass(fqn);
        os.flush();
    }

    @Override
    public void flush() {
        os.flush();
    }

    @Override
    public DataOutputStream getDataOutputStream() throws FileNotFoundException {
        throw new NotImplementedException("Use EscapedPrintStreamOutput");
    }


}
