package com.turtlepick.agent.core.trace;

public final class HttpRequestContext {

    private final String method;
    private final String requestUri;

    public HttpRequestContext(String method, String requestUri) {
        this.method = method;
        this.requestUri = requestUri;
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUri() {
        return requestUri;
    }
}
