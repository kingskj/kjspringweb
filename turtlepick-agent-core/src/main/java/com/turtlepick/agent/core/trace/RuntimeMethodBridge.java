package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

public final class RuntimeMethodBridge {

    private RuntimeMethodBridge() {
    }

    public static void enter(int methodId, String fqcnMethod) {
        RuntimeTraceContext context = TraceContextHolder.getOrCreate();
        context.push(methodId, fqcnMethod);
    }

    public static void exit(int methodId, boolean error) {
        RuntimeTraceContext context = TraceContextHolder.get();
        if (context == null) {
            return;
        }

        MethodFrame top = context.peek();
        if (top == null) {
            TraceContextHolder.clear();
            return;
        }

        if (top.getMethodId() != methodId) {
            AgentLog.warn("method stack mismatch expected=" + methodId + " actual=" + top.getMethodId());
            context.clear();
            TraceContextHolder.clear();
            return;
        }

        context.pop();

        if (context.isEmpty()) {
            TraceContextHolder.clear();
        }
    }
}
