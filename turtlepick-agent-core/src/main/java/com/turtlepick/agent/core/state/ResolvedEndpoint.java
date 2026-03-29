package com.turtlepick.agent.core.state;

import com.turtlepick.agent.core.http.EndpointInfo;

public final class ResolvedEndpoint {

    private final Integer endpointId;
    private final String entryType;
    private final String entryKey;
    private final String httpMethod;
    private final String resolutionStatus;

    private ResolvedEndpoint(
            Integer endpointId,
            String entryType,
            String entryKey,
            String httpMethod,
            String resolutionStatus) {
        this.endpointId = endpointId;
        this.entryType = entryType;
        this.entryKey = entryKey;
        this.httpMethod = httpMethod;
        this.resolutionStatus = resolutionStatus;
    }

    public static ResolvedEndpoint resolved(EndpointInfo endpointInfo) {
        return new ResolvedEndpoint(
                Integer.valueOf(endpointInfo.getEndpointId()),
                endpointInfo.getEntryType(),
                endpointInfo.getEntryKey(),
                endpointInfo.getHttpMethod(),
                "RESOLVED"
        );
    }

    public static ResolvedEndpoint unresolved(String resolutionStatus) {
        return new ResolvedEndpoint(null, null, null, null, resolutionStatus);
    }

    public Integer getEndpointId() {
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

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public boolean isResolved() {
        return endpointId != null;
    }
}
