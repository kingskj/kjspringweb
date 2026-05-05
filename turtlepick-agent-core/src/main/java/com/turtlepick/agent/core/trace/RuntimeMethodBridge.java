package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.ResolvedEndpoint;
import com.turtlepick.agent.core.util.AgentLog;

public final class RuntimeMethodBridge {

    private static final int MAX_USER_FRAMES = 10;
    private static final String[] EMPTY_PACKAGES = new String[0];
    private static final ErrorArgCaptureOptions DEFAULT_ARG_OPTIONS =
            new ErrorArgCaptureOptions(true, 10000, new String[0]);

    private static volatile AgentStateHolder agentStateHolder;
    private static volatile EndpointResolver endpointResolver;
    private static volatile String[] userFramePackages = EMPTY_PACKAGES;
    private static volatile ErrorArgCaptureOptions errorArgOptions = DEFAULT_ARG_OPTIONS;

    private RuntimeMethodBridge() {
    }

    public static void installStateHolder(AgentStateHolder holder) {
        agentStateHolder = holder;
    }

    public static void installEndpointResolver(EndpointResolver resolver) {
        endpointResolver = resolver;
    }

    public static void installErrorMetaOptions(String[] packages) {
        if (packages == null || packages.length == 0) {
            userFramePackages = EMPTY_PACKAGES;
            return;
        }
        String[] copy = new String[packages.length];
        System.arraycopy(packages, 0, copy, 0, packages.length);
        userFramePackages = copy;
    }

    public static void installErrorArgOptions(ErrorArgCaptureOptions options) {
        errorArgOptions = options == null ? DEFAULT_ARG_OPTIONS : options;
    }

    public static void enter(int methodId, String fqcnMethod) {
        RuntimeTraceContext context = TraceContextHolder.get();

        if (context == null) {
            AgentStateHolder holder = agentStateHolder;
            if (holder != null && !holder.isLogOn()) {
                return;
            }
            context = TraceContextHolder.getOrCreate();
        }

        boolean root = context.isEmpty();
        context.push(methodId, fqcnMethod);

        if (root) {
            EndpointResolver resolver = endpointResolver;
            if (resolver != null) {
                HttpRequestContext httpRequestContext = HttpRequestContextHolder.get();
                ResolvedEndpoint resolvedEndpoint = resolver.resolve(methodId, httpRequestContext);
                context.attachResolvedEndpoint(resolvedEndpoint, httpRequestContext);
            }
        }
    }

    public static void exit(int methodId, boolean isError) {
        try {
            exitUnsafe(methodId, isError, null, null);
        } catch (Throwable t) {
            handleExitBridgeFailure(methodId, t);
        }
    }

    public static void exit(int methodId, Throwable throwable, Object[] args) {
        try {
            exitUnsafe(methodId, true, throwable, args);
        } catch (Throwable t) {
            handleExitBridgeFailure(methodId, t);
        }
    }

    private static void exitUnsafe(int methodId, boolean isError, Throwable throwable, Object[] args) {
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

        if (isError) {
            if (throwable == null) {
                context.markError();
            } else {
                ErrorMeta meta = ErrorMetaExtractor.extract(throwable, userFramePackages, MAX_USER_FRAMES);
                String[] errorArgs = ErrorArgExtractor.extract(args, errorArgOptions);
                context.markError(top.getCallId(), meta, errorArgs);
            }
        }

        long exitNanoTime = System.nanoTime();
        context.pop();
        context.addCompletedNode(top, exitNanoTime);

        if (context.isEmpty()) {
            try {
                AgentStateHolder holder = agentStateHolder;
                if (holder == null || holder.isLogOn()) {
                    TraceLogWriter.write(context);
                }
            } catch (Throwable t) {
                AgentLog.warn("trace flush failed methodId=" + context.getEntryMethodId()
                        + " cause=" + t.getClass().getSimpleName());
            } finally {
                TraceContextHolder.clear();
            }
        }
    }

    private static void handleExitBridgeFailure(int methodId, Throwable throwable) {
        AgentLog.warn("method exit bridge failed methodId=" + methodId
                + " cause=" + throwable.getClass().getSimpleName());
        TraceContextHolder.clear();
    }

}
