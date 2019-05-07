package com.studio.plugin.acm.profiler;

import com.android.ide.common.internal.WaitableExecutor;

import java.io.File;
import java.io.IOException;

public class WaitableAgent implements IWeaver {

    private IWeaver baseWeaver;
    private WaitableExecutor waitableExecutor;

    public WaitableAgent(BaseWeaver baseWeaver) {
        this.baseWeaver = baseWeaver;
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
    }

    public void weaveJar(File inputJar, File outputJar) throws IOException {
        waitableExecutor.execute(() -> {
            baseWeaver.weaveJar(inputJar, outputJar);
            return null;
        });
    }

    public void weaveSingleClassToFile(File inputFile, File outputFile, String inputBaseDir) throws IOException {
        waitableExecutor.execute(() -> {
            baseWeaver.weaveSingleClassToFile(inputFile, outputFile, inputBaseDir);
            return null;
        });
    }
}
