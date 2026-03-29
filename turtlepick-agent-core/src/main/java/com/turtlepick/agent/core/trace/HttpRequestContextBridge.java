package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Method;
import java.util.Locale;

public final class HttpRequestContextBridge {

    private HttpRequestContextBridge() {
    }

    public static void safeEnter(Object request) {
        try {
            doEnter(request);
        } catch (Throwable t) {
            if (isFatal(t)) {
                throw (Error) t;
            }
            HttpRequestContextHolder.clear();
            AgentLog.warn("http context enter skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
        }
    }

    public static void safeExit() {
        try {
            HttpRequestContextHolder.clear();
        } catch (Throwable t) {
            if (isFatal(t)) {
                throw (Error) t;
            }
            AgentLog.warn("http context clear skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
        }
    }

    private static void doEnter(Object request) throws Exception {
        if (request == null) {
            HttpRequestContextHolder.clear();
            return;
        }

        HttpRequestContext previous = HttpRequestContextHolder.get();
        if (previous != null) {
            AgentLog.warn("http context overwrite previousMethod=" + previous.getMethod()
                    + " previousUri=" + previous.getRequestUri());
        }

        String method = normalizeMethod(invokeString(request, "getMethod"));
        String requestUri = invokeString(request, "getRequestURI");
        String contextPath = invokeString(request, "getContextPath");
        String normalizedUri = normalizeRequestUri(requestUri, contextPath);

        HttpRequestContextHolder.set(new HttpRequestContext(method, normalizedUri));
    }

    private static String invokeString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        Object value = method.invoke(target);
        return value == null ? null : String.valueOf(value);
    }

    private static String normalizeMethod(String method) {
        if (method == null || method.trim().length() == 0) {
            return "";
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeRequestUri(String requestUri, String contextPath) {
        String normalized = requestUri == null ? "/" : requestUri.trim();
        if (normalized.length() == 0) {
            normalized = "/";
        }

        String normalizedContextPath = contextPath == null ? "" : contextPath.trim();
        if (normalizedContextPath.length() > 0
                && !"/".equals(normalizedContextPath)
                && normalized.startsWith(normalizedContextPath)) {
            normalized = normalized.substring(normalizedContextPath.length());
        }

        if (normalized.length() == 0) {
            normalized = "/";
        }
        if (normalized.charAt(0) != '/') {
            normalized = "/" + normalized;
        }
        while (normalized.indexOf("//") >= 0) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }

    private static boolean isFatal(Throwable throwable) {
        return throwable instanceof VirtualMachineError || throwable instanceof ThreadDeath;
    }
}
