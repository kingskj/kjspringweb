package com.turtlepick.agent.core.bootstrap;

import com.turtlepick.agent.core.config.AgentConfig;
import com.turtlepick.agent.core.git.GitCommitHashProvider;
import com.turtlepick.agent.core.http.EngineMetaClient;
import com.turtlepick.agent.core.http.MetaResponse;
import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.state.EndpointRegistry;
import com.turtlepick.agent.core.state.InterfaceMethodRegistry;
import com.turtlepick.agent.core.state.MethodMappingRegistry;
import com.turtlepick.agent.core.util.AgentLog;

public final class AgentBootstrapService {

    private final AgentStateHolder stateHolder;
    private final MethodMappingRegistry methodMappingRegistry;
    private final EndpointRegistry endpointRegistry;
    private final InterfaceMethodRegistry interfaceMethodRegistry;
    private final GitCommitHashProvider commitHashProvider;
    private final EngineMetaClient engineMetaClient;

    public AgentBootstrapService(
            AgentStateHolder stateHolder,
            MethodMappingRegistry methodMappingRegistry,
            EndpointRegistry endpointRegistry,
            InterfaceMethodRegistry interfaceMethodRegistry,
            GitCommitHashProvider commitHashProvider,
            EngineMetaClient engineMetaClient) {
        this.stateHolder = stateHolder;
        this.methodMappingRegistry = methodMappingRegistry;
        this.endpointRegistry = endpointRegistry;
        this.interfaceMethodRegistry = interfaceMethodRegistry;
        this.commitHashProvider = commitHashProvider;
        this.engineMetaClient = engineMetaClient;
    }

    public BootstrapResult bootstrap(AgentConfig config) {
        stateHolder.markLogOff();
        methodMappingRegistry.clear();
        endpointRegistry.clear();
        interfaceMethodRegistry.clear();

        String commitHash = null;
        try {
            commitHash = commitHashProvider.resolveFullCommitHash(config);
            MetaResponse response = engineMetaClient.requestMeta(config, commitHash);

            if (!response.isOk()) {
                stateHolder.markLogOff();
                methodMappingRegistry.clear();
                endpointRegistry.clear();
                interfaceMethodRegistry.clear();
                return BootstrapResult.failure(commitHash, response.getStatus(), response.getReason(), response.getAgentId());
            }

            if (response.getMethods().isEmpty()) {
                AgentLog.warn("meta ok but methods empty commitHash=" + commitHash);
                stateHolder.markLogOff();
                methodMappingRegistry.clear();
                endpointRegistry.clear();
                interfaceMethodRegistry.clear();
                return BootstrapResult.failure(commitHash, response.getStatus(), "METHODS_EMPTY", response.getAgentId());
            }

            methodMappingRegistry.replaceAll(response.getMethods());
            endpointRegistry.replaceAll(response.getEndpoints());
            interfaceMethodRegistry.replaceRepositoryMethods(response.getRepositoryMethods());
            interfaceMethodRegistry.replaceDeclaredMethods(response.getMethods());
            if (response.getEndpoints().isEmpty()) {
                AgentLog.warn("meta ok but endpoints empty commitHash=" + commitHash);
            }
            if (response.getRepositoryMethods().isEmpty()) {
                AgentLog.warn("meta ok but repositoryMethods empty commitHash=" + commitHash);
            }
            return BootstrapResult.success(
                    commitHash,
                    response.getStatus(),
                    response.getAgentId(),
                    methodMappingRegistry.size(),
                    endpointRegistry.size(),
                    interfaceMethodRegistry.size(),
                    interfaceMethodRegistry.declaredMethodSize()
            );
        } catch (Exception e) {
            stateHolder.markLogOff();
            methodMappingRegistry.clear();
            endpointRegistry.clear();
            interfaceMethodRegistry.clear();
            return BootstrapResult.failure(commitHash, "LOG_OFF", e.getClass().getSimpleName() + ":" + e.getMessage(), null);
        }
    }
}
