package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.instrument.MethodSignatureParser;
import com.turtlepick.agent.core.instrument.ParsedMethodSignature;
import com.turtlepick.agent.core.state.ResolvedEndpoint;
import com.turtlepick.agent.core.util.AgentLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RuntimeTraceContext {

    // 유닛4c: emx 계산 실패 케이스를 eniFqcnMethod 기준으로 최초 1회만 warn (에러 폭주 시 로그 폭탄 방지)
    private static final MethodSignatureParser EMX_PARSER = new MethodSignatureParser();
    private static final Set<String> EMX_WARNED = ConcurrentHashMap.newKeySet();

    private final String traceId;
    private final Deque<MethodFrame> stack = new ArrayDeque<MethodFrame>();
    private final List<CompletedNode> nodes = new ArrayList<CompletedNode>();
    private int nextCallId;
    private long traceStartNanoTime;
    private long occurredAtMs;
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
    private List<StackFrame> stackFrames = new ArrayList<StackFrame>();
    private List<UserFrame> userFrames = new ArrayList<UserFrame>();
    private String[] errorArgs;
    private boolean errorNodeMismatch;

    public RuntimeTraceContext() {
        this.traceId = UUID.randomUUID().toString();
    }

    public String getTraceId() {
        return traceId;
    }

    public long getOccurredAtMs() {
        return occurredAtMs;
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

    public List<StackFrame> snapshotStackFrames() {
        return new ArrayList<StackFrame>(stackFrames);
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
            occurredAtMs = System.currentTimeMillis();
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

    public void markError(int callId, String eniFqcnMethod, ErrorMeta meta, String[] args) {
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
            this.stackFrames = new ArrayList<StackFrame>(meta.getStackFrames());
            this.userFrames = new ArrayList<UserFrame>(meta.getUserFrames());
            this.errorNodeMismatch = computeErrorNodeMismatch(eniFqcnMethod, this.userFrames);
        }
        this.errorArgs = args == null || args.length == 0 ? null : copyOf(args);
    }

    // 유닛4c: eni 노드 method와 source 기준 userFrame method가 다르면 true.
    // 계산 불가(source 없음/파싱 실패/예외)는 false + warn-once (true 오탐이 화면을 오염시키지 않게).
    private static boolean computeErrorNodeMismatch(String eniFqcnMethod, List<UserFrame> userFrames) {
        try {
            UserFrame source = SourceUserFrameResolver.resolve(userFrames);
            if (source == null) {
                warnEmxOnce(eniFqcnMethod, "no_source_user_frame");
                return false;
            }
            if (eniFqcnMethod == null) {
                warnEmxOnce(eniFqcnMethod, "eni_fqcn_null");
                return false;
            }
            ParsedMethodSignature eni = EMX_PARSER.parse(eniFqcnMethod);
            if (eni == null) {
                warnEmxOnce(eniFqcnMethod, "eni_parse_failed");
                return false;
            }
            String eniClass = normalizeProxyClass(eni.getClassName());
            String eniMethod = eni.getMethodName();
            String sourceClass = normalizeProxyClass(source.getDeclaringClass());
            String sourceMethod = source.getMethodName();
            if (eniClass == null || eniMethod == null || sourceClass == null || sourceMethod == null) {
                warnEmxOnce(eniFqcnMethod, "null_class_or_method");
                return false;
            }
            return !(eniClass.equals(sourceClass) && eniMethod.equals(sourceMethod));
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            warnEmxOnce(eniFqcnMethod, "compute_error:" + t.getClass().getSimpleName());
            return false;
        }
    }

    // 프록시 class(BoardService$$SpringCGLIB$$0)는 원본 앞부분(BoardService)으로 정규화 후 비교한다.
    private static String normalizeProxyClass(String className) {
        if (className == null) {
            return null;
        }
        int idx = className.indexOf("$$");
        return idx >= 0 ? className.substring(0, idx) : className;
    }

    private static void warnEmxOnce(String eniFqcnMethod, String reason) {
        String key = eniFqcnMethod == null ? "<null>" : eniFqcnMethod;
        if (EMX_WARNED.add(key)) {
            AgentLog.warn("emx compute skipped cause=" + reason + " eni=" + key);
        }
    }

    public boolean isErrorNodeMismatch() {
        return errorNodeMismatch;
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
        occurredAtMs = 0L;
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
        stackFrames.clear();
        userFrames.clear();
        errorArgs = null;
        errorNodeMismatch = false;
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
