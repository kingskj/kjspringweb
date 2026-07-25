package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.config.BusinessErrorConfig;
import com.turtlepick.agent.core.config.SlowTraceConfig;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.InterfaceMethodRegistry;
import com.turtlepick.agent.core.state.ResolvedEndpoint;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Method;

public final class RuntimeMethodBridge {

    private static final int MAX_USER_FRAMES = 10;
    private static final String[] EMPTY_PACKAGES = new String[0];
    private static final ErrorArgCaptureOptions DEFAULT_ARG_OPTIONS =
            new ErrorArgCaptureOptions(true, 10000, new String[0]);

    private static volatile AgentStateHolder agentStateHolder;
    private static volatile EndpointResolver endpointResolver;
    private static volatile InterfaceMethodRegistry interfaceMethodRegistry;
    private static volatile String[] userFramePackages = EMPTY_PACKAGES;
    private static volatile ErrorArgCaptureOptions errorArgOptions = DEFAULT_ARG_OPTIONS;
    private static volatile BusinessErrorConfig businessErrorConfig = BusinessErrorConfig.disabled();
    private static volatile BusinessErrorMatcher businessErrorMatcher =
            new BusinessErrorMatcher(BusinessErrorConfig.disabled());
    private static volatile SlowTraceConfig slowTraceConfig = SlowTraceConfig.disabled();

    private RuntimeMethodBridge() {
    }

    public static void installStateHolder(AgentStateHolder holder) {
        agentStateHolder = holder;
    }

    public static void installEndpointResolver(EndpointResolver resolver) {
        endpointResolver = resolver;
    }

    public static void installInterfaceMethodRegistry(InterfaceMethodRegistry registry) {
        interfaceMethodRegistry = registry;
    }

    public static int enterInterfaceMethod(Object proxy, Method method) {
        return enterInterfaceMethod(proxy, method, null);
    }

    public static int enterInterfaceMethod(Object proxy, Method method, Object[] args) {
        try {
            InterfaceMethodRegistry registry = interfaceMethodRegistry;
            if (registry == null || registry.isEmpty()) {
                return 0;
            }
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null || context.isPendingHttpFlush()) {
                return 0;
            }
            InterfaceMethodRegistry.Match match = registry.lookup(proxy, method);
            if (match == null) {
                return 0;
            }
            // JPA Repository inherited frame → SQL attach 허용(true).
            enter(match.getMethodId(), match.getFqcnMethod(), args, true);
            return match.getMethodId();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("interface method enter failed cause=" + t.getClass().getSimpleName());
            return 0;
        }
    }

    public static int enterDeclaredInterfaceMethod(Method method) {
        return enterDeclaredInterfaceMethod(method, null);
    }

    public static int enterDeclaredInterfaceMethod(Method method, Object[] args) {
        try {
            InterfaceMethodRegistry registry = interfaceMethodRegistry;
            if (registry == null || registry.isDeclaredEmpty()) {
                return 0;
            }
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null || context.isPendingHttpFlush()) {
                return 0;
            }
            InterfaceMethodRegistry.Match match = registry.lookupDeclared(method);
            if (match == null) {
                return 0;
            }
            // MyBatis Mapper declared frame → SQL attach 허용(true).
            enter(match.getMethodId(), match.getFqcnMethod(), args, true);
            return match.getMethodId();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("declared interface method enter failed cause=" + t.getClass().getSimpleName());
            return 0;
        }
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

    public static void installBusinessErrorConfig(BusinessErrorConfig config) {
        BusinessErrorConfig effectiveConfig = config == null ? BusinessErrorConfig.disabled() : config;
        businessErrorConfig = effectiveConfig;
        businessErrorMatcher = new BusinessErrorMatcher(effectiveConfig);
    }

    public static void installSlowTraceConfig(SlowTraceConfig config) {
        slowTraceConfig = config == null ? SlowTraceConfig.disabled() : config;
    }

    public static void enter(int methodId, String fqcnMethod) {
        enter(methodId, fqcnMethod, null);
    }

    public static void enter(int methodId, String fqcnMethod, Object[] args) {
        // 일반 method probe(Controller/Service/Component) 경로 → SQL attach 불가(false).
        enter(methodId, fqcnMethod, args, false);
    }

    public static void enter(int methodId, String fqcnMethod, Object[] args, boolean sqlAttachAllowed) {
        RuntimeTraceContext context = TraceContextHolder.get();

        if (context != null && context.isPendingHttpFlush()) {
            return;
        }

        if (context == null) {
            AgentStateHolder holder = agentStateHolder;
            if (holder != null && !holder.isLogOn()) {
                return;
            }
            context = TraceContextHolder.getOrCreate();
        }

        boolean root = context.isEmpty();
        context.push(methodId, fqcnMethod, args, sqlAttachAllowed);

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
            if (!context.isPendingHttpFlush()) {
                TraceContextHolder.clear();
            }
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
                BusinessErrorMatcher matcher = businessErrorMatcher;
                if (matcher != null && matcher.matches(throwable)) {
                    context.markBusinessCandidate(top.getCallId(), top.getFqcnMethod(), meta);
                } else {
                    context.markError(top.getCallId(), top.getFqcnMethod(), meta, null);
                }
            }
        }

        long exitNanoTime = System.nanoTime();
        context.pop();
        context.addCompletedNode(top, exitNanoTime);

        if (context.isEmpty()) {
            context.markTraceEnd(exitNanoTime);
            if (context.isHttpTrace()) {
                context.markPendingHttpFlush();
            } else {
                flushAndClear(context);
            }
        }
    }

    public static void finishHttpRequest(Integer status, boolean exceptionPropagated) {
        finishHttpRequest(status, exceptionPropagated, System.nanoTime());
    }

    public static void finishHttpRequest(Integer status, boolean exceptionPropagated, long finishNanoTime) {
        try {
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null) {
                return;
            }
            if (context.isPendingHttpFlush()) {
                // status는 safeExit(C안)에서 committed면 최종값, unknown이면 null로 이미 결정됨.
                context.attachHttpStatus(status);
                context.attachHttpExitNanoTime(finishNanoTime);
                flushAndClear(context);
                return;
            }
            if (context.isEmpty()) {
                TraceContextHolder.clear();
                return;
            }
            AgentLog.warn("http finish before trace root exit entryMethodId=" + context.getEntryMethodId());
            context.clear();
            TraceContextHolder.clear();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            AgentLog.warn("http finish bridge failed cause=" + t.getClass().getSimpleName());
            TraceContextHolder.clear();
        }
    }

    private static void flushAndClear(RuntimeTraceContext context) {
        try {
            AgentStateHolder holder = agentStateHolder;
            if (holder == null || holder.isLogOn()) {
                context.finalizeBusinessErrorDecision(businessErrorConfig);
                context.finalizeSlowDecision(slowTraceConfig);
                context.materializeParams(errorArgOptions);
                TraceLogWriter.write(context);
            }
        } catch (Throwable t) {
            AgentLog.warn("trace flush failed methodId=" + context.getEntryMethodId()
                    + " cause=" + t.getClass().getSimpleName());
        } finally {
            TraceContextHolder.clear();
        }
    }

    private static void handleExitBridgeFailure(int methodId, Throwable throwable) {
        AgentLog.warn("method exit bridge failed methodId=" + methodId
                + " cause=" + throwable.getClass().getSimpleName());
        TraceContextHolder.clear();
    }

}
