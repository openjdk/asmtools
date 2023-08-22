package org.openjdk.asmtools.common;

/**
 * A sorted list of error messages
 */
public final class ErrorMessage {

    public int where;
    public String message;
    public ErrorMessage next;

    /**
     * Constructor
     */
    public ErrorMessage(int where, String message) {
        this.where = where;
        this.message = message;
    }
}
