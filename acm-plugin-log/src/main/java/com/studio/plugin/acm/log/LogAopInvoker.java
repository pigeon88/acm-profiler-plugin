package com.studio.plugin.acm.log;


import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class LogAopInvoker extends AopInvoker {

    private static final OSLog LOG = OSLog.getLog("profiler");

    static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            Thread t = new Thread(runnable, group.getName());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    });

    static final ThreadLocal<StackCount> threadLocalStack = new ThreadLocal<>();
    LogContent logContent;
    //long mExecuteTimeout = 100;
    long startTimeMillis;

    public LogAopInvoker(String target, String methodName, String argsName, int executeTimeout) {
        super(target, methodName, argsName, executeTimeout);
        this.logContent = new LogContent(target, methodName, argsName);
    }

    @Override
    public void beforeInvoke() {
        StackCount stackCount = threadLocalStack.get();
        if (stackCount == null) {
            threadLocalStack.set(stackCount = new StackCount());
            if (isMainThread()) {
                setExecuteTimeoutLocked(5 * 1000);
            }
        }


        int count = stackCount.incrementAndGet();
        //System.out.println(String.format("[%s]%s -> stack push: %d", Thread.currentThread().getName(), mMethodName, count));
        stackCount.add(this, logContent, count);
        startTimeMillis = System.currentTimeMillis();
    }

    private boolean isMainThread() {
        return "main".equals(Thread.currentThread().getName());
    }

    @Override
    public void afterInvoke() {
        StackCount stackCount = threadLocalStack.get();
        //System.out.println(String.format("[%s]%s <- stack pop: %d", Thread.currentThread().getName(), mMethodName, stackCount.count));
        if (stackCount.count > 0) {
            long executeTime = System.currentTimeMillis() - startTimeMillis;
            LogContent logContent = stackCount.get(this);
            logContent.setExecuteTime(executeTime);
            int count = stackCount.decrementAndGet();
            if (count == 0) {
                cancelExecuteTimeoutLocked();
                logPrint(threadLocalStack.get());
                threadLocalStack.set(null);
            }
        }
    }

    void logPrint(StackCount stackCount) {
        StringBuilder builder = new StringBuilder();
        stackCount.count = 0;
        Iterator<LogContent> it = stackCount.stack.values().iterator();
        while (it.hasNext()) {
            LogContent logContent = it.next();
            if (logContent.executeTime >= mExecuteTimeout) {
                String pop = logContent.getMessage();
                builder.append(pop);
                if (it.hasNext()) {
                    builder.append("\n");
                    //builder.append(String.format("%" + logContent.count * 3 + "s", ""));
                }
            }
        }

        stackCount.stack.clear();
        if (builder.length() > 0) {
            LOG.log(builder.toString());
        }
    }

    //boolean mBroadcastsScheduled;
    //boolean mPendingBroadcastTimeoutMessage;

    final void setExecuteTimeoutLocked(long timeoutTime) {
        //if (!mPendingBroadcastTimeoutMessage) {
        //Message msg = mHandler.obtainMessage(BROADCAST_TIMEOUT_MSG, this);
        //mHandler.sendMessageAtTime(msg, timeoutTime);
        final StackCount stackCount = threadLocalStack.get();
        ScheduledFuture<?> schedule = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                scheduleBroadcastsLocked(stackCount);
            }
        }, timeoutTime, TimeUnit.MILLISECONDS);
        threadLocalStack.get().schedule = schedule;
        //mPendingBroadcastTimeoutMessage = true;
        //}
    }

    final void cancelExecuteTimeoutLocked() {
        //if (mPendingBroadcastTimeoutMessage) {
        //mHandler.removeMessages(BROADCAST_TIMEOUT_MSG, this);
        ScheduledFuture<?> schedule = threadLocalStack.get().schedule;
        if (schedule != null) {
            schedule.cancel(true);
            threadLocalStack.get().schedule = null;
        }
        //mPendingBroadcastTimeoutMessage = false;
        //}
    }

    public void scheduleBroadcastsLocked(StackCount stackCount) {
        /*if (DEBUG_BROADCAST) Slog.v(TAG_BROADCAST, "Schedule broadcasts ["
                + mQueueName + "]: current="
                + mBroadcastsScheduled);*/

        /*if (mBroadcastsScheduled) {
            return;
        }*/
        //mHandler.sendMessage(mHandler.obtainMessage(BROADCAST_INTENT_MSG, this));
        logPrint(stackCount);
        //mBroadcastsScheduled = true;
    }

    static class StackCount {
        private volatile int count;
        private Map<String, LogContent> stack = new LinkedHashMap<>();
        private ScheduledFuture<?> schedule;

        public final int incrementAndGet() {
            return ++count;
        }

        public final int decrementAndGet() {
            return --count;
        }

        public ScheduledFuture<?> getSchedule() {
            return schedule;
        }

        public void setSchedule(ScheduledFuture<?> schedule) {
            this.schedule = schedule;
        }

        public LogContent get(AopInvoker aopInvoker) {
            return stack.get(aopInvoker.toString());
        }

        public void add(AopInvoker aopInvoker, LogContent logContent, int count) {
            logContent.setCount(count);
            stack.put(aopInvoker.toString(), logContent);
        }

        @Override
        public String toString() {
            return "StackCount{" +
                    "count=" + count +
                    '}';
        }
    }

    static class LogContent {
        long executeTime = Integer.MAX_VALUE; //默认为5s+
        String target;
        String methodName;
        String argsName;
        boolean isANR;
        int count;

        public LogContent(String target, String methodName, String argsName) {
            this.target = target;
            this.methodName = methodName;
            this.argsName = argsName;
            this.isANR = true;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setExecuteTime(long executeTime) {
            this.isANR = false;
            this.executeTime = executeTime;
        }

        public String getMessage() {
            String anr = isANR ? "(ANR)" : "";
            return String.format("[%s] %s->%s %.2fs %s", Thread.currentThread().getName(), target, methodName + argsName, executeTime / 1000f, anr);
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}
