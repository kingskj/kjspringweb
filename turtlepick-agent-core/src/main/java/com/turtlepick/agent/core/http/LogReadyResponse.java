package com.turtlepick.agent.core.http;

public final class LogReadyResponse {

    private final int httpStatus;
    private final String resultCode;
    private final String reason;

    private LogReadyResponse(int httpStatus, String resultCode, String reason) {
        this.httpStatus = httpStatus;
        this.resultCode = resultCode;
        this.reason = reason;
    }

    public static LogReadyResponse ok(String resultCode) {
        return new LogReadyResponse(200, resultCode, null);
    }

    public static LogReadyResponse failure(int httpStatus, String reason) {
        return new LogReadyResponse(httpStatus, null, reason);
    }

    public boolean isAccepted() {
        return "ACK".equals(resultCode) || "ALREADY_PROCESSED".equals(resultCode);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getReason() {
        return reason;
    }

    public String toLogDetail() {
        if (resultCode != null) {
            return "resultCode=" + resultCode;
        }
        if (reason != null) {
            return "reason=" + reason;
        }
        return "reason=UNKNOWN";
    }
}
