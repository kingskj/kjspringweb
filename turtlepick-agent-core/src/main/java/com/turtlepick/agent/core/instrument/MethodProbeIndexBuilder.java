package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MethodProbeIndexBuilder {

    private final MethodSignatureParser parser = new MethodSignatureParser();

    public MethodProbeIndex build(Map<String, Integer> methodMappings) {
        LinkedHashMap<String, List<MethodProbeSpec>> byClassName =
                new LinkedHashMap<String, List<MethodProbeSpec>>();

        for (Map.Entry<String, Integer> entry : methodMappings.entrySet()) {
            try {
                ParsedMethodSignature parsed = parser.parse(entry.getKey());

                MethodProbeSpec spec = new MethodProbeSpec(
                        entry.getValue().intValue(),
                        parsed.getFqcnMethod(),
                        parsed.getClassName(),
                        parsed.getMethodName(),
                        parsed.getParamTypeNames()
                );

                List<MethodProbeSpec> specs = byClassName.get(parsed.getClassName());
                if (specs == null) {
                    specs = new ArrayList<MethodProbeSpec>();
                    byClassName.put(parsed.getClassName(), specs);
                }
                specs.add(spec);
            } catch (RuntimeException e) {
                AgentLog.warn("method probe parse skipped fqcnMethod=" + entry.getKey() + " reason=" + e.getMessage());
            }
        }

        return new MethodProbeIndex(byClassName);
    }
}
