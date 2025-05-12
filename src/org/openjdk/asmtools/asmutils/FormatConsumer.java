package org.openjdk.asmtools.asmutils;

@FunctionalInterface
public interface FormatConsumer<F, A> {
    void format(F t, A... arg);
}
