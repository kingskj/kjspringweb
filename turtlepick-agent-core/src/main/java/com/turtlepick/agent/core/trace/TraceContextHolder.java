package com.turtlepick.agent.core.trace;

public final class TraceContextHolder {

    private static final ThreadLocal<RuntimeTraceContext> HOLDER = new ThreadLocal<RuntimeTraceContext>();

    private TraceContextHolder() {
    }

    public static RuntimeTraceContext get() {
        return HOLDER.get();
    }

    public static RuntimeTraceContext getOrCreate() {
        RuntimeTraceContext context = HOLDER.get();
        if (context == null) {
            context = new RuntimeTraceContext();
            HOLDER.set(context);
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
