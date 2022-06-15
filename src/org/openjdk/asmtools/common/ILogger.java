package org.openjdk.asmtools.common;

import java.io.PrintWriter;

import static java.lang.String.format;

public interface ILogger {

    // A logged message isn't attached to a position of a parsed file/
    int NOWHERE = Integer.MAX_VALUE;
    // Replacement for the tab found in an input
    CharSequence TAB_REPLACEMENT = "    ";

    default void warning(int where, String id, Object... args) {
        throw new NotImplementedException();
    }

    default void error(int where, String id, Object... args) {
        throw new NotImplementedException();
    }

    default void info(String id, Object... args) {
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
        println(format, args);
    }

    default void trace(String format, Object... args) {
        getOutLog().print(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void printErrorLn(String format, Object... args) {
        getErrLog().println(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void println(String format, Object... args) {
        getOutLog().println(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void println() {
        getOutLog().println();
    }

    default void print(String format, Object... args) {
        getOutLog().print(( args == null || args.length == 0) ? format : format(format, args));
    }

    default void print(char ch) {
        getOutLog().print(ch);
    }

    PrintWriter getErrLog();

    PrintWriter getOutLog();

    void printException(Throwable throwable);

}
