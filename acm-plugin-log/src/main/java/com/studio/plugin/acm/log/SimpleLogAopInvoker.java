package com.studio.plugin.acm.log;

/**
 * 方法调用时间统计Invoker实现
 *
 * @author yangyp
 */
public class SimpleLogAopInvoker extends AopInvoker {

    private static final AopLog LOG = AopLog.getLog("profiler", SimpleLogAopInvoker.class);
    private Long mStartTime;

    public SimpleLogAopInvoker(String target, String methodName, String argsName, int executeTimeout) {
        super(target, methodName, argsName, executeTimeout);
    }

    @Override
    public void beforeInvoke() {
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void afterInvoke() {
        Long diffTime = System.currentTimeMillis() - mStartTime;
        if (diffTime >= getExecuteTimeout()) {
            LOG.log("%s->%s: %dms", getTarget(), getMethodName() + getArgsName(), diffTime);
        }
    }

}