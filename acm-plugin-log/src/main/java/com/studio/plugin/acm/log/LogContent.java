package com.studio.plugin.acm.log;

public class LogContent {

    private long executeTime = LogAopInvoker.ANR_TIMEOUT; //默认为5s+
    private String target;
    private String methodName;
    private String argsName;
    private int count;
    private boolean isANR;

    public LogContent(String target, String methodName, String argsName, int count) {
        this.target = target;
        this.methodName = methodName;
        this.argsName = argsName;
        this.count = count;
        this.isANR = true;
    }

    public int getCount() {
        return count;
    }

    public void setExecuteTime(long executeTime) {
        this.isANR = false;
        this.executeTime = executeTime;
    }

    public String getMessage() {
        String anr = isANR ? "+ (ANR)" : "";
        return String.format("%s->%s %.2fs", target, methodName + argsName, executeTime / 1000f) + anr;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
