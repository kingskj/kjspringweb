package com.turtlepick.agent.core.trace;

public final class ErrorArgCaptureOptions {

    private final boolean enabled;
    private final int maxLength;
    private final String[] excludeClassPatterns;

    public ErrorArgCaptureOptions(boolean enabled, int maxLength, String[] excludeClassPatterns) {
        this.enabled = enabled;
        this.maxLength = maxLength;
        this.excludeClassPatterns = copyOf(excludeClassPatterns);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String[] getExcludeClassPatterns() {
        return copyOf(excludeClassPatterns);
    }

    private static String[] copyOf(String[] value) {
        if (value == null || value.length == 0) {
            return new String[0];
        }
        String[] copy = new String[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return copy;
    }
}
