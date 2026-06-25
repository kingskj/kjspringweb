package com.turtlepick.agent.core.instrument;

import com.turtlepick.agent.core.util.AgentLog;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MethodProbeIndex {

    private static final MethodProbeIndex EMPTY =
            new MethodProbeIndex(Collections.<String, List<MethodProbeSpec>>emptyMap());

    private final Map<String, List<MethodProbeSpec>> byClassName;

    public MethodProbeIndex(Map<String, List<MethodProbeSpec>> byClassName) {
        this.byClassName = Collections.unmodifiableMap(new HashMap<String, List<MethodProbeSpec>>(byClassName));
    }

    public static MethodProbeIndex empty() {
        return EMPTY;
    }

    public boolean containsClass(String className) {
        return byClassName.containsKey(className);
    }

    public Set<String> classNames() {
        return byClassName.keySet();
    }

    public MethodProbeSpec find(String className, String methodName, Type[] argumentTypes, Type returnType) {
        List<MethodProbeSpec> specs = byClassName.get(className);
        if (specs == null || specs.isEmpty()) {
            return null;
        }

        List<String> runtimeParamTypes = toCanonicalParamTypes(argumentTypes);
        String runtimeReturnType = toCanonicalTypeName(returnType);
        MethodProbeSpec matched = null;

        for (int i = 0; i < specs.size(); i++) {
            MethodProbeSpec spec = specs.get(i);
            if (!spec.getMethodName().equals(methodName)) {
                continue;
            }
            if (!sameParams(spec.getParamTypeNames(), runtimeParamTypes)) {
                continue;
            }
            if (!spec.getReturnTypeName().equals(runtimeReturnType)) {
                continue;
            }
            if (matched != null) {
                AgentLog.warn("ambiguous method probe skip"
                        + " className=" + className
                        + " methodName=" + methodName);
                return null;
            }
            matched = spec;
        }

        return matched;
    }

    private boolean sameParams(List<String> left, List<String> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).equals(right.get(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> toCanonicalParamTypes(Type[] argumentTypes) {
        List<String> result = new ArrayList<String>(argumentTypes.length);
        for (int i = 0; i < argumentTypes.length; i++) {
            result.add(toCanonicalTypeName(argumentTypes[i]));
        }
        return result;
    }

    private String toCanonicalTypeName(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return "boolean";
            case Type.BYTE:
                return "byte";
            case Type.CHAR:
                return "char";
            case Type.SHORT:
                return "short";
            case Type.INT:
                return "int";
            case Type.FLOAT:
                return "float";
            case Type.LONG:
                return "long";
            case Type.DOUBLE:
                return "double";
            case Type.VOID:
                return "void";
            case Type.ARRAY:
                return toCanonicalTypeName(type.getElementType()) + repeatArraySuffix(type.getDimensions());
            case Type.OBJECT:
                return type.getClassName();
            default:
                throw new IllegalArgumentException("unsupported type: " + type);
        }
    }

    private String repeatArraySuffix(int dimensions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < dimensions; i++) {
            builder.append("[]");
        }
        return builder.toString();
    }
}
