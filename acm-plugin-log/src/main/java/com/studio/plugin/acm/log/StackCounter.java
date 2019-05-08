package com.studio.plugin.acm.log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class StackCounter {

    private String threadName;
    private volatile int count;
    private Map<Integer, LogContent> stack = new LinkedHashMap<>();
    private ScheduledFuture<?> schedule;

    public StackCounter() {
        this.threadName = Thread.currentThread().getName();
    }

    public String getThreadName() {
        return threadName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

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

    public Map<Integer, LogContent> getStack() {
        return stack;
    }

    public LogContent get(AopInvoker aopInvoker) {
        return stack.get(aopInvoker.hashCode());
    }

    public void add(AopInvoker aopInvoker, LogContent logContent) {
        stack.put(aopInvoker.hashCode(), logContent);
    }

    public LogContent remove(AopInvoker aopInvoker) {
        return stack.remove(aopInvoker.hashCode());
    }

    @Override
    public String toString() {
        return "StackCount{" +
                "count=" + count +
                '}';
    }
}
