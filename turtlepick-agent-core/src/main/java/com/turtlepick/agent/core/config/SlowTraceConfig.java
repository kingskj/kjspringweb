package com.turtlepick.agent.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SlowTraceConfig {

    private static final SlowTraceConfig DISABLED =
            new SlowTraceConfig(false, 0, Collections.<SlowExcludeRule>emptyList());

    private final boolean enabled;
    private final int thresholdMs;
    private final List<SlowExcludeRule> excludes;

    public SlowTraceConfig(boolean enabled, int thresholdMs, List<SlowExcludeRule> excludes) {
        this.enabled = enabled;
        this.thresholdMs = thresholdMs;
        this.excludes = immutableRuleCopy(excludes);
    }

    public static SlowTraceConfig disabled() {
        return DISABLED;
    }

    public boolean isEnabled() {
        return enabled && thresholdMs > 0;
    }

    public int getThresholdMs() {
        return thresholdMs;
    }

    public List<SlowExcludeRule> getExcludes() {
        return excludes;
    }

    public boolean isExcluded(Integer endpointId, String entryFqcnMethod) {
        if (!isEnabled() || excludes.isEmpty()) {
            return false;
        }
        for (int i = 0; i < excludes.size(); i++) {
            SlowExcludeRule rule = excludes.get(i);
            if (rule.matches(endpointId, entryFqcnMethod)) {
                return true;
            }
        }
        return false;
    }

    public String describeRuleIds() {
        if (excludes.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < excludes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(excludes.get(i).getRuleId());
        }
        builder.append(']');
        return builder.toString();
    }

    private static List<SlowExcludeRule> immutableRuleCopy(List<SlowExcludeRule> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<SlowExcludeRule> copy = new ArrayList<SlowExcludeRule>();
        for (SlowExcludeRule value : values) {
            if (value != null && value.isValid()) {
                copy.add(value);
            }
        }
        if (copy.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(copy);
    }
}
