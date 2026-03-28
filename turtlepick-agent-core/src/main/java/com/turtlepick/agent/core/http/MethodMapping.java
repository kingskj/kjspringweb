package com.turtlepick.agent.core.http;

public final class MethodMapping {

    private final int methodId;
    private final String fqcnMethod;

    public MethodMapping(int methodId, String fqcnMethod) {
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
    }

    public int getMethodId() {
        return methodId;
    }

    public String getFqcnMethod() {
        return fqcnMethod;
    }
}
