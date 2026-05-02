package com.turtlepick.agent.core.http;

public final class CompletedTraceFile {

    private final String serverId;
    private final String commitHash;
    private final String fileName;

    public CompletedTraceFile(String serverId, String commitHash, String fileName) {
        this.serverId = serverId;
        this.commitHash = commitHash;
        this.fileName = fileName;
    }

    public String getServerId() {
        return serverId;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getFileName() {
        return fileName;
    }
}
