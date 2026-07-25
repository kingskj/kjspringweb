package com.turtlepick.agent.core.trace;

public final class HttpRequestContext {

    private final String method;
    private final String requestUri;
    private final long enterNanoTime;

    public HttpRequestContext(String method, String requestUri) {
        this(method, requestUri, System.nanoTime());
    }

    public HttpRequestContext(String method, String requestUri, long enterNanoTime) {
        this.method = method;
        this.requestUri = requestUri;
        this.enterNanoTime = enterNanoTime;
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public long getEnterNanoTime() {
        return enterNanoTime;
    }
}
