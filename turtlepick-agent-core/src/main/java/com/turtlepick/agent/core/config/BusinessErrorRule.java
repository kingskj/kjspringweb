package com.turtlepick.agent.core.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BusinessErrorRule {

    private final String ruleId;
    private final String exceptionClassName;
    private final String codeAccessor;
    private final Set<String> excludeCodes;

    public BusinessErrorRule(
            String ruleId,
            String exceptionClassName,
            String codeAccessor,
            Set<String> excludeCodes
    ) {
        this.ruleId = trimToNull(ruleId);
        this.exceptionClassName = trimToNull(exceptionClassName);
        this.codeAccessor = trimToNull(codeAccessor);
        this.excludeCodes = immutableCopy(excludeCodes);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public String getCodeAccessor() {
        return codeAccessor;
    }

    public Set<String> getExcludeCodes() {
        return excludeCodes;
    }

    public boolean isValid() {
        return ruleId != null
                && exceptionClassName != null
                && codeAccessor != null
                && !excludeCodes.isEmpty();
    }

    private static Set<String> immutableCopy(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> copy = new LinkedHashSet<String>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                copy.add(normalized);
            }
        }
        if (copy.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
