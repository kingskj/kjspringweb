package com.turtlepick.agent.core.state;

import com.turtlepick.agent.core.http.MethodMapping;
import com.turtlepick.agent.core.http.RepositoryMethodDto;
import com.turtlepick.agent.core.instrument.MethodSignatureParser;
import com.turtlepick.agent.core.instrument.ParsedMethodSignature;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InterfaceMethodRegistry {

    public static final class Match {

        private final int methodId;
        private final String fqcnMethod;

        Match(int methodId, String fqcnMethod) {
            this.methodId = methodId;
            this.fqcnMethod = fqcnMethod;
        }

        public int getMethodId() {
            return methodId;
        }

        public String getFqcnMethod() {
            return fqcnMethod;
        }
    }

    private final MethodSignatureParser methodSignatureParser = new MethodSignatureParser();

    private volatile Map<String, Match> repositoryMethods = Collections.emptyMap();
    private volatile Map<String, Match> declaredMethods = Collections.emptyMap();

    public void replaceRepositoryMethods(List<RepositoryMethodDto> repositoryMethods) {
        Map<String, Match> next = new HashMap<String, Match>();
        Set<String> ambiguousKeys = new HashSet<String>();
        if (repositoryMethods != null) {
            for (int i = 0; i < repositoryMethods.size(); i++) {
                RepositoryMethodDto dto = repositoryMethods.get(i);
                String key = buildKey(dto.getOwner(), dto.getMethodName(), dto.getRuntimeParams());
                if (ambiguousKeys.contains(key)) {
                    continue;
                }
                if (next.containsKey(key)) {
                    ambiguousKeys.add(key);
                    next.remove(key);
                    AgentLog.warn("interface method key collision key=" + key + " methodIds dropped");
                    continue;
                }
                next.put(key, new Match(dto.getMethodId(), dto.getFqcnMethod()));
            }
        }
        this.repositoryMethods = next;
    }

    public void replaceDeclaredMethods(List<MethodMapping> methods) {
        Map<String, Match> next = new HashMap<String, Match>();
        Set<String> ambiguousKeys = new HashSet<String>();
        if (methods != null) {
            for (int i = 0; i < methods.size(); i++) {
                MethodMapping method = methods.get(i);
                try {
                    ParsedMethodSignature parsed = methodSignatureParser.parse(method.getFqcnMethod());
                    String key = buildKey(parsed.getClassName(), parsed.getMethodName(), parsed.getParamTypeNames());
                    if (ambiguousKeys.contains(key)) {
                        continue;
                    }
                    if (next.containsKey(key)) {
                        ambiguousKeys.add(key);
                        next.remove(key);
                        AgentLog.warn("declared interface method key collision key=" + key + " methodIds dropped");
                        continue;
                    }
                    next.put(key, new Match(method.getMethodId(), method.getFqcnMethod()));
                } catch (RuntimeException e) {
                    AgentLog.warn("declared interface method parse skipped fqcnMethod="
                            + method.getFqcnMethod() + " reason=" + e.getMessage());
                }
            }
        }
        declaredMethods = next;
    }

    public void clear() {
        repositoryMethods = Collections.emptyMap();
        declaredMethods = Collections.emptyMap();
    }

    public int size() {
        return repositoryMethods.size();
    }

    public int declaredMethodSize() {
        return declaredMethods.size();
    }

    public boolean isEmpty() {
        return repositoryMethods.isEmpty();
    }

    public boolean isDeclaredEmpty() {
        return declaredMethods.isEmpty();
    }

    public Match lookup(Object proxy, Method method) {
        Map<String, Match> snapshot = repositoryMethods;
        if (snapshot.isEmpty() || proxy == null || method == null) {
            return null;
        }

        String suffix = buildSuffix(method);

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isInterface()) {
            Match direct = snapshot.get(declaringClass.getName() + suffix);
            if (direct != null) {
                return direct;
            }
        }

        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        Match found = null;
        int hitCount = 0;
        for (int i = 0; i < interfaces.length; i++) {
            Match candidate = snapshot.get(interfaces[i].getName() + suffix);
            if (candidate != null) {
                hitCount++;
                found = candidate;
            }
        }
        if (hitCount > 1) {
            AgentLog.warn("interface method lookup ambiguous suffix=" + suffix + " hits=" + hitCount);
            return null;
        }
        return hitCount == 1 ? found : null;
    }

    public Match lookupDeclared(Method method) {
        Map<String, Match> snapshot = declaredMethods;
        if (snapshot.isEmpty() || method == null) {
            return null;
        }

        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == null || Object.class.equals(declaringClass) || !declaringClass.isInterface()) {
            return null;
        }

        return snapshot.get(declaringClass.getName() + buildSuffix(method));
    }

    private static String buildSuffix(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        StringBuilder builder = new StringBuilder(48);
        builder.append('#').append(method.getName()).append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(toSourceTypeName(paramTypes[i]));
        }
        return builder.append(')').toString();
    }

    private static String buildKey(String owner, String methodName, List<String> runtimeParams) {
        StringBuilder builder = new StringBuilder(64);
        builder.append(owner).append('#').append(methodName).append('(');
        if (runtimeParams != null) {
            for (int i = 0; i < runtimeParams.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(runtimeParams.get(i));
            }
        }
        return builder.append(')').toString();
    }

    private static String toSourceTypeName(Class<?> type) {
        if (type.isArray()) {
            return toSourceTypeName(type.getComponentType()) + "[]";
        }
        return type.getName();
    }
}
