package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.ResolvedEndpoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class RuntimeTraceContext {

    private final String traceId;
    private final Deque<MethodFrame> stack = new ArrayDeque<MethodFrame>();
    private final List<CompletedNode> nodes = new ArrayList<CompletedNode>();
    private int nextCallId;
    private long traceStartNanoTime;
    private int entryMethodId;
    private String entryFqcnMethod;
    private Integer endpointId;
    private String endpointEntryType;
    private String endpointEntryKey;
    private String endpointHttpMethod;
    private String requestMethod;
    private String requestUri;
    private String endpointResolutionStatus;
    private boolean hasError;
    private Integer errorCallId;
    private String exceptionClass;
    private String exceptionMessage;
    private String rootExceptionClass;
    private String rootExceptionMessage;
    private List<UserFrame> userFrames = new ArrayList<UserFrame>();
    private String[] errorArgs;

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

    public boolean hasError() {
        return hasError;
    }

    public Integer getErrorCallId() {
        return errorCallId;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getRootExceptionClass() {
        return rootExceptionClass;
    }

    public String getRootExceptionMessage() {
        return rootExceptionMessage;
    }

    public List<UserFrame> snapshotUserFrames() {
        return new ArrayList<UserFrame>(userFrames);
    }

    public String[] snapshotErrorArgs() {
        return copyOf(errorArgs);
    }

    public List<CompletedNode> snapshotNodes() {
        return new ArrayList<CompletedNode>(nodes);
    }

    public void push(int methodId, String fqcnMethod) {
        long now = System.nanoTime();
        int parentCallId;
        if (stack.isEmpty()) {
            traceStartNanoTime = now;
            parentCallId = 0;
            this.entryMethodId = methodId;
            this.entryFqcnMethod = fqcnMethod;
        } else {
            MethodFrame parent = stack.peek();
            parentCallId = parent == null ? 0 : parent.getCallId();
        }

        int callId = ++nextCallId;
        stack.push(new MethodFrame(callId, parentCallId, methodId, fqcnMethod, now));
    }

    public void addCompletedNode(MethodFrame frame, long exitNanoTime) {
        long startOffsetMs = (frame.getStartNanoTime() - traceStartNanoTime) / 1000000L;
        long endOffsetMs = (exitNanoTime - traceStartNanoTime) / 1000000L;
        nodes.add(new CompletedNode(
                frame.getCallId(),
                frame.getParentCallId(),
                frame.getMethodId(),
                frame.getFqcnMethod(),
                startOffsetMs,
                endOffsetMs
        ));
    }

    public void markError() {
        hasError = true;
    }

    public void markError(int callId, ErrorMeta meta, String[] args) {
        hasError = true;
        if (errorCallId != null) {
            return;
        }
        errorCallId = Integer.valueOf(callId);
        if (meta != null) {
            this.exceptionClass = meta.getExceptionClass();
            this.exceptionMessage = meta.getExceptionMessage();
            this.rootExceptionClass = meta.getRootExceptionClass();
            this.rootExceptionMessage = meta.getRootExceptionMessage();
            this.userFrames = new ArrayList<UserFrame>(meta.getUserFrames());
        }
        this.errorArgs = args == null || args.length == 0 ? null : copyOf(args);
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
        nodes.clear();
        nextCallId = 0;
        traceStartNanoTime = 0L;
        entryMethodId = 0;
        entryFqcnMethod = null;
        endpointId = null;
        endpointEntryType = null;
        endpointEntryKey = null;
        endpointHttpMethod = null;
        requestMethod = null;
        requestUri = null;
        endpointResolutionStatus = null;
        hasError = false;
        errorCallId = null;
        exceptionClass = null;
        exceptionMessage = null;
        rootExceptionClass = null;
        rootExceptionMessage = null;
        userFrames.clear();
        errorArgs = null;
    }

    private static String[] copyOf(String[] value) {
        if (value == null || value.length == 0) {
            return null;
        }
        String[] copy = new String[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return copy;
    }
}
