package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompletedNode {

    private final int callId;
    private final int parentCallId;
    private final int methodId;
    private final String fqcnMethod;
    private final long startOffsetMs;
    private final long endOffsetMs;
    private final Object[] args;
    private final List<TraceSql> sqlPayloads;
    private List<TraceParam> params = Collections.emptyList();

    public CompletedNode(
            int callId,
            int parentCallId,
            int methodId,
            String fqcnMethod,
            long startOffsetMs,
            long endOffsetMs,
            Object[] args,
            List<TraceSql> sqlPayloads
    ) {
        this.callId = callId;
        this.parentCallId = parentCallId;
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
        this.args = args;
        this.sqlPayloads = sqlPayloads == null || sqlPayloads.isEmpty()
                ? Collections.<TraceSql>emptyList()
                : Collections.unmodifiableList(new ArrayList<TraceSql>(sqlPayloads));
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

    public Object[] getArgs() {
        return args;
    }

    public List<TraceSql> getSqlPayloads() {
        return sqlPayloads;
    }

    public void attachParams(List<TraceParam> value) {
        this.params = value == null || value.isEmpty()
                ? Collections.<TraceParam>emptyList()
                : Collections.unmodifiableList(new ArrayList<TraceParam>(value));
    }

    public List<TraceParam> getParams() {
        return params;
    }

}
