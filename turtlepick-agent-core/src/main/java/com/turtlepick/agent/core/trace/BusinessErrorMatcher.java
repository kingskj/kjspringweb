package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.config.BusinessErrorConfig;
import com.turtlepick.agent.core.config.BusinessErrorRule;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BusinessErrorMatcher {

    private final BusinessErrorConfig config;
    private final Set<String> warnedFailures = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<AccessorCacheKey, ResolvedAccessor> accessorCache =
            new ConcurrentHashMap<AccessorCacheKey, ResolvedAccessor>();

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
        ResolvedAccessor accessor = resolveAccessor(rule, throwable.getClass());
        if (accessor.isMiss()) {
            warnOnce(rule, throwable, "accessor_not_found");
            return null;
        }

        try {
            Object value = accessor.getValue(throwable);
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

    private ResolvedAccessor resolveAccessor(BusinessErrorRule rule, Class<?> actualClass) {
        AccessorCacheKey key = new AccessorCacheKey(rule.getRuleId(), actualClass.getName());
        ResolvedAccessor cached = accessorCache.get(key);
        if (cached != null) {
            return cached;
        }

        ResolvedAccessor resolved = lookupAccessor(rule, actualClass);
        ResolvedAccessor previous = accessorCache.putIfAbsent(key, resolved);
        if (previous != null) {
            return previous;
        }
        if (!resolved.isMiss()) {
            AgentLog.info("business-error accessor resolved"
                    + " ruleId=" + rule.getRuleId()
                    + " actualClass=" + actualClass.getName()
                    + " kind=" + resolved.getKindLogValue()
                    + " accessor=" + rule.getCodeAccessor());
        }
        return resolved;
    }

    private ResolvedAccessor lookupAccessor(BusinessErrorRule rule, Class<?> actualClass) {
        String accessorName = rule.getCodeAccessor();
        Method publicMethod = findPublicMethod(actualClass, accessorName);
        if (publicMethod != null) {
            return ResolvedAccessor.method(publicMethod);
        }

        Method declaredMethod = findDeclaredMethod(actualClass, accessorName);
        if (declaredMethod != null) {
            return ResolvedAccessor.method(declaredMethod);
        }

        Field field = findDeclaredField(actualClass, accessorName);
        if (field != null) {
            return ResolvedAccessor.field(field);
        }

        return ResolvedAccessor.miss();
    }

    private Method findPublicMethod(Class<?> actualClass, String accessorName) {
        try {
            Method method = actualClass.getMethod(accessorName);
            if (method.getParameterTypes().length != 0) {
                return null;
            }
            return makeAccessible(method) ? method : null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }

    private Method findDeclaredMethod(Class<?> actualClass, String accessorName) {
        Class<?> cursor = actualClass;
        while (cursor != null) {
            try {
                Method method = cursor.getDeclaredMethod(accessorName);
                if (method.getParameterTypes().length == 0 && makeAccessible(method)) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
                // Try superclass.
            } catch (SecurityException e) {
                // Try superclass.
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private Field findDeclaredField(Class<?> actualClass, String accessorName) {
        Class<?> cursor = actualClass;
        while (cursor != null) {
            try {
                Field field = cursor.getDeclaredField(accessorName);
                if (makeAccessible(field)) {
                    return field;
                }
            } catch (NoSuchFieldException e) {
                // Try superclass.
            } catch (SecurityException e) {
                // Try superclass.
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    private boolean makeAccessible(java.lang.reflect.AccessibleObject accessibleObject) {
        try {
            accessibleObject.setAccessible(true);
            return true;
        } catch (SecurityException e) {
            return false;
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

    private static final class AccessorCacheKey {

        private final String ruleId;
        private final String actualClassName;

        private AccessorCacheKey(String ruleId, String actualClassName) {
            this.ruleId = ruleId;
            this.actualClassName = actualClassName;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof AccessorCacheKey)) {
                return false;
            }
            AccessorCacheKey that = (AccessorCacheKey) other;
            return equalsNullable(ruleId, that.ruleId)
                    && equalsNullable(actualClassName, that.actualClassName);
        }

        @Override
        public int hashCode() {
            int result = ruleId == null ? 0 : ruleId.hashCode();
            result = 31 * result + (actualClassName == null ? 0 : actualClassName.hashCode());
            return result;
        }

        private static boolean equalsNullable(String left, String right) {
            return left == null ? right == null : left.equals(right);
        }
    }

    private static final class ResolvedAccessor {

        private enum Kind {
            METHOD,
            FIELD,
            MISS
        }

        private static final ResolvedAccessor MISS = new ResolvedAccessor(Kind.MISS, null, null);

        private final Kind kind;
        private final Method method;
        private final Field field;

        private ResolvedAccessor(Kind kind, Method method, Field field) {
            this.kind = kind;
            this.method = method;
            this.field = field;
        }

        private static ResolvedAccessor method(Method method) {
            return new ResolvedAccessor(Kind.METHOD, method, null);
        }

        private static ResolvedAccessor field(Field field) {
            return new ResolvedAccessor(Kind.FIELD, null, field);
        }

        private static ResolvedAccessor miss() {
            return MISS;
        }

        private boolean isMiss() {
            return kind == Kind.MISS;
        }

        private String getKindLogValue() {
            return kind == Kind.METHOD ? "method" : "field";
        }

        private Object getValue(Throwable throwable) throws Exception {
            if (kind == Kind.METHOD) {
                return method.invoke(throwable);
            }
            if (kind == Kind.FIELD) {
                return field.get(throwable);
            }
            return null;
        }
    }
}
