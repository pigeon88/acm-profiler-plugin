package com.studio.plugin.acm.log;

/**
 * 插入到类中的实际代码
 *
 * @author yangyp
 */
public abstract class AopInvoker {

    protected final String mTarget;
    protected final String mMethodName;
    protected final String mArgsName;
    protected static int mExecuteTimeout;

    public AopInvoker(String target, String methodName, String argsName, int executeTimeout) {
        this.mTarget = target;
        this.mMethodName = methodName;
        this.mArgsName = argsName;
        mExecuteTimeout = executeTimeout;
    }

    /**
     * 方法开始前执行的逻辑
     */
    public abstract void beforeInvoke();

    /**
     * 方法结束后执行的逻辑
     */
    public abstract void afterInvoke();

    public String getTarget() {
        return mTarget;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public String getArgsName() {
        return mArgsName;
    }

    public int getExecuteTimeout() {
        return mExecuteTimeout;
    }
}