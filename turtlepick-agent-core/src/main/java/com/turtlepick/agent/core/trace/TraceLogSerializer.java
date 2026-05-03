package com.turtlepick.agent.core.trace;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TraceLogSerializer {

    private TraceLogSerializer() {
    }

    public static String serializeHeader(String commitHash, long createdAtMs, boolean verboseFieldNames) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        appendStringField(builder, "f", "h");
        appendNumberField(builder, "v", Integer.valueOf(1));
        appendBooleanField(builder, "vfn", verboseFieldNames);
        appendStringField(builder, "c", commitHash);
        appendNumberField(builder, "ts", Long.valueOf(createdAtMs));
        builder.append('}');
        return builder.toString();
    }

    public static String serialize(RuntimeTraceContext context, boolean verboseFieldNames) {
        List<CompletedNode> nodes = context.snapshotNodes();
        Collections.sort(nodes, new Comparator<CompletedNode>() {
            @Override
            public int compare(CompletedNode left, CompletedNode right) {
                return Integer.compare(left.getCallId(), right.getCallId());
            }
        });

        return verboseFieldNames ? serializeVerbose(context, nodes) : serializeCompact(context, nodes);
    }

    private static String serializeCompact(RuntimeTraceContext context, List<CompletedNode> nodes) {
        StringBuilder builder = new StringBuilder(192);
        builder.append('{');
        appendStringField(builder, "f", "t");
        appendNumberField(builder, "ep", context.getEndpointId());
        appendBooleanField(builder, "e", context.hasError());
        appendCompactError(builder, context);
        appendCompactNodes(builder, nodes);
        builder.append('}');
        return builder.toString();
    }

    private static String serializeVerbose(RuntimeTraceContext context, List<CompletedNode> nodes) {
        StringBuilder builder = new StringBuilder(256);
        builder.append('{');
        appendStringField(builder, "format", "trace");
        appendNumberField(builder, "endpointId", context.getEndpointId());
        appendBooleanField(builder, "error", context.hasError());
        appendVerboseError(builder, context);
        appendVerboseNodes(builder, nodes);
        builder.append('}');
        return builder.toString();
    }

    private static void appendCompactError(StringBuilder builder, RuntimeTraceContext context) {
        Integer errorCallId = context.getErrorCallId();
        if (errorCallId == null) {
            return;
        }
        appendNumberField(builder, "eci", errorCallId);
        appendStringField(builder, "ec", context.getExceptionClass());
        appendStringField(builder, "em", context.getExceptionMessage());
        if (hasDistinctRootException(context)) {
            appendStringField(builder, "rc", context.getRootExceptionClass());
            appendStringField(builder, "rm", context.getRootExceptionMessage());
        }
        appendCompactUserFrames(builder, context.snapshotUserFrames());
        appendStringArrayField(builder, "ea", context.snapshotErrorArgs());
    }

    private static void appendVerboseError(StringBuilder builder, RuntimeTraceContext context) {
        Integer errorCallId = context.getErrorCallId();
        if (errorCallId == null) {
            return;
        }
        appendNumberField(builder, "errorCallId", errorCallId);
        appendStringField(builder, "exceptionClass", context.getExceptionClass());
        appendStringField(builder, "exceptionMessage", context.getExceptionMessage());
        if (hasDistinctRootException(context)) {
            appendStringField(builder, "rootExceptionClass", context.getRootExceptionClass());
            appendStringField(builder, "rootExceptionMessage", context.getRootExceptionMessage());
        }
        appendVerboseUserFrames(builder, context.snapshotUserFrames());
        appendStringArrayField(builder, "errorArgs", context.snapshotErrorArgs());
    }

    private static void appendStringArrayField(StringBuilder builder, String key, String[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        appendFieldName(builder, key);
        builder.append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            appendJsonString(builder, values[i]);
        }
        builder.append(']');
    }

    private static boolean hasDistinctRootException(RuntimeTraceContext context) {
        String exceptionClass = context.getExceptionClass();
        String rootExceptionClass = context.getRootExceptionClass();
        return rootExceptionClass != null && !rootExceptionClass.equals(exceptionClass);
    }

    private static void appendCompactUserFrames(StringBuilder builder, List<UserFrame> userFrames) {
        if (userFrames == null || userFrames.isEmpty()) {
            return;
        }
        appendFieldName(builder, "uf");
        builder.append('[');
        for (int i = 0; i < userFrames.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            UserFrame frame = userFrames.get(i);
            builder.append('{');
            builder.append("\"c\":");
            appendJsonString(builder, frame.getClassName());
            builder.append(",\"m\":");
            appendJsonString(builder, frame.getMethodName());
            builder.append(",\"l\":").append(frame.getLineNumber());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendVerboseUserFrames(StringBuilder builder, List<UserFrame> userFrames) {
        if (userFrames == null || userFrames.isEmpty()) {
            return;
        }
        appendFieldName(builder, "userFrames");
        builder.append('[');
        for (int i = 0; i < userFrames.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            UserFrame frame = userFrames.get(i);
            builder.append('{');
            builder.append("\"className\":");
            appendJsonString(builder, frame.getClassName());
            builder.append(",\"methodName\":");
            appendJsonString(builder, frame.getMethodName());
            builder.append(",\"lineNumber\":").append(frame.getLineNumber());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendCompactNodes(StringBuilder builder, List<CompletedNode> nodes) {
        appendFieldName(builder, "n");
        builder.append('[');
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }

            CompletedNode node = nodes.get(i);
            builder.append('{');
            builder.append("\"i\":").append(node.getCallId());
            builder.append(",\"p\":").append(node.getParentCallId());
            builder.append(",\"m\":").append(node.getMethodId());
            builder.append(",\"st\":").append(node.getStartOffsetMs());
            builder.append(",\"et\":").append(node.getEndOffsetMs());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendVerboseNodes(StringBuilder builder, List<CompletedNode> nodes) {
        appendFieldName(builder, "nodes");
        builder.append('[');
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }

            CompletedNode node = nodes.get(i);
            builder.append('{');
            builder.append("\"callId\":").append(node.getCallId());
            builder.append(",\"parentCallId\":").append(node.getParentCallId());
            builder.append(",\"methodId\":").append(node.getMethodId());
            builder.append(",\"startOffsetMs\":").append(node.getStartOffsetMs());
            builder.append(",\"endOffsetMs\":").append(node.getEndOffsetMs());
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendStringField(StringBuilder builder, String key, String value) {
        appendFieldName(builder, key);
        appendJsonString(builder, value);
    }

    private static void appendJsonString(StringBuilder builder, String value) {
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

    private static void appendBooleanField(StringBuilder builder, String key, boolean value) {
        appendFieldName(builder, key);
        builder.append(value);
    }

    private static void appendFieldName(StringBuilder builder, String key) {
        if (builder.length() > 1) {
            builder.append(',');
        }
        builder.append('"').append(key).append("\":");
    }
}
