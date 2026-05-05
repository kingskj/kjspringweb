package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.AgentStateHolder;
import com.turtlepick.agent.core.util.AgentLog;

import java.io.InputStream;
import java.lang.reflect.Method;

final class ResumeHandler {

    private final AgentStateHolder stateHolder;
    private final String serverCommitHash;
    private final LogReadyNotifier logReadyNotifier;

    ResumeHandler(AgentStateHolder stateHolder, String serverCommitHash, LogReadyNotifier notifier) {
        this.stateHolder = stateHolder;
        this.serverCommitHash = serverCommitHash;
        this.logReadyNotifier = notifier;
    }

    void handle(Object request, Object response) throws Exception {
        String body = readBody(request);
        String command = extractJsonString(body, "command");
        String requestedHash = extractJsonString(body, "commitHash");

        if (!"RESUME_LOGGING".equals(command)) {
            stateHolder.markLogOff();
            AgentLog.warn("resume rejected reason=INVALID_COMMAND command=" + command);
            writeJson(response, buildResponse("LOG_OFF", "INVALID_COMMAND"));
            return;
        }

        if (requestedHash != null && !requestedHash.isEmpty()
                && !requestedHash.equals(serverCommitHash)) {
            stateHolder.markLogOff();
            AgentLog.warn("resume rejected reason=COMMIT_MISMATCH requested=" + requestedHash);
            writeJson(response, buildResponse("LOG_OFF", "COMMIT_MISMATCH"));
            return;
        }

        stateHolder.markLogOn();
        logReadyNotifier.start();
        AgentLog.info("agent state LOG_ON reason=RESUME");
        writeJson(response, "{\"state\":\"LOG_ON\",\"serverCommitHash\":\"" + serverCommitHash + "\"}");
    }

    private String buildResponse(String state, String reason) {
        return "{\"state\":\"" + state + "\""
                + ",\"reason\":\"" + reason + "\""
                + ",\"serverCommitHash\":\"" + serverCommitHash + "\"}";
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
}
