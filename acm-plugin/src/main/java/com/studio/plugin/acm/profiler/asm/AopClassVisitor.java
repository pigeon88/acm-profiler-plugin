package com.studio.plugin.acm.profiler.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * AOP class visitor，主要做的就是调用了AopMethodVisitor
 *
 * @author yangyp
 */
public class AopClassVisitor extends ClassVisitor {

    private final String mTargetPackageName;
    private int mExecuteTimeout;
    private String mActualClassFullName;

    //private boolean mNeedModifyMethod = false;
    public AopClassVisitor(int api, ClassVisitor cv, String targetPackageName, int executeTimeout) {
        super(api, cv);
        mTargetPackageName = targetPackageName;
        mExecuteTimeout = executeTimeout;
        //mNeedModifyMethod = false;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        mActualClassFullName = name.replaceAll("/", ".");
        //if (mActualClassFullName != null) {
        //mNeedModifyMethod = mActualClassFullName.startsWith(mTargetPackageName);
        //}
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (/*mNeedModifyMethod &&*/ !(
                "<init>".equalsIgnoreCase(name)
                        || isAcc(access, Opcodes.ACC_ABSTRACT)
                        || isAcc(access, Opcodes.ACC_NATIVE)
                        || isAcc(access, Opcodes.ACC_STATIC)
        )) {
            return new AopMethodVisitor(api, mv, access, desc, mActualClassFullName, name, mTargetPackageName, mExecuteTimeout);
        }
        return mv;
    }

    static boolean isAcc(int access, int acc) {
        return (access & acc) == acc;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        //mNeedModifyMethod = false;
    }
}