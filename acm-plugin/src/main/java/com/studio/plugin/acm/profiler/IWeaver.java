package com.studio.plugin.acm.profiler;

import java.io.IOException;
import java.io.InputStream;

public interface IWeaver {

    /**
     * Check a certain file is weavable
     */
    boolean isWeavableClass(String filePath) throws IOException;

    /**
     * Weave single class to byte array
     */
    byte[] weaveSingleClassToByteArray(InputStream inputStream) throws IOException;
}
