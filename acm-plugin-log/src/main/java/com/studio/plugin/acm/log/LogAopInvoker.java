package com.studio.plugin.acm.log;

import android.os.Looper;

/**
 * 方法调用时间统计Invoker实现
 *
 * @author yangyp
 */
public class LogAopInvoker extends AopInvoker {

    private static final OSLog LOG = OSLog.getLog("profiler");
    private Long mStartTime;

    public LogAopInvoker(Object target, String methodName, String argsName, int executeTimeout) {
        super(target, methodName, argsName, executeTimeout);
    }

    @Override
    public void beforeInvoke() {
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void afterInvoke() {
        Long diffTime = System.currentTimeMillis() - mStartTime;
        String threadName = getThreadName();
        /*if (diffTime > 100) {
            if (diffTime > getExecuteTimeout()) {
                Log.e("AopInvoker", getMessageLog(diffTime, threadName));
            } else {
                Log.i("AopInvoker", getMessageLog(diffTime, threadName));
            }
        }*/

        if (diffTime > getExecuteTimeout()) {
            LOG.log(getMessageLog(diffTime, threadName));
        }
    }

    private String getMessageLog(Long diffTime, String threadName) {
        return String.format("[%s] %s->%s: %dms", threadName, getTarget().getClass().getName(), getMethodName() + getArgsName(), diffTime);
    }


    private String getThreadName() {
        return Looper.getMainLooper() == Looper.myLooper() ? "main" : "thread";
    }

}