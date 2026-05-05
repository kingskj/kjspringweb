package com.turtlepick.agent.core.instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParsedMethodSignature {

    private final String fqcnMethod;
    private final String className;
    private final String methodName;
    private final List<String> paramTypeNames;
    private final String returnTypeName;

    public ParsedMethodSignature(
            String fqcnMethod,
            String className,
            String methodName,
            List<String> paramTypeNames,
            String returnTypeName) {
        this.fqcnMethod = fqcnMethod;
        this.className = className;
        this.methodName = methodName;
        this.paramTypeNames = Collections.unmodifiableList(new ArrayList<String>(paramTypeNames));
        this.returnTypeName = returnTypeName;
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

    public String getReturnTypeName() {
        return returnTypeName;
    }
}
