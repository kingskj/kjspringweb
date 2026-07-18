package com.turtlepick.agent.core;

import com.turtlepick.agent.core.bootstrap.AgentBootstrapService;
import com.turtlepick.agent.core.config.AgentConfig;
import com.turtlepick.agent.core.config.TurtlepickConfigLoader;
import com.turtlepick.agent.core.git.GitCommandRunner;
import com.turtlepick.agent.core.git.GitCommitHashProvider;
import com.turtlepick.agent.core.http.EngineMetaClient;
import com.turtlepick.agent.core.http.EngineLogReadyClient;
import com.turtlepick.agent.core.http.LogReadyJsonCodec;
import com.turtlepick.agent.core.http.MetaJsonCodec;
import com.turtlepick.agent.core.instrument.ApplicationMethodTransformer;
import com.turtlepick.agent.core.instrument.MyBatisMapperProxyTransformer;
import com.turtlepick.agent.core.instrument.SpringAopProxyInvokeTransformer;
import com.turtlepick.agent.core.instrument.SpringWebRequestTransformer;
import com.turtlepick.agent.core.instrument.TomcatFilterChainInterceptTransformer;
import com.turtlepick.agent.core.instrument.MethodProbeIndex;
import com.turtlepick.agent.core.instrument.MethodProbeIndexBuilder;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointRegistry;
import com.turtlepick.agent.core.state.EndpointResolver;
import com.turtlepick.agent.core.state.InterfaceMethodRegistry;
import com.turtlepick.agent.core.state.MethodMappingRegistry;
import com.turtlepick.agent.core.trace.AgentHttpBridge;
import com.turtlepick.agent.core.trace.AgentRuntimeController;
import com.turtlepick.agent.core.trace.ErrorArgCaptureOptions;
import com.turtlepick.agent.core.trace.RuntimeMethodBridge;
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
            InterfaceMethodRegistry interfaceMethodRegistry = new InterfaceMethodRegistry();
            GitCommandRunner gitCommandRunner = new GitCommandRunner();
            GitCommitHashProvider commitHashProvider = new GitCommitHashProvider(gitCommandRunner);
            MetaJsonCodec metaJsonCodec = new MetaJsonCodec();
            EngineMetaClient engineMetaClient = new EngineMetaClient(metaJsonCodec);
            EngineLogReadyClient logReadyClient = new EngineLogReadyClient(new LogReadyJsonCodec());
            AgentBootstrapService bootstrapService = new AgentBootstrapService(
                    stateHolder,
                    methodMappingRegistry,
                    endpointRegistry,
                    interfaceMethodRegistry,
                    commitHashProvider,
                    engineMetaClient
            );

            RuntimeMethodBridge.installStateHolder(stateHolder);
            RuntimeMethodBridge.installEndpointResolver(new EndpointResolver(endpointRegistry));
            RuntimeMethodBridge.installInterfaceMethodRegistry(interfaceMethodRegistry);
            RuntimeMethodBridge.installErrorMetaOptions(config.getUserFramePackages());
            RuntimeMethodBridge.installErrorArgOptions(new ErrorArgCaptureOptions(
                    config.isErrorArgsEnabled(),
                    config.getErrorArgsMaxLength(),
                    config.getErrorArgsExcludeClasses()
            ));
            RuntimeMethodBridge.installBusinessErrorConfig(config.getBusinessErrorConfig());

            ApplicationMethodTransformer applicationTransformer =
                    new ApplicationMethodTransformer(MethodProbeIndex.empty());
            boolean retransformSupported = inst.isRetransformClassesSupported();
            inst.addTransformer(applicationTransformer, retransformSupported);
            if (!retransformSupported) {
                AgentLog.warn("method probe retransform disabled cause=JVM_NOT_SUPPORTED");
            }
            inst.addTransformer(new SpringAopProxyInvokeTransformer(), false);
            inst.addTransformer(new MyBatisMapperProxyTransformer(), false);

            AgentRuntimeController runtimeController = new AgentRuntimeController(
                    inst,
                    config,
                    bootstrapService,
                    stateHolder,
                    methodMappingRegistry,
                    endpointRegistry,
                    interfaceMethodRegistry,
                    logReadyClient,
                    applicationTransformer,
                    new MethodProbeIndexBuilder());
            AgentHttpBridge.install(runtimeController);

            if (config.isInstrumentationHttp()) {
                inst.addTransformer(new TomcatFilterChainInterceptTransformer(), false);
                inst.addTransformer(new SpringWebRequestTransformer(), false);
            }

            AgentRuntimeController.ActivationResult result = runtimeController.bootstrapAndActivate();
            if (!result.isSuccess()) {
                AgentLog.warn("meta log_off"
                        + " status=" + result.getStatus()
                        + " agentId=" + result.getAgentId()
                        + " commitHash=" + result.getServerCommitHash()
                        + " reason=" + result.getReason()
                        + " resumeReceiver=true");
                return;
            }

            AgentLog.info("method probe installed"
                    + " commitHash=" + result.getServerCommitHash()
                    + " methodCount=" + result.getMethodCount()
                    + " endpointCount=" + result.getEndpointCount()
                    + " interfaceMethodCount=" + result.getInterfaceMethodCount()
                    + " declaredMethodCount=" + result.getDeclaredMethodCount()
                    + " retransformTransformed=" + result.getRetransformSummary().getTransformed()
                    + " retransformFailed=" + result.getRetransformSummary().getFailed()
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

