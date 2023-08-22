package org.openjdk.asmtools.common;

import static java.lang.String.format;

import org.openjdk.asmtools.common.outputs.log.DualStreamToolOutput;
import org.openjdk.asmtools.common.outputs.ToolOutput;

import java.util.regex.Pattern;

public interface ILogger {

    // A logged message isn't attached to a position of a parsed file
    int NOWHERE = Integer.MAX_VALUE;
    // Replacement for the tab found in an input
    CharSequence TAB_REPLACEMENT = "    ";

    Pattern usagePattern = Pattern.compile("(-.*?)\s([PGSDCSOIU]+.*)");

    default String getResourceString(String id, Object... args) {
        throw new NotImplementedException();
    }
    default void warning(int where, String id, Object... args) { throw new NotImplementedException(); }

    default void error(int where, String id, Object... args) {
        throw new NotImplementedException();
    }

    default void info(String id, Object... args) {
        String message = getInfo(id, args);
        if (message != null) {
            println(message);
        }
    }

    default String getInfo(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void warning(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(Throwable exception) { error(NOWHERE, exception.getMessage()); }

    default void traceln(String format, Object... args) {
        getOutputs().printlne(( args == null || args.length == 0) ? format : format(format, args));;
    }

    default void trace(String format, Object... args) {
        getOutputs().printe(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void printErrorLn(String format, Object... args) {
        getOutputs().printlne(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void println(String format, Object... args) {
        getOutputs().printlns(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void println() {
        getOutputs().printlns("");
    }

    default void print(String format, Object... args) {
        getOutputs().prints(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void print(char ch) {
        getOutputs().prints(ch);
    }

    DualStreamToolOutput getOutputs();

    void setOutputs(DualStreamToolOutput nwoutput);

    default ToolOutput getToolOutput() {
        throw new NotImplementedException("implement wisely!");
    }

    default void setToolOutput(ToolOutput toolOutput) {
        throw new NotImplementedException("implement wisely!");
    }

    void printException(Throwable throwable);
}
