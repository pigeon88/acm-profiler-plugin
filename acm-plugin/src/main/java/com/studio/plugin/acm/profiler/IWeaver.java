package com.studio.plugin.acm.profiler;

import java.io.File;
import java.io.IOException;

public interface IWeaver {

    void weaveJar(File inputJar, File outputJar) throws IOException;

    void weaveSingleClassToFile(File inputFile, File outputFile, String inputBaseDir) throws IOException;
}
