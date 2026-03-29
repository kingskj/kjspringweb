package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.ResolvedEndpoint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class RuntimeTraceContext {

    private final String traceId;
    private final Deque<MethodFrame> stack = new ArrayDeque<MethodFrame>();
    private int entryMethodId;
    private String entryFqcnMethod;
    private Integer endpointId;
    private String endpointEntryType;
    private String endpointEntryKey;
    private String endpointHttpMethod;
    private String requestMethod;
    private String requestUri;
    private String endpointResolutionStatus;

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

    public Integer getEndpointId() {
        return endpointId;
    }

    public String getEndpointEntryType() {
        return endpointEntryType;
    }

    public String getEndpointEntryKey() {
        return endpointEntryKey;
    }

    public String getEndpointHttpMethod() {
        return endpointHttpMethod;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public String getEndpointResolutionStatus() {
        return endpointResolutionStatus;
    }

    public void push(int methodId, String fqcnMethod) {
        if (stack.isEmpty()) {
            this.entryMethodId = methodId;
            this.entryFqcnMethod = fqcnMethod;
        }
        stack.push(new MethodFrame(methodId, fqcnMethod, System.nanoTime()));
    }

    public void attachResolvedEndpoint(ResolvedEndpoint resolvedEndpoint, HttpRequestContext httpRequestContext) {
        if (resolvedEndpoint != null) {
            this.endpointId = resolvedEndpoint.getEndpointId();
            this.endpointEntryType = resolvedEndpoint.getEntryType();
            this.endpointEntryKey = resolvedEndpoint.getEntryKey();
            this.endpointHttpMethod = resolvedEndpoint.getHttpMethod();
            this.endpointResolutionStatus = resolvedEndpoint.getResolutionStatus();
        } else {
            this.endpointId = null;
            this.endpointEntryType = null;
            this.endpointEntryKey = null;
            this.endpointHttpMethod = null;
            this.endpointResolutionStatus = null;
        }

        if (httpRequestContext != null) {
            this.requestMethod = httpRequestContext.getMethod();
            this.requestUri = httpRequestContext.getRequestUri();
        } else {
            this.requestMethod = null;
            this.requestUri = null;
        }
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
        entryMethodId = 0;
        entryFqcnMethod = null;
        endpointId = null;
        endpointEntryType = null;
        endpointEntryKey = null;
        endpointHttpMethod = null;
        requestMethod = null;
        requestUri = null;
        endpointResolutionStatus = null;
    }
}
