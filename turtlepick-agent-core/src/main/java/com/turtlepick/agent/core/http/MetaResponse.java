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

    public MetaResponse(String status, String reason, String agentId, String commitHash, List<MethodMapping> methods) {
        this.status = status;
        this.reason = reason;
        this.agentId = agentId;
        this.commitHash = commitHash;
        if (methods == null) {
            this.methods = Collections.emptyList();
        } else {
            this.methods = Collections.unmodifiableList(new ArrayList<MethodMapping>(methods));
        }
    }

    public static MetaResponse logOff(String reason) {
        return new MetaResponse("LOG_OFF", reason, null, null, Collections.<MethodMapping>emptyList());
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
}
