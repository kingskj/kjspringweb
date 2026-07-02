package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.bootstrap.AgentBootstrapService;
import com.turtlepick.agent.core.bootstrap.BootstrapResult;
import com.turtlepick.agent.core.config.AgentConfig;
import com.turtlepick.agent.core.http.EngineLogReadyClient;
import com.turtlepick.agent.core.instrument.ApplicationMethodTransformer;
import com.turtlepick.agent.core.instrument.MethodProbeIndex;
import com.turtlepick.agent.core.instrument.MethodProbeIndexBuilder;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointRegistry;
import com.turtlepick.agent.core.state.InterfaceMethodRegistry;
import com.turtlepick.agent.core.state.MethodMappingRegistry;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;

public final class AgentRuntimeController {

    private final Object lock = new Object();
    private final Instrumentation instrumentation;
    private final AgentConfig config;
    private final AgentBootstrapService bootstrapService;
    private final AgentStateHolder stateHolder;
    private final MethodMappingRegistry methodMappingRegistry;
    private final EndpointRegistry endpointRegistry;
    private final InterfaceMethodRegistry interfaceMethodRegistry;
    private final EngineLogReadyClient logReadyClient;
    private final ApplicationMethodTransformer applicationTransformer;
    private final MethodProbeIndexBuilder indexBuilder;

    private volatile String serverCommitHash;
    private String traceWriterCommitHash;
    private LogReadyNotifier logReadyNotifier;

    public AgentRuntimeController(
            Instrumentation instrumentation,
            AgentConfig config,
            AgentBootstrapService bootstrapService,
            AgentStateHolder stateHolder,
            MethodMappingRegistry methodMappingRegistry,
            EndpointRegistry endpointRegistry,
            InterfaceMethodRegistry interfaceMethodRegistry,
            EngineLogReadyClient logReadyClient,
            ApplicationMethodTransformer applicationTransformer,
            MethodProbeIndexBuilder indexBuilder) {
        this.instrumentation = instrumentation;
        this.config = config;
        this.bootstrapService = bootstrapService;
        this.stateHolder = stateHolder;
        this.methodMappingRegistry = methodMappingRegistry;
        this.endpointRegistry = endpointRegistry;
        this.interfaceMethodRegistry = interfaceMethodRegistry;
        this.logReadyClient = logReadyClient;
        this.applicationTransformer = applicationTransformer;
        this.indexBuilder = indexBuilder;
    }

    public ActivationResult bootstrapAndActivate() {
        return reloadMetaAndActivate("BOOTSTRAP", null);
    }

    public ActivationResult reloadMetaAndActivate(String trigger, String expectedCommitHash) {
        synchronized (lock) {
            String normalizedTrigger = trimToNull(trigger);
            if (normalizedTrigger == null) {
                normalizedTrigger = "RELOAD_META";
            }

            BootstrapResult result = bootstrapService.bootstrap(config);
            String resultCommitHash = trimToNull(result.getCommitHash());
            if (resultCommitHash != null) {
                serverCommitHash = resultCommitHash;
            }

            if (!result.isSuccess()) {
                return ActivationResult.failure(result, normalizedTrigger);
            }

            String normalizedExpectedCommitHash = trimToNull(expectedCommitHash);
            if (normalizedExpectedCommitHash != null && !normalizedExpectedCommitHash.equals(resultCommitHash)) {
                stateHolder.markLogOff();
                methodMappingRegistry.clear();
                endpointRegistry.clear();
                interfaceMethodRegistry.clear();
                AgentLog.warn("meta reload rejected reason=COMMIT_MISMATCH"
                        + " trigger=" + normalizedTrigger
                        + " requested=" + normalizedExpectedCommitHash
                        + " actual=" + resultCommitHash);
                return ActivationResult.failure(resultCommitHash, result.getStatus(), "COMMIT_MISMATCH",
                        result.getAgentId(), normalizedTrigger);
            }

            MethodProbeIndex nextIndex = indexBuilder.build(methodMappingRegistry.snapshot());
            MethodProbeIndex previousIndex = applicationTransformer.updateIndex(nextIndex);
            RetransformSummary retransformSummary = retransformLoadedClasses(previousIndex, nextIndex);

            LogReadyNotifier notifier = ensureLogReadyNotifier(resultCommitHash);
            notifier.start();
            stateHolder.markLogOn();

            AgentLog.info("agent state LOG_ON"
                    + " reason=" + normalizedTrigger
                    + " commitHash=" + resultCommitHash
                    + " methodCount=" + result.getMethodCount()
                    + " endpointCount=" + result.getEndpointCount()
                    + " interfaceMethodCount=" + result.getInterfaceMethodCount()
                    + " declaredMethodCount=" + result.getDeclaredMethodCount()
                    + " retransformTransformed=" + retransformSummary.getTransformed()
                    + " retransformSkipped=" + retransformSummary.getSkipped()
                    + " retransformFailed=" + retransformSummary.getFailed());

            return ActivationResult.success(result, normalizedTrigger, retransformSummary);
        }
    }

    public String getServerCommitHash() {
        return serverCommitHash;
    }

    public void markLogOff() {
        stateHolder.markLogOff();
    }

    private LogReadyNotifier ensureLogReadyNotifier(String commitHash) {
        String normalizedCommitHash = trimToNull(commitHash);
        if (normalizedCommitHash == null) {
            throw new IllegalStateException("commitHash must not be blank");
        }

        if (logReadyNotifier != null && normalizedCommitHash.equals(traceWriterCommitHash)) {
            return logReadyNotifier;
        }

        if (logReadyNotifier != null) {
            logReadyNotifier.shutdown();
        }

        LogReadyNotifier nextNotifier = new LogReadyNotifier(logReadyClient, config, normalizedCommitHash, stateHolder);
        TraceLogWriter.install(
                config.getLoggingDir(),
                config.getRollingIntervalMinutes(),
                normalizedCommitHash,
                config.isVerboseFieldNames(),
                nextNotifier);
        logReadyNotifier = nextNotifier;
        traceWriterCommitHash = normalizedCommitHash;
        return nextNotifier;
    }

    private RetransformSummary retransformLoadedClasses(MethodProbeIndex previousIndex, MethodProbeIndex nextIndex) {
        HashSet<String> targetClassNames = new HashSet<String>();
        if (previousIndex != null) {
            targetClassNames.addAll(previousIndex.classNames());
        }
        if (nextIndex != null) {
            targetClassNames.addAll(nextIndex.classNames());
        }

        if (targetClassNames.isEmpty()) {
            return new RetransformSummary(0, 0, 0);
        }
        if (!instrumentation.isRetransformClassesSupported()) {
            AgentLog.warn("method probe retransform skipped cause=RETRANSFORM_NOT_SUPPORTED"
                    + " targetClassCount=" + targetClassNames.size());
            return new RetransformSummary(0, targetClassNames.size(), 0);
        }

        int transformed = 0;
        int skipped = 0;
        int failed = 0;
        Class<?>[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (int i = 0; i < loadedClasses.length; i++) {
            Class<?> loadedClass = loadedClasses[i];
            if (loadedClass == null || !targetClassNames.contains(loadedClass.getName())) {
                continue;
            }
            if (loadedClass.getClassLoader() == null || !instrumentation.isModifiableClass(loadedClass)) {
                skipped++;
                continue;
            }

            try {
                instrumentation.retransformClasses(loadedClass);
                transformed++;
            } catch (UnmodifiableClassException e) {
                failed++;
                AgentLog.warn("method probe retransform failed className=" + loadedClass.getName()
                        + " cause=UnmodifiableClassException:" + safeMessage(e));
            } catch (Throwable t) {
                if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                    throw (Error) t;
                }
                failed++;
                AgentLog.warn("method probe retransform failed className=" + loadedClass.getName()
                        + " cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
            }
        }
        return new RetransformSummary(transformed, skipped, failed);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }

    public static final class ActivationResult {
        private final boolean success;
        private final String trigger;
        private final String serverCommitHash;
        private final String status;
        private final String reason;
        private final String agentId;
        private final int methodCount;
        private final int endpointCount;
        private final int interfaceMethodCount;
        private final int declaredMethodCount;
        private final RetransformSummary retransformSummary;

        private ActivationResult(
                boolean success,
                String trigger,
                String serverCommitHash,
                String status,
                String reason,
                String agentId,
                int methodCount,
                int endpointCount,
                int interfaceMethodCount,
                int declaredMethodCount,
                RetransformSummary retransformSummary) {
            this.success = success;
            this.trigger = trigger;
            this.serverCommitHash = serverCommitHash;
            this.status = status;
            this.reason = reason;
            this.agentId = agentId;
            this.methodCount = methodCount;
            this.endpointCount = endpointCount;
            this.interfaceMethodCount = interfaceMethodCount;
            this.declaredMethodCount = declaredMethodCount;
            this.retransformSummary = retransformSummary == null
                    ? new RetransformSummary(0, 0, 0)
                    : retransformSummary;
        }

        static ActivationResult success(BootstrapResult result, String trigger, RetransformSummary summary) {
            return new ActivationResult(true, trigger, result.getCommitHash(), result.getStatus(), null,
                    result.getAgentId(), result.getMethodCount(), result.getEndpointCount(),
                    result.getInterfaceMethodCount(), result.getDeclaredMethodCount(), summary);
        }

        static ActivationResult failure(BootstrapResult result, String trigger) {
            return new ActivationResult(false, trigger, result.getCommitHash(), result.getStatus(), result.getReason(),
                    result.getAgentId(), 0, 0, 0, 0, new RetransformSummary(0, 0, 0));
        }

        public static ActivationResult failure(String commitHash, String status, String reason,
                                               String agentId, String trigger) {
            return new ActivationResult(false, trigger, commitHash, status, reason, agentId,
                    0, 0, 0, 0, new RetransformSummary(0, 0, 0));
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTrigger() {
            return trigger;
        }

        public String getServerCommitHash() {
            return serverCommitHash;
        }

        public String getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public String getAgentId() {
            return agentId;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public int getEndpointCount() {
            return endpointCount;
        }

        public int getInterfaceMethodCount() {
            return interfaceMethodCount;
        }

        public int getDeclaredMethodCount() {
            return declaredMethodCount;
        }

        public RetransformSummary getRetransformSummary() {
            return retransformSummary;
        }
    }

    public static final class RetransformSummary {
        private final int transformed;
        private final int skipped;
        private final int failed;

        private RetransformSummary(int transformed, int skipped, int failed) {
            this.transformed = transformed;
            this.skipped = skipped;
            this.failed = failed;
        }

        public int getTransformed() {
            return transformed;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }
    }
}
