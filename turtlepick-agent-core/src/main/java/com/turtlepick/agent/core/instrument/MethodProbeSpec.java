package com.turtlepick.agent.core.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MethodProbeSpec {

    private final int methodId;
    private final String fqcnMethod;
    private final String className;
    private final String methodName;
    private final List<String> paramTypeNames;

    public MethodProbeSpec(
            int methodId,
            String fqcnMethod,
            String className,
            String methodName,
            List<String> paramTypeNames) {
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.className = className;
        this.methodName = methodName;
        this.paramTypeNames = Collections.unmodifiableList(new ArrayList<String>(paramTypeNames));
    }

    public int getMethodId() {
        return methodId;
    }

    public String getFqcnMethod() {
        return fqcnMethod;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParamTypeNames() {
        return paramTypeNames;
    }
}
