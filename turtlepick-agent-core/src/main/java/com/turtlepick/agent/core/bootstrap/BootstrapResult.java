package com.turtlepick.agent.core.bootstrap;

public final class BootstrapResult {

    private final boolean success;
    private final String commitHash;
    private final String status;
    private final String reason;
    private final String agentId;
    private final int methodCount;
    private final int endpointCount;
    private final int interfaceMethodCount;
    private final int declaredMethodCount;

    public BootstrapResult(
            boolean success,
            String commitHash,
            String status,
            String reason,
            String agentId,
            int methodCount,
            int endpointCount,
            int interfaceMethodCount,
            int declaredMethodCount) {
        this.success = success;
        this.commitHash = commitHash;
        this.status = status;
        this.reason = reason;
        this.agentId = agentId;
        this.methodCount = methodCount;
        this.endpointCount = endpointCount;
        this.interfaceMethodCount = interfaceMethodCount;
        this.declaredMethodCount = declaredMethodCount;
    }

    public static BootstrapResult success(
            String commitHash,
            String status,
            String agentId,
            int methodCount,
            int endpointCount,
            int interfaceMethodCount,
            int declaredMethodCount) {
        return new BootstrapResult(true, commitHash, status, null, agentId,
                methodCount, endpointCount, interfaceMethodCount, declaredMethodCount);
    }

    public static BootstrapResult failure(String commitHash, String status, String reason, String agentId) {
        return new BootstrapResult(false, commitHash, status, reason, agentId, 0, 0, 0, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCommitHash() {
        return commitHash;
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
}
