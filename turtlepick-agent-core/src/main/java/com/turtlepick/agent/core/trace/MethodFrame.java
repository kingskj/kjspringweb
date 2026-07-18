package com.turtlepick.agent.core.trace;

public final class MethodFrame {

    private final int callId;
    private final int parentCallId;
    private final int methodId;
    private final String fqcnMethod;
    private final long startNanoTime;
    private final Object[] args;

    public MethodFrame(int callId, int parentCallId, int methodId, String fqcnMethod, long startNanoTime, Object[] args) {
        this.callId = callId;
        this.parentCallId = parentCallId;
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.startNanoTime = startNanoTime;
        this.args = args;
    }

    public int getCallId() {
        return callId;
    }

    public int getParentCallId() {
        return parentCallId;
    }

    public int getMethodId() {
        return methodId;
    }

    public String getFqcnMethod() {
        return fqcnMethod;
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public Object[] getArgs() {
        return args;
    }
}
