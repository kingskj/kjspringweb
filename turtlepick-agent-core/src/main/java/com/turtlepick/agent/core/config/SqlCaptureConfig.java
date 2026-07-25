package com.turtlepick.agent.core.config;

public final class SqlCaptureConfig {

    private static final int DEFAULT_MAX_BIND_VALUE_LENGTH = 512;

    private final boolean enabled;
    private final int maxBindValueLength;

    public SqlCaptureConfig(
            boolean enabled,
            int maxBindValueLength) {
        this.enabled = enabled;
        this.maxBindValueLength = maxBindValueLength > 0
                ? maxBindValueLength
                : DEFAULT_MAX_BIND_VALUE_LENGTH;
    }

    public static SqlCaptureConfig disabled() {
        return new SqlCaptureConfig(false, DEFAULT_MAX_BIND_VALUE_LENGTH);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxBindValueLength() {
        return maxBindValueLength;
    }
}
