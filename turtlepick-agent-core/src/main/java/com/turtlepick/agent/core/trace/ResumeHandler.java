package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

import java.io.InputStream;
import java.lang.reflect.Method;

final class ResumeHandler {

    private final AgentRuntimeController runtimeController;

    ResumeHandler(AgentRuntimeController runtimeController) {
        this.runtimeController = runtimeController;
    }

    void handle(Object request, Object response) throws Exception {
        String body = readBody(request);
        String command = extractJsonString(body, "command");
        String requestedHash = extractJsonString(body, "commitHash");

        if (!"RESUME_LOGGING".equals(command) && !"RELOAD_META".equals(command)) {
            runtimeController.markLogOff();
            AgentLog.warn("resume rejected reason=INVALID_COMMAND command=" + command);
            writeJson(response, buildResponse(AgentRuntimeController.ActivationResult.failure(
                    runtimeController.getServerCommitHash(), "LOG_OFF", "INVALID_COMMAND", null, command)));
            return;
        }

        AgentRuntimeController.ActivationResult result =
                runtimeController.reloadMetaAndActivate(command, requestedHash);
        writeJson(response, buildResponse(result));
    }

    private String buildResponse(AgentRuntimeController.ActivationResult result) {
        AgentRuntimeController.RetransformSummary summary = result.getRetransformSummary();
        StringBuilder builder = new StringBuilder();
        builder.append("{\"state\":\"").append(result.isSuccess() ? "LOG_ON" : "LOG_OFF").append("\"");
        appendJsonField(builder, "reason", result.getReason());
        appendJsonField(builder, "status", result.getStatus());
        appendJsonField(builder, "trigger", result.getTrigger());
        appendJsonField(builder, "serverCommitHash", result.getServerCommitHash());
        appendJsonField(builder, "agentId", result.getAgentId());
        builder.append(",\"methodCount\":").append(result.getMethodCount());
        builder.append(",\"endpointCount\":").append(result.getEndpointCount());
        builder.append(",\"retransformTransformed\":").append(summary.getTransformed());
        builder.append(",\"retransformSkipped\":").append(summary.getSkipped());
        builder.append(",\"retransformFailed\":").append(summary.getFailed());
        builder.append("}");
        return builder.toString();
    }

    static void writeJson(Object response, String json) throws Exception {
        Method setStatus = response.getClass().getMethod("setStatus", int.class);
        setStatus.invoke(response, 200);

        Method setContentType = response.getClass().getMethod("setContentType", String.class);
        setContentType.invoke(response, "application/json;charset=UTF-8");

        Method getWriter = response.getClass().getMethod("getWriter");
        Object writer = getWriter.invoke(response);

        Method write = writer.getClass().getMethod("write", String.class);
        write.invoke(writer, json);

        Method flush = writer.getClass().getMethod("flush");
        flush.invoke(writer);
    }

    private String readBody(Object request) throws Exception {
        Method getInputStream = request.getClass().getMethod("getInputStream");
        InputStream is = (InputStream) getInputStream.invoke(request);
        byte[] buf = new byte[4096];
        int total = 0;
        int read;
        while ((read = is.read(buf, total, buf.length - total)) != -1) {
            total += read;
            if (total == buf.length) {
                AgentLog.warn("resume body truncated maxBytes=4096");
                break;
            }
        }
        return new String(buf, 0, total, "UTF-8");
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int ki = json.indexOf(search);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + search.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private void appendJsonField(StringBuilder builder, String key, String value) {
        builder.append(",\"").append(key).append("\":");
        if (value == null) {
            builder.append("null");
            return;
        }
        builder.append("\"").append(escapeJson(value)).append("\"");
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        String hex = Integer.toHexString(ch);
                        builder.append("\\u");
                        for (int j = hex.length(); j < 4; j++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        return builder.toString();
    }
}
