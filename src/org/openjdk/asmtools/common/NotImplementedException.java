package org.openjdk.asmtools.common;

/** class used to indicate missing functionality */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException() {
        super("The method is not yet implemented");
    }
    public NotImplementedException(String reason) {
        super(reason);
    }
}
