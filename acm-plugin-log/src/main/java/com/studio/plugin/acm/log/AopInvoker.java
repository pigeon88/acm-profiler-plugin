package com.studio.plugin.acm.log;

/**
 * 插入到类中的实际代码
 *
 * @author yangyp
 */
public abstract class AopInvoker {

    private final Object mTarget;
    private final String mMethodName;
    private final String mArgsName;
    protected final int mExecuteTimeout;

    public AopInvoker(Object target, String methodName, String argsName, int executeTimeout) {
        this.mTarget = target;
        this.mMethodName = methodName;
        this.mArgsName = argsName;
        this.mExecuteTimeout = executeTimeout;
    }

    /**
     * 方法开始前执行的逻辑
     */
    public abstract void beforeInvoke();

    /**
     * 方法结束后执行的逻辑
     */
    public abstract void afterInvoke();

    public Object getTarget() {
        return mTarget;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public String getArgsName() {
        return mArgsName;
    }

    public int getExecuteTimeout() {
        return mExecuteTimeout <= 0 ? 500 : mExecuteTimeout;
    }

    /**
     * 生成一个invoker
     *
     * @param className
     * @param methodName
     * @return
     */
    public static AopInvoker newInvoker(Object className, String methodName, String argsName, int executeTimeout) {
        return new LogAopInvoker(className, methodName, argsName, executeTimeout);
    }
}