package de.jakomi1.datapack;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface DatapackContentWriter {

    void write(Path packRoot) throws IOException;
}
