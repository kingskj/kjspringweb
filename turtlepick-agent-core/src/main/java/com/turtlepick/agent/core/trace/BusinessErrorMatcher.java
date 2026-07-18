package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.config.BusinessErrorConfig;
import com.turtlepick.agent.core.config.BusinessErrorRule;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BusinessErrorMatcher {

    private final BusinessErrorConfig config;
    private final Set<String> warnedFailures = ConcurrentHashMap.newKeySet();

    public BusinessErrorMatcher(BusinessErrorConfig config) {
        this.config = config == null ? BusinessErrorConfig.disabled() : config;
    }

    public boolean matches(Throwable throwable) {
        if (throwable == null || !config.hasRules()) {
            return false;
        }
        List<BusinessErrorRule> rules = config.getRules();
        for (int i = 0; i < rules.size(); i++) {
            BusinessErrorRule rule = rules.get(i);
            if (rule == null || !rule.isValid()) {
                continue;
            }
            if (!isInstanceOf(throwable.getClass(), rule.getExceptionClassName())) {
                continue;
            }
            String code = extractCode(rule, throwable);
            if (code != null && rule.getExcludeCodes().contains(code)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInstanceOf(Class<?> actualClass, String expectedClassName) {
        if (actualClass == null || expectedClassName == null) {
            return false;
        }
        Set<Class<?>> visited = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
        return classMatches(actualClass, expectedClassName, visited);
    }

    private boolean classMatches(Class<?> actualClass, String expectedClassName, Set<Class<?>> visited) {
        if (actualClass == null || !visited.add(actualClass)) {
            return false;
        }
        if (expectedClassName.equals(actualClass.getName())) {
            return true;
        }
        Class<?>[] interfaces = actualClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (classMatches(interfaces[i], expectedClassName, visited)) {
                return true;
            }
        }
        return classMatches(actualClass.getSuperclass(), expectedClassName, visited);
    }

    private String extractCode(BusinessErrorRule rule, Throwable throwable) {
        try {
            Method method = throwable.getClass().getMethod(rule.getCodeAccessor());
            Object value = method.invoke(throwable);
            if (value == null) {
                warnOnce(rule, throwable, "null_code");
                return null;
            }
            if (value instanceof Enum<?>) {
                return ((Enum<?>) value).name();
            }
            return String.valueOf(value);
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            warnOnce(rule, throwable, t.getClass().getSimpleName());
            return null;
        }
    }

    private void warnOnce(BusinessErrorRule rule, Throwable throwable, String reason) {
        String ruleId = rule == null ? "<null>" : rule.getRuleId();
        String actualClass = throwable == null ? "<null>" : throwable.getClass().getName();
        String key = ruleId + "|" + actualClass + "|" + reason;
        if (warnedFailures.add(key)) {
            AgentLog.warn("business-error rule match skipped"
                    + " ruleId=" + ruleId
                    + " actualClass=" + actualClass
                    + " cause=" + reason);
        }
    }
}
