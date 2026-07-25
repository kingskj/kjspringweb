package com.turtlepick.agent.core.config;

public final class SlowExcludeRule {

    private final String ruleId;
    private final String endpointId;
    private final String method;

    public SlowExcludeRule(String ruleId, String endpointId, String method) {
        this.ruleId = trimToNull(ruleId);
        this.endpointId = trimToNull(endpointId);
        this.method = trimToNull(method);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getMethod() {
        return method;
    }

    public boolean isValid() {
        return ruleId != null && exactlyOneSelector();
    }

    public boolean matches(Integer actualEndpointId, String actualMethod) {
        if (!isValid()) {
            return false;
        }
        if (endpointId != null) {
            return actualEndpointId != null && endpointId.equals(String.valueOf(actualEndpointId));
        }
        return method != null && method.equals(actualMethod);
    }

    private boolean exactlyOneSelector() {
        return (endpointId != null && method == null) || (endpointId == null && method != null);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
