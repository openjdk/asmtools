package org.openjdk.asmtools.common.inputs;

import org.openjdk.asmtools.common.Environment;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;

public interface ToolInput {

    String getFileName();

    DataInputStream getDataInputStream(Optional<Environment> logger) throws URISyntaxException, IOException;

    Collection<String> readAllLines() throws IOException;

}
