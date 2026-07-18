package com.turtlepick.agent.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BusinessErrorConfig {

    private static final BusinessErrorConfig DISABLED =
            new BusinessErrorConfig(Collections.<Integer>emptySet(), Collections.<BusinessErrorRule>emptyList());

    private final Set<Integer> excludeHttpStatuses;
    private final List<BusinessErrorRule> rules;

    public BusinessErrorConfig(Set<Integer> excludeHttpStatuses, List<BusinessErrorRule> rules) {
        this.excludeHttpStatuses = immutableStatusCopy(excludeHttpStatuses);
        this.rules = immutableRuleCopy(rules);
    }

    public static BusinessErrorConfig disabled() {
        return DISABLED;
    }

    public Set<Integer> getExcludeHttpStatuses() {
        return excludeHttpStatuses;
    }

    public List<BusinessErrorRule> getRules() {
        return rules;
    }

    public boolean isHttpStatusExcluded(Integer status) {
        return status != null && excludeHttpStatuses.contains(status);
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }

    public boolean isEmpty() {
        return excludeHttpStatuses.isEmpty() && rules.isEmpty();
    }

    public String describeRuleIds() {
        if (rules.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(rules.get(i).getRuleId());
        }
        builder.append(']');
        return builder.toString();
    }

    private static Set<Integer> immutableStatusCopy(Set<Integer> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<Integer>(values));
    }

    private static List<BusinessErrorRule> immutableRuleCopy(List<BusinessErrorRule> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<BusinessErrorRule> copy = new ArrayList<BusinessErrorRule>();
        for (BusinessErrorRule value : values) {
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
