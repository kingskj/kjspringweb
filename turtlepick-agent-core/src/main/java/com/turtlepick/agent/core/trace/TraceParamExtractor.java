package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.instrument.MethodSignatureParser;
import com.turtlepick.agent.core.instrument.ParsedMethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class TraceParamExtractor {

    private static final int MAX_FIELDS = 12;
    private static final MethodSignatureParser SIGNATURE_PARSER = new MethodSignatureParser();

    private TraceParamExtractor() {
    }

    public static List<TraceParam> extract(String fqcnMethod, Object[] args, ErrorArgCaptureOptions options) {
        if (options == null || !options.isEnabled() || args == null || args.length == 0) {
            return null;
        }

        List<String> declaredTypes = parseParamTypes(fqcnMethod);
        List<TraceParam> params = new ArrayList<TraceParam>(args.length);
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String declaredType = i < declaredTypes.size() ? declaredTypes.get(i) : null;
            String type = declaredType != null ? declaredType : runtimeType(arg);
            String value = stringify(arg, options);
            List<TraceParamField> fields = extractFields(arg, options);
            params.add(new TraceParam("arg" + i, type, value, fields));
        }
        return params;
    }

    private static List<String> parseParamTypes(String fqcnMethod) {
        try {
            ParsedMethodSignature signature = SIGNATURE_PARSER.parse(fqcnMethod);
            return signature.getParamTypeNames();
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            return new ArrayList<String>(0);
        }
    }

    private static List<TraceParamField> extractFields(Object value, ErrorArgCaptureOptions options) {
        if (!shouldInspectFields(value, options)) {
            return null;
        }

        List<TraceParamField> fields = new ArrayList<TraceParamField>();
        Class<?> type = value.getClass();
        while (type != null && type != Object.class && fields.size() < MAX_FIELDS) {
            Field[] declaredFields;
            try {
                declaredFields = type.getDeclaredFields();
            } catch (Throwable t) {
                return fields;
            }
            for (int i = 0; i < declaredFields.length && fields.size() < MAX_FIELDS; i++) {
                Field field = declaredFields[i];
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
                    continue;
                }
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    Object fieldValue = field.get(value);
                    fields.add(new TraceParamField(field.getName(), runtimeType(fieldValue), stringify(fieldValue, options)));
                } catch (Throwable t) {
                    fields.add(new TraceParamField(field.getName(), field.getType().getName(), "<field access failed>"));
                }
            }
            type = type.getSuperclass();
        }
        return fields;
    }

    private static boolean shouldInspectFields(Object value, ErrorArgCaptureOptions options) {
        if (value == null) {
            return false;
        }
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isEnum() || type.isArray()) {
            return false;
        }
        String className = type.getName();
        if (isExcluded(className, options.getExcludeClassPatterns())) {
            return false;
        }
        return !(className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("sun.")
                || className.startsWith("org.springframework."));
    }

    private static String stringify(Object value, ErrorArgCaptureOptions options) {
        if (value == null) {
            return "null";
        }
        String className = value.getClass().getName();
        if (isExcluded(className, options.getExcludeClassPatterns())) {
            return "<excluded: " + className + ">";
        }
        try {
            String text = String.valueOf(value);
            int maxLength = options.getMaxLength();
            if (maxLength > 0 && text.length() > maxLength) {
                if (maxLength <= 3) {
                    return text.substring(0, maxLength);
                }
                return text.substring(0, maxLength - 3) + "...";
            }
            return text;
        } catch (Throwable t) {
            return "<toString failed: " + className + ">";
        }
    }

    private static boolean isExcluded(String className, String[] patterns) {
        if (className == null || patterns == null || patterns.length == 0) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            String pattern = trimToNull(patterns[i]);
            if (pattern == null) {
                continue;
            }
            if (pattern.endsWith(".**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (className.equals(prefix) || className.startsWith(prefix + ".")) {
                    return true;
                }
            } else if (className.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String runtimeType(Object value) {
        return value == null ? null : value.getClass().getName();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
