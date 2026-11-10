package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.config.BusinessErrorConfig;
import com.turtlepick.agent.core.config.SlowTraceConfig;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.InterfaceMethodRegistry;
import com.turtlepick.agent.core.state.ResolvedEndpoint;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class RuntimeMethodBridge {

    private static final int MAX_USER_FRAMES = 10;
    private static final String[] EMPTY_PACKAGES = new String[0];
    private static final ErrorArgCaptureOptions DEFAULT_ARG_OPTIONS =
            new ErrorArgCaptureOptions(true, 10000, new String[0]);

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    // 이름 비교라 Spring Data 의존이 없고, ClassValue라 classloader를 붙잡지 않는다.
    private static final ClassValue<Boolean> SPRING_DATA_REPOSITORY_TYPE = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return Boolean.valueOf(hasInterfaceNamed(type, SPRING_DATA_REPOSITORY, new HashSet<Class<?>>()));
        }
    };

    private static volatile boolean batchHookWarned;
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
            if (registry == null || registry.isAllEmpty()) {
                return 0;
            }
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null || context.isPendingHttpFlush()) {
                return 0;
            }
            InterfaceMethodRegistry.Match match = registry.lookup(proxy, method);
            if (match == null && method != null && isSpringDataRepositoryType(method.getDeclaringClass())) {
                // @Query 등 repository interface에 직접 선언된 DECLARED 메서드.
                // Service interface·MyBatis Mapper·custom fragment와의 중복 노드를 막기 위해
                // 선언 interface가 Spring Data Repository 하위일 때만 declared 표를 본다.
                match = registry.lookupDeclared(method);
            }
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

    private static boolean isSpringDataRepositoryType(Class<?> type) {
        return type != null && type.isInterface() && SPRING_DATA_REPOSITORY_TYPE.get(type).booleanValue();
    }

    private static boolean hasInterfaceNamed(Class<?> type, String name, Set<Class<?>> visited) {
        Class<?>[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> itf = interfaces[i];
            if (!visited.add(itf)) {
                continue;
            }
            if (name.equals(itf.getName()) || hasInterfaceNamed(itf, name, visited)) {
                return true;
            }
        }
        return false;
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

        boolean root = context == null || context.isEmpty();
        if (root && !shouldStartRoot(methodId)) {
            return;
        }

        if (context == null) {
            AgentStateHolder holder = agentStateHolder;
            if (holder != null && !holder.isLogOn()) {
                return;
            }
            context = TraceContextHolder.getOrCreate();
        }

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

    // HTTP 문맥 안에서는 메타에 등록된 HTTP 진입 메서드만 trace 루트가 된다.
    // 핸들러보다 먼저 실행되는 @ModelAttribute·선행 서비스 호출이 루트를 선점하면
    // 루트 종료 후 pending 상태가 되어 실제 핸들러가 무시된다. HTTP 문맥 밖은 기존과 같다.
    private static boolean shouldStartRoot(int methodId) {
        if (HttpRequestContextHolder.get() == null) {
            return true;
        }
        EndpointResolver resolver = endpointResolver;
        return resolver != null && resolver.hasHttpEntry(methodId);
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

    // AbstractStep.execute 정상 반환 시점. 최종 FAILED이고 Throwable이 기록된 step만 현재 trace의
    // top frame에 에러로 남긴다. Batch의 실패 판정이라 business 규칙과 무관하게 markError로 표시한다.
    public static void onBatchStepReturned(Object stepExecution) {
        try {
            if (stepExecution == null) {
                return;
            }
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null || context.isPendingHttpFlush()) {
                return;
            }
            MethodFrame top = context.peek();
            if (top == null) {
                return;
            }
            BatchStepAccess access = BatchStepAccess.CACHE.get(stepExecution.getClass());
            if (!access.isSupported() || !"FAILED".equals(access.statusName(stepExecution))) {
                return;
            }
            Throwable failure = access.firstFailure(stepExecution);
            if (failure == null) {
                return;
            }
            ErrorMeta meta = ErrorMetaExtractor.extract(failure, userFramePackages, MAX_USER_FRAMES);
            context.markError(top.getCallId(), top.getFqcnMethod(), meta, null);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            if (!batchHookWarned) {
                batchHookWarned = true;
                AgentLog.warn("batch step hook failed cause=" + t.getClass().getSimpleName());
            }
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
