package com.turtlepick.agent.core.http;

import static com.turtlepick.agent.core.http.JsonCodecSupport.escapeJson;
import static com.turtlepick.agent.core.http.JsonCodecSupport.findMatchingBracket;
import static com.turtlepick.agent.core.http.JsonCodecSupport.readNullableStringField;
import static com.turtlepick.agent.core.http.JsonCodecSupport.skipWhitespace;

public final class LogReadyJsonCodec {

    public String encodeRequest(CompletedTraceFile request) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        builder.append("\"serverId\":\"").append(escapeJson(request.getServerId())).append("\",");
        builder.append("\"commitHash\":\"").append(escapeJson(request.getCommitHash())).append("\",");
        builder.append("\"fileName\":\"").append(escapeJson(request.getFileName())).append("\"");
        builder.append('}');
        return builder.toString();
    }

    public LogReadyResponse decodeResponse(String json) {
        if (json == null || json.trim().length() == 0) {
            return LogReadyResponse.failure(200, "EMPTY_RESPONSE");
        }

        try {
            int start = skipWhitespace(json, 0, json.length());
            if (start >= json.length() || json.charAt(start) != '{') {
                return LogReadyResponse.failure(200, "INVALID_RESPONSE:NOT_OBJECT");
            }

            int objectEnd = findMatchingBracket(json, start, json.length(), '{', '}');
            int end = objectEnd + 1;
            String resultCode = readNullableStringField(json, "resultCode", start, end);
            if (resultCode == null || resultCode.trim().length() == 0) {
                return LogReadyResponse.failure(200, "INVALID_RESPONSE:MISSING_RESULT_CODE");
            }
            return LogReadyResponse.ok(resultCode);
        } catch (RuntimeException e) {
            return LogReadyResponse.failure(200, "INVALID_RESPONSE:" + e.getClass().getSimpleName());
        }
    }
}
