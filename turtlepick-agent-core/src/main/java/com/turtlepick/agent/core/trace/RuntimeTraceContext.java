package com.turtlepick.agent.core.trace;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class RuntimeTraceContext {

    private final String traceId;
    private final Deque<MethodFrame> stack = new ArrayDeque<MethodFrame>();
    private int entryMethodId;
    private String entryFqcnMethod;

    public RuntimeTraceContext() {
        this.traceId = UUID.randomUUID().toString();
    }

    public String getTraceId() {
        return traceId;
    }

    public int getEntryMethodId() {
        return entryMethodId;
    }

    public String getEntryFqcnMethod() {
        return entryFqcnMethod;
    }

    public void push(int methodId, String fqcnMethod) {
        if (stack.isEmpty()) {
            this.entryMethodId = methodId;
            this.entryFqcnMethod = fqcnMethod;
        }
        stack.push(new MethodFrame(methodId, fqcnMethod, System.nanoTime()));
    }

    public MethodFrame peek() {
        return stack.peek();
    }

    public MethodFrame pop() {
        return stack.poll();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public void clear() {
        stack.clear();
    }
}
