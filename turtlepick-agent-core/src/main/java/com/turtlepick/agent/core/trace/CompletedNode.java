package com.turtlepick.agent.core.trace;

public final class CompletedNode {

    private final int callId;
    private final int parentCallId;
    private final int methodId;
    private final String fqcnMethod;
    private final long startOffsetMs;
    private final long endOffsetMs;

    public CompletedNode(
            int callId,
            int parentCallId,
            int methodId,
            String fqcnMethod,
            long startOffsetMs,
            long endOffsetMs
    ) {
        this.callId = callId;
        this.parentCallId = parentCallId;
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
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

    public long getStartOffsetMs() {
        return startOffsetMs;
    }

    public long getEndOffsetMs() {
        return endOffsetMs;
    }
}
