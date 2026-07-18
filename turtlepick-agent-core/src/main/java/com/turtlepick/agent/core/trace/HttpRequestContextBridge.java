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
        safeExit(null, false);
    }

    public static void safeExit(Object response, boolean exceptionPropagated) {
        try {
            Integer status;
            if (!exceptionPropagated) {
                status = readStatus(response);
            } else if (isCommitted(response)) {
                // 전파돼도 응답이 이미 커밋됐으면 status는 최종값이라 신뢰한다 (C안).
                status = readStatus(response);
            } else {
                // 전파 + 미커밋: 컨테이너가 이후 status를 바꿀 수 있어 신뢰 불가 → fail-safe.
                status = null;
            }
            RuntimeMethodBridge.finishHttpRequest(status, exceptionPropagated);
        } catch (Throwable t) {
            if (isFatal(t)) {
                throw (Error) t;
            }
            RuntimeMethodBridge.finishHttpRequest(null, exceptionPropagated);
            AgentLog.warn("http context clear skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
        } finally {
            HttpRequestContextHolder.clear();
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

    private static Integer readStatus(Object response) {
        if (response == null) {
            return null;
        }
        try {
            Method method = response.getClass().getMethod("getStatus");
            Object value = method.invoke(response);
            if (value instanceof Number) {
                return Integer.valueOf(((Number) value).intValue());
            }
            if (value != null) {
                return Integer.valueOf(String.valueOf(value));
            }
        } catch (Throwable t) {
            if (isFatal(t)) {
                throw (Error) t;
            }
            AgentLog.warn("http status read skipped cause=" + t.getClass().getSimpleName() + ":" + safeMessage(t));
        }
        return null;
    }

    private static boolean isCommitted(Object response) {
        if (response == null) {
            return false;
        }
        try {
            Method method = response.getClass().getMethod("isCommitted");
            Object value = method.invoke(response);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable t) {
            if (isFatal(t)) {
                throw (Error) t;
            }
            return false;
        }
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
