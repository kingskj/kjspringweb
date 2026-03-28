package com.turtlepick.agent.core.trace;

public final class MethodFrame {

    private final int methodId;
    private final String fqcnMethod;
    private final long startNanoTime;

    public MethodFrame(int methodId, String fqcnMethod, long startNanoTime) {
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.startNanoTime = startNanoTime;
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
}
