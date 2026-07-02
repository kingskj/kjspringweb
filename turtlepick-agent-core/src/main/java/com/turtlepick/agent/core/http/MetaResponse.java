package com.turtlepick.agent.core.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MetaResponse {

    private final String status;
    private final String reason;
    private final String agentId;
    private final String commitHash;
    private final List<MethodMapping> methods;
    private final List<EndpointInfo> endpoints;
    private final List<RepositoryMethodDto> repositoryMethods;

    public MetaResponse(
            String status,
            String reason,
            String agentId,
            String commitHash,
            List<MethodMapping> methods,
            List<EndpointInfo> endpoints,
            List<RepositoryMethodDto> repositoryMethods) {
        this.status = status;
        this.reason = reason;
        this.agentId = agentId;
        this.commitHash = commitHash;
        if (methods == null) {
            this.methods = Collections.emptyList();
        } else {
            this.methods = Collections.unmodifiableList(new ArrayList<MethodMapping>(methods));
        }
        if (endpoints == null) {
            this.endpoints = Collections.emptyList();
        } else {
            this.endpoints = Collections.unmodifiableList(new ArrayList<EndpointInfo>(endpoints));
        }
        if (repositoryMethods == null) {
            this.repositoryMethods = Collections.emptyList();
        } else {
            this.repositoryMethods = Collections.unmodifiableList(
                    new ArrayList<RepositoryMethodDto>(repositoryMethods));
        }
    }

    public static MetaResponse logOff(String reason) {
        return new MetaResponse(
                "LOG_OFF",
                reason,
                null,
                null,
                Collections.<MethodMapping>emptyList(),
                Collections.<EndpointInfo>emptyList(),
                Collections.<RepositoryMethodDto>emptyList()
        );
    }

    public boolean isOk() {
        return "OK".equalsIgnoreCase(status);
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

    public String getCommitHash() {
        return commitHash;
    }

    public List<MethodMapping> getMethods() {
        return methods;
    }

    public List<EndpointInfo> getEndpoints() {
        return endpoints;
    }

    public List<RepositoryMethodDto> getRepositoryMethods() {
        return repositoryMethods;
    }
}
