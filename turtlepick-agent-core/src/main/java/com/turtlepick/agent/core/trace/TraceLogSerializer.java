package com.turtlepick.agent.core.trace;

public final class TraceLogSerializer {

    private TraceLogSerializer() {
    }

    public static String serialize(RuntimeTraceContext context, long timestampMs) {
        StringBuilder builder = new StringBuilder(384);
        builder.append('{');
        appendStringField(builder, "traceId", context.getTraceId());
        appendNumberField(builder, "entryMethodId", Long.valueOf(context.getEntryMethodId()));
        appendStringField(builder, "entryFqcnMethod", context.getEntryFqcnMethod());
        appendNumberField(builder, "endpointId", context.getEndpointId());
        appendStringField(builder, "endpointEntryType", context.getEndpointEntryType());
        appendStringField(builder, "endpointEntryKey", context.getEndpointEntryKey());
        appendStringField(builder, "endpointHttpMethod", context.getEndpointHttpMethod());
        appendStringField(builder, "requestMethod", context.getRequestMethod());
        appendStringField(builder, "requestUri", context.getRequestUri());
        appendStringField(builder, "endpointResolutionStatus", context.getEndpointResolutionStatus());
        appendNumberField(builder, "timestampMs", Long.valueOf(timestampMs));
        builder.append('}');
        return builder.toString();
    }

    private static void appendStringField(StringBuilder builder, String key, String value) {
        appendFieldName(builder, key);
        if (value == null) {
            builder.append("null");
            return;
        }

        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append("\\u");
                        String hex = Integer.toHexString(ch);
                        for (int j = hex.length(); j < 4; j++) {
                            builder.append('0');
                        }
                        builder.append(hex);
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        builder.append('"');
    }

    private static void appendNumberField(StringBuilder builder, String key, Number value) {
        appendFieldName(builder, key);
        if (value == null) {
            builder.append("null");
        } else {
            builder.append(value);
        }
    }

    private static void appendFieldName(StringBuilder builder, String key) {
        if (builder.length() > 1) {
            builder.append(',');
        }
        builder.append('"').append(key).append("\":");
    }
}
