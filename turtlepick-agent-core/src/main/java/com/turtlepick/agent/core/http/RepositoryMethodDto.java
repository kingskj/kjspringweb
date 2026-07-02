package com.turtlepick.agent.core.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RepositoryMethodDto {

    private final int methodId;
    private final String owner;
    private final String methodName;
    private final List<String> params;
    private final List<String> runtimeParams;
    private final String returnType;
    private final String fqcnMethod;

    public RepositoryMethodDto(
            int methodId,
            String owner,
            String methodName,
            List<String> params,
            List<String> runtimeParams,
            String returnType,
            String fqcnMethod) {
        this.methodId = methodId;
        this.owner = owner;
        this.methodName = methodName;
        this.params = unmodifiableCopy(params);
        this.runtimeParams = unmodifiableCopy(runtimeParams);
        this.returnType = returnType;
        this.fqcnMethod = fqcnMethod;
    }

    private static List<String> unmodifiableCopy(List<String> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(source));
    }

    public int getMethodId() {
        return methodId;
    }

    public String getOwner() {
        return owner;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParams() {
        return params;
    }

    public List<String> getRuntimeParams() {
        return runtimeParams;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getFqcnMethod() {
        return fqcnMethod;
    }
}
