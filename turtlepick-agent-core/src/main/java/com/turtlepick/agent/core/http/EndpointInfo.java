package com.turtlepick.agent.core.http;

public final class EndpointInfo {

    private final int endpointId;
    private final String entryType;
    private final String entryKey;
    private final String httpMethod;
    private final int entryMethodId;

    public EndpointInfo(int endpointId, String entryType, String entryKey, String httpMethod, int entryMethodId) {
        this.endpointId = endpointId;
        this.entryType = entryType;
        this.entryKey = entryKey;
        this.httpMethod = httpMethod;
        this.entryMethodId = entryMethodId;
    }

    public int getEndpointId() {
        return endpointId;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getEntryKey() {
        return entryKey;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public int getEntryMethodId() {
        return entryMethodId;
    }

    public boolean isHttp() {
        return "HTTP".equalsIgnoreCase(entryType);
    }
}
