package com.turtlepick.agent.core.http;

public final class MetaRequest {

    private final String serverId;
    private final String appName;
    private final String commitHash;

    public MetaRequest(String serverId, String appName, String commitHash) {
        this.serverId = serverId;
        this.appName = appName;
        this.commitHash = commitHash;
    }

    public String getServerId() {
        return serverId;
    }

    public String getAppName() {
        return appName;
    }

    public String getCommitHash() {
        return commitHash;
    }
}
