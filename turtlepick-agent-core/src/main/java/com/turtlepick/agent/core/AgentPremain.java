package com.turtlepick.agent.core;

import com.turtlepick.agent.core.bootstrap.AgentBootstrapService;
import com.turtlepick.agent.core.bootstrap.BootstrapResult;
import com.turtlepick.agent.core.config.AgentConfig;
import com.turtlepick.agent.core.config.TurtlepickConfigLoader;
import com.turtlepick.agent.core.git.GitCommandRunner;
import com.turtlepick.agent.core.git.GitCommitHashProvider;
import com.turtlepick.agent.core.http.EngineMetaClient;
import com.turtlepick.agent.core.http.MetaJsonCodec;
import com.turtlepick.agent.core.instrument.ApplicationMethodTransformer;
import com.turtlepick.agent.core.instrument.SpringWebRequestTransformer;
import com.turtlepick.agent.core.instrument.MethodProbeIndex;
import com.turtlepick.agent.core.instrument.MethodProbeIndexBuilder;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointRegistry;
import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.MethodMappingRegistry;
import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
import com.turtlepick.agent.core.trace.TraceLogWriter;
import com.turtlepick.agent.core.util.AgentLog;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

public final class AgentPremain {

    private AgentPremain() {
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        AgentLog.info("begin");

        try {
            File agentJarFile = resolveAgentJarFile();
            inst.appendToSystemClassLoaderSearch(new JarFile(agentJarFile));

            TurtlepickConfigLoader configLoader = new TurtlepickConfigLoader();
            AgentConfig config = configLoader.load(agentJarFile);

            AgentStateHolder stateHolder = new AgentStateHolder();
            MethodMappingRegistry methodMappingRegistry = new MethodMappingRegistry();
            EndpointRegistry endpointRegistry = new EndpointRegistry();
            GitCommandRunner gitCommandRunner = new GitCommandRunner();
            GitCommitHashProvider commitHashProvider = new GitCommitHashProvider(gitCommandRunner);
            MetaJsonCodec metaJsonCodec = new MetaJsonCodec();
            EngineMetaClient engineMetaClient = new EngineMetaClient(metaJsonCodec);
            AgentBootstrapService bootstrapService = new AgentBootstrapService(
                    stateHolder,
                    methodMappingRegistry,
                    endpointRegistry,
                    commitHashProvider,
                    engineMetaClient
            );

            BootstrapResult result = bootstrapService.bootstrap(config);
            if (!result.isSuccess()) {
                AgentLog.warn("meta log_off"
                        + " status=" + result.getStatus()
                        + " agentId=" + result.getAgentId()
                        + " commitHash=" + result.getCommitHash()
                        + " reason=" + result.getReason());
                return;
            }

            RuntimeMethodBridge.installEndpointResolver(new EndpointResolver(endpointRegistry));
            TraceLogWriter.install(config.getLoggingDir(), config.getRollingIntervalMinutes());
            MethodProbeIndex probeIndex =
                    new MethodProbeIndexBuilder().build(methodMappingRegistry.snapshot());

            inst.addTransformer(new ApplicationMethodTransformer(probeIndex), false);
            if (config.isInstrumentationHttp()) {
                inst.addTransformer(new SpringWebRequestTransformer(), false);
            }

            AgentLog.info("method probe installed"
                    + " commitHash=" + result.getCommitHash()
                    + " methodCount=" + result.getMethodCount()
                    + " endpointCount=" + result.getEndpointCount()
                    + " httpInstrumentation=" + config.isInstrumentationHttp());
        } catch (Throwable t) {
            AgentLog.error("startup failed; agent disabled", t);
        }
    }

    private static File resolveAgentJarFile() {
        try {
            URL location = AgentPremain.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                throw new IllegalStateException("agent code source location is null");
            }
            return new File(location.toURI()).getAbsoluteFile();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("failed to resolve agent jar path", e);
        }
    }
}
