package com.turtlepick.agent.core.trace;

final class BusinessErrorCandidate {

    private final int callId;
    private final String fqcnMethod;
    private final ErrorMeta errorMeta;

    BusinessErrorCandidate(int callId, String fqcnMethod, ErrorMeta errorMeta) {
        this.callId = callId;
        this.fqcnMethod = fqcnMethod;
        this.errorMeta = errorMeta;
    }

    int getCallId() {
        return callId;
    }

    String getFqcnMethod() {
        return fqcnMethod;
    }

    ErrorMeta getErrorMeta() {
        return errorMeta;
    }
}
