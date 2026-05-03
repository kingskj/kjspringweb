package com.turtlepick.agent.core.trace;

public final class ErrorArgExtractor {

    private ErrorArgExtractor() {
    }

    public static String[] extract(Object[] args, ErrorArgCaptureOptions options) {
        if (options == null || !options.isEnabled() || args == null || args.length == 0) {
            return new String[0];
        }

        String[] result = new String[args.length];
        String[] patterns = options.getExcludeClassPatterns();
        int maxLength = options.getMaxLength();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                result[i] = "null";
                continue;
            }

            String className = arg.getClass().getName();
            if (isExcluded(className, patterns)) {
                result[i] = "<excluded: " + className + ">";
                continue;
            }
            result[i] = safeToString(arg, maxLength);
        }
        return result;
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

    private static String safeToString(Object arg, int maxLength) {
        String value;
        try {
            value = arg.toString();
        } catch (Throwable t) {
            return "<toString failed: " + arg.getClass().getName() + ">";
        }
        if (value == null) {
            return "null";
        }
        if (maxLength > 0 && value.length() > maxLength) {
            if (maxLength <= 3) {
                return value.substring(0, maxLength);
            }
            return value.substring(0, maxLength - 3) + "...";
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
