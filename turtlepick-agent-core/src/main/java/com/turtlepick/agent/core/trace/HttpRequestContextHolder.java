package com.turtlepick.agent.core.trace;

public final class HttpRequestContextHolder {

    private static final ThreadLocal<HttpRequestContext> HOLDER = new ThreadLocal<HttpRequestContext>();

    private HttpRequestContextHolder() {
    }

    public static HttpRequestContext get() {
        return HOLDER.get();
    }

    public static void set(HttpRequestContext context) {
        HOLDER.set(context);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
