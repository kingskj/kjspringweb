package com.turtlepick.agent.core.state;

import com.turtlepick.agent.core.http.EndpointInfo;
import com.turtlepick.agent.core.trace.HttpRequestContext;
import com.turtlepick.agent.core.util.AgentLog;

import java.util.ArrayList;
import java.util.List;

public final class EndpointResolver {

    private final EndpointRegistry endpointRegistry;

    public EndpointResolver(EndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    public ResolvedEndpoint resolve(int entryMethodId, HttpRequestContext httpRequestContext) {
        List<EndpointInfo> candidates = endpointRegistry.findByEntryMethodId(entryMethodId);
        if (candidates.isEmpty()) {
            AgentLog.warn("endpoint unresolved status=NO_CANDIDATE entryMethodId=" + entryMethodId);
            return ResolvedEndpoint.unresolved("NO_CANDIDATE");
        }

        if (candidates.size() == 1) {
            return ResolvedEndpoint.resolved(candidates.get(0));
        }

        if (httpRequestContext == null) {
            AgentLog.warn("endpoint unresolved status=HTTP_CONTEXT_MISSING entryMethodId=" + entryMethodId);
            return ResolvedEndpoint.unresolved("HTTP_CONTEXT_MISSING");
        }

        List<EndpointInfo> matched = new ArrayList<EndpointInfo>();
        for (EndpointInfo candidate : candidates) {
            if (!candidate.isHttp()) {
                continue;
            }
            if (!equalsIgnoreCase(trimToEmpty(candidate.getHttpMethod()), httpRequestContext.getMethod())) {
                continue;
            }
            if (!matchesPath(candidate.getEntryKey(), httpRequestContext.getRequestUri())) {
                continue;
            }
            matched.add(candidate);
        }

        if (matched.size() == 1) {
            return ResolvedEndpoint.resolved(matched.get(0));
        }
        if (matched.isEmpty()) {
            AgentLog.warn("endpoint unresolved status=NO_HTTP_MATCH entryMethodId=" + entryMethodId
                    + " requestMethod=" + httpRequestContext.getMethod()
                    + " requestUri=" + httpRequestContext.getRequestUri());
            return ResolvedEndpoint.unresolved("NO_HTTP_MATCH");
        }

        AgentLog.warn("endpoint unresolved status=AMBIGUOUS entryMethodId=" + entryMethodId
                + " requestMethod=" + httpRequestContext.getMethod()
                + " requestUri=" + httpRequestContext.getRequestUri()
                + " candidateCount=" + matched.size());
        return ResolvedEndpoint.unresolved("AMBIGUOUS");
    }

    boolean matchesPath(String entryKey, String requestUri) {
        String normalizedPattern = normalizePath(entryKey);
        String normalizedRequestUri = normalizePath(requestUri);

        if ("/".equals(normalizedPattern) && "/".equals(normalizedRequestUri)) {
            return true;
        }

        String[] patternSegments = splitSegments(normalizedPattern);
        String[] requestSegments = splitSegments(normalizedRequestUri);
        if (patternSegments.length != requestSegments.length) {
            return false;
        }

        for (int i = 0; i < patternSegments.length; i++) {
            String patternSegment = patternSegments[i];
            if (isPathVariable(patternSegment)) {
                continue;
            }
            if (!patternSegment.equals(requestSegments[i])) {
                return false;
            }
        }
        return true;
    }

    private String[] splitSegments(String path) {
        if (path == null || "/".equals(path)) {
            return new String[0];
        }
        return path.substring(1).split("/");
    }

    private boolean isPathVariable(String segment) {
        return segment != null
                && segment.length() >= 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}';
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }

        String normalized = path.trim();
        if (normalized.length() == 0) {
            return "/";
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

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }
}
