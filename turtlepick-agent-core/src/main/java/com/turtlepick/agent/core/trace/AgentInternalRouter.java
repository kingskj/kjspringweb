package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.state.AgentStateHolder;

import java.lang.reflect.Method;

final class AgentInternalRouter {

    private static final String RESUME_SUFFIX = "/agent/resume";
    private static volatile AgentStateHolder stateHolder;
    private static volatile String serverCommitHash;
    private static volatile LogReadyNotifier logReadyNotifier;

    private AgentInternalRouter() {
    }

    static boolean isInstalled() {
        return stateHolder != null && serverCommitHash != null && logReadyNotifier != null;
    }

    static void install(AgentStateHolder holder, String commitHash, LogReadyNotifier notifier) {
        serverCommitHash = commitHash;
        logReadyNotifier = notifier;
        stateHolder = holder;
    }

    static boolean isInternalRequest(Object request) throws Exception {
        String method = invokeString(request, "getMethod");
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = invokeString(request, "getRequestURI");
        String contextPath = invokeString(request, "getContextPath");
        String normalizedUri = normalizeUri(uri, contextPath);
        return RESUME_SUFFIX.equals(normalizedUri);
    }

    static void handle(Object request, Object response) throws Exception {
        new ResumeHandler(stateHolder, serverCommitHash, logReadyNotifier).handle(request, response);
    }

    static void writeFailure(Object response) {
        try {
            stateHolder.markLogOff();
            String json = "{\"state\":\"LOG_OFF\",\"reason\":\"RESUME_HANDLE_FAILED\""
                    + ",\"serverCommitHash\":\"" + serverCommitHash + "\"}";
            ResumeHandler.writeJson(response, json);
        } catch (Throwable ignored) {
        }
    }

    static String invokeString(Object target, String methodName) throws Exception {
        Method m = target.getClass().getMethod(methodName);
        Object v = m.invoke(target);
        return v == null ? null : String.valueOf(v);
    }

    private static String normalizeUri(String uri, String contextPath) {
        if (uri == null) return "/";
        String normalized = uri.trim();
        if (normalized.isEmpty()) return "/";

        String cp = (contextPath == null) ? "" : contextPath.trim();
        if (!cp.isEmpty() && !"/".equals(cp) && normalized.startsWith(cp)) {
            normalized = normalized.substring(cp.length());
        }
        if (normalized.isEmpty()) return "/";
        if (normalized.charAt(0) != '/') normalized = "/" + normalized;
        return normalized;
    }
}
