package com.studio.plugin.acm.profiler.asm;

import com.studio.plugin.acm.profiler.AopEngine;
import com.studio.plugin.acm.profiler.BaseWeaver;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by quinn on 07/09/2018
 */
public class ASMWeaver extends BaseWeaver {

    private int executeTimeout;

    public ASMWeaver(int executeTimeout) {
        this.executeTimeout = executeTimeout;
    }

    @Override
    public byte[] weaveSingleClassToByteArray(InputStream inputStream) throws IOException {
        /*ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ExtendClassWriter(classLoader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor classWriterWrapper = wrapClassWriter(classWriter);
        classReader.accept(classWriterWrapper, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();*/
        return AopEngine.doAspect(inputStream, null, executeTimeout);
    }

    protected ClassVisitor wrapClassWriter(ClassWriter classWriter) {
        return classWriter;
    }
}
