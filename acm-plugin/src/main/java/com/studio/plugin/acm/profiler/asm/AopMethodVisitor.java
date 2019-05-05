package com.studio.plugin.acm.profiler.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.HashMap;
import java.util.Map;


/**
 * AOP method visitor，主要做的就是在方法中插入了AopInvoker的代码
 *
 * @author yangyp
 */
class AopMethodVisitor extends AdviceAdapter {

    static final Map<String, String> ByteCode = new HashMap<>();

    static {
        ByteCode.put("V", "void");
        ByteCode.put("Z", "boolean");
        ByteCode.put("C", "char");
        ByteCode.put("B", "byte");
        ByteCode.put("S", "short");
        ByteCode.put("I", "int");
        ByteCode.put("F", "float");
        ByteCode.put("J", "long");
        ByteCode.put("D", "double");
        //ByteCode.put("Ljava/lang/Object", "Object");
    }

    public static final String DEFAULT_AOP_CLASS = "com/studio/plugin/acm/log/LogAopInvoker";
    private String mAopClassName = DEFAULT_AOP_CLASS;
    private int mExecuteTimeout;
    private final String mClassName;
    private final String mMethodName;
    private final String mParamName;
    private int mInvokerVarIndex = 1;

    public AopMethodVisitor(int api, MethodVisitor originMV, int access, String desc, String className, String methodName, String aopClassName, int executeTimeout) {
        super(api, originMV, access, methodName, desc);
        mClassName = className;
        mMethodName = methodName;
        mParamName = desc;
        mExecuteTimeout = executeTimeout;
        if (aopClassName != null && aopClassName.trim().length() > 0) {
            aopClassName = aopClassName.replaceAll("/", ".");
            mAopClassName = aopClassName;
        }
    }

    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        beginAspect();
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode);
        afterAspect();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 2, maxLocals + 1);
    }

    /**
     * 在方法开始插入AopInvoker.aspectBeforeInvoke()
     */
    private void beginAspect() {
        if (mv == null) {
            return;
        }

        mv.visitTypeInsn(NEW, mAopClassName);
        mv.visitInsn(DUP);
        //mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(mClassName);
        mv.visitLdcInsn(mMethodName);
        mv.visitLdcInsn(getArgsName(methodDesc));
        mv.visitLdcInsn(mExecuteTimeout);
        mv.visitMethodInsn(INVOKESPECIAL, mAopClassName, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", false);
        mInvokerVarIndex = newLocal(Type.getType(String.format("L%s;", mAopClassName))); //"Landroid/support/studio/plugin/monitor/AopInvoker;"
        mv.visitVarInsn(ASTORE, mInvokerVarIndex);

        mv.visitVarInsn(ALOAD, mInvokerVarIndex);
        mv.visitMethodInsn(INVOKEVIRTUAL, mAopClassName, "beforeInvoke", "()V", false);
    }

    /**
     * 在方法结束插入AopInvoker.aspectAfterInvoke()
     */
    private void afterAspect() {
        if (mv == null) {
            return;
        }
        mv.visitVarInsn(ALOAD, mInvokerVarIndex);
        mv.visitMethodInsn(INVOKEVIRTUAL, mAopClassName, "afterInvoke", "()V", false);
    }

    public static String getArgsName(String argsName) {
        String[] args = argsName.split("[\\(|;|\\)]");
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0, iSize = args.length - 2; i < iSize; i++) {
            String realArgName = getRealArgName(args[i]);
            if (realArgName != null) {
                builder.append(realArgName);
                if (i < iSize - 1) {
                    builder.append(", ");
                }
            }
        }
        builder.append(")");
        builder.append(getRealArgName(args[args.length - 1]));
        return builder.toString();
    }

    private static String getRealArgName(String argName) {
        if (argName == null || "".equals(argName)) {
            return null;
        }
        if (argName.length() == 1) {
            return ByteCode.get(argName);
        }

        if (argName.indexOf("L") != -1) {
            return argName.substring(argName.lastIndexOf("/") + 1);
        }

        StringBuilder array = new StringBuilder();
        char[] chars = argName.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '[') {
                array.append("[]");
            } else {
                return getRealArgName(argName.substring(i + 1)) + array.toString();
            }
        }

        return argName;
    }
}