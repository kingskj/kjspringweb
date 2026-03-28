package com.turtlepick.agent.core.state;

import com.turtlepick.agent.core.http.MethodMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class MethodMappingRegistry {

    private final AtomicReference<Map<String, Integer>> mappings =
            new AtomicReference<Map<String, Integer>>(Collections.<String, Integer>emptyMap());

    public void replaceAll(List<MethodMapping> methodMappings) {
        LinkedHashMap<String, Integer> next = new LinkedHashMap<String, Integer>();

        if (methodMappings != null) {
            for (MethodMapping mapping : methodMappings) {
                if (mapping == null) {
                    throw new IllegalArgumentException("method mapping must not be null");
                }

                String fqcnMethod = trimToNull(mapping.getFqcnMethod());
                if (fqcnMethod == null) {
                    throw new IllegalArgumentException("fqcnMethod must not be blank");
                }
                if (mapping.getMethodId() <= 0) {
                    throw new IllegalArgumentException("methodId must be positive: " + mapping.getMethodId());
                }

                Integer previous = next.put(fqcnMethod, Integer.valueOf(mapping.getMethodId()));
                if (previous != null) {
                    throw new IllegalArgumentException("duplicate fqcnMethod detected: " + fqcnMethod);
                }
            }
        }

        mappings.set(Collections.unmodifiableMap(next));
    }

    public void clear() {
        mappings.set(Collections.<String, Integer>emptyMap());
    }

    public Integer findMethodId(String fqcnMethod) {
        return mappings.get().get(fqcnMethod);
    }

    public Map<String, Integer> snapshot() {
        return mappings.get();
    }

    public int size() {
        return mappings.get().size();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
