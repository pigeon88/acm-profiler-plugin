package com.studio.plugin.acm.log;


import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class LogAopInvoker extends AopInvoker {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, "main-timeout");
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    });

    private static final AopLog LOG = AopLog.getLog("profiler");
    public static final int ANR_TIMEOUT = 5 * 1000;
    private static final ThreadLocal<StackCounter> threadLocalStack = new ThreadLocal<>();
    private long startTimeMillis;

    public LogAopInvoker(String target, String methodName, String argsName, int executeTimeout) {
        super(target, methodName, argsName, executeTimeout);
    }

    @Override
    public void beforeInvoke() {
        StackCounter stackCount = threadLocalStack.get();
        if (stackCount == null) {
            threadLocalStack.set(stackCount = new StackCounter());
            if (isMainThread()) {
                setExecuteTimeoutLocked(ANR_TIMEOUT);
            }
        }


        int count = stackCount.incrementAndGet();
        //System.out.println(String.format("[%s]%s -> stack push: %d", Thread.currentThread().getName(), mMethodName, count));
        stackCount.add(this, new LogContent(mTarget, mMethodName, mArgsName, count));
        startTimeMillis = System.currentTimeMillis();
    }

    private boolean isMainThread() {
        return "main".equals(Thread.currentThread().getName());
    }

    @Override
    public void afterInvoke() {
        StackCounter stackCount = threadLocalStack.get();
        //System.out.println(String.format("[%s]%s <- stack pop: %d", Thread.currentThread().getName(), mMethodName, stackCount.count));
        if (stackCount != null && stackCount.getCount() > 0) {
            long executeTime = System.currentTimeMillis() - startTimeMillis;
            if (executeTime < mExecuteTimeout) {
                stackCount.remove(this);
            } else {
                LogContent logContent = stackCount.get(this);
                logContent.setExecuteTime(executeTime);
            }
            int count = stackCount.decrementAndGet();
            if (count == 0) {
                cancelExecuteTimeoutLocked();
                logPrint(threadLocalStack.get());
                threadLocalStack.set(null);
            }
        }
    }

    void logPrint(StackCounter stackCount) {
        StringBuilder builder = new StringBuilder();
        stackCount.setCount(0);
        Iterator<LogContent> it = stackCount.getStack().values().iterator();
        while (it.hasNext()) {
            LogContent logContent = it.next();
            builder.append("[" + stackCount.getThreadName() + "] ");
            int space = (logContent.getCount() - 2) * 4;
            while (space-- > 0) {
                builder.append("-");
            }
            builder.append(logContent.getMessage());
            if (it.hasNext()) {
                builder.append("\n");
            }
        }

        stackCount.getStack().clear();
        if (builder.length() > 0) {
            LOG.log(builder.toString());
        }
    }

    final void setExecuteTimeoutLocked(long timeoutTime) {
        final StackCounter stackCount = threadLocalStack.get();
        ScheduledFuture<?> schedule = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleBroadcastsLocked(stackCount);
            }
        }, timeoutTime, TimeUnit.MILLISECONDS);
        threadLocalStack.get().setSchedule(schedule);
    }

    final void cancelExecuteTimeoutLocked() {
        ScheduledFuture<?> schedule = threadLocalStack.get().getSchedule();
        if (schedule != null) {
            schedule.cancel(true);
            threadLocalStack.get().setSchedule(null);
        }
    }

    public void scheduleBroadcastsLocked(StackCounter stackCount) {
        logPrint(stackCount);
    }
}
