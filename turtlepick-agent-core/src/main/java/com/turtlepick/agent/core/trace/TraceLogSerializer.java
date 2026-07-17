package com.turtlepick.agent.core.trace;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TraceLogSerializer {

    private static final DateTimeFormatter NODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private TraceLogSerializer() {
    }

    public static String serializeHeader(
            String commitHash,
            long createdAtMs,
            boolean verboseFieldNames,
            String serverId,
            String appName
    ) {
        return verboseFieldNames
                ? serializeVerboseHeader(commitHash, createdAtMs, serverId, appName)
                : serializeCompactHeader(commitHash, createdAtMs, serverId, appName);
    }

    private static String serializeCompactHeader(String commitHash, long createdAtMs, String serverId, String appName) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        appendStringField(builder, "f", "h");
        appendNumberField(builder, "v", Integer.valueOf(1));
        appendBooleanField(builder, "vfn", false);
        appendStringField(builder, "c", commitHash);
        appendNumberField(builder, "ts", Long.valueOf(createdAtMs));
        appendOptionalStringField(builder, "sid", serverId);
        appendOptionalStringField(builder, "app", appName);
        builder.append('}');
        return builder.toString();
    }

    private static String serializeVerboseHeader(String commitHash, long createdAtMs, String serverId, String appName) {
        StringBuilder builder = new StringBuilder(192);
        builder.append('{');
        appendStringField(builder, "recordKind", "header");
        appendNumberField(builder, "version", Integer.valueOf(1));
        appendBooleanField(builder, "verboseFieldNames", true);
        appendStringField(builder, "commitHash", commitHash);
        appendNumberField(builder, "fileCreatedAt", Long.valueOf(createdAtMs));
        appendOptionalStringField(builder, "serverId", serverId);
        appendOptionalStringField(builder, "appName", appName);
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
        appendStringField(builder, "rid", context.getTraceId());
        appendNumberField(builder, "ts", Long.valueOf(context.getOccurredAtMs()));
        appendNumberField(builder, "ep", context.getEndpointId());
        appendBooleanField(builder, "e", context.hasError());
        if (context.hasError()) {
            // 유닛4d: e:true면 erk 필수(slow 계약 불변식). agent는 현재 exception만 캡처하므로 하드코딩.
            // slow 트랙 추가 시 hasError만으로 판정 금지 — errorKind 상태값을 context에 올려야 한다(park).
            appendStringField(builder, "erk", "exception");
        }
        appendCompactError(builder, context);
        appendCompactNodes(builder, nodes, context.getOccurredAtMs());
        builder.append('}');
        return builder.toString();
    }

    private static String serializeVerbose(RuntimeTraceContext context, List<CompletedNode> nodes) {
        StringBuilder builder = new StringBuilder(256);
        builder.append('{');
        appendStringField(builder, "recordKind", "trace");
        appendStringField(builder, "requestId", context.getTraceId());
        appendNumberField(builder, "occurredAt", Long.valueOf(context.getOccurredAtMs()));
        appendNumberField(builder, "endpointId", context.getEndpointId());
        appendBooleanField(builder, "error", context.hasError());
        if (context.hasError()) {
            appendStringField(builder, "errorKind", "exception");
        }
        appendVerboseError(builder, context);
        appendVerboseNodes(builder, nodes, context.getOccurredAtMs());
        builder.append('}');
        return builder.toString();
    }

    // 유닛4a~4c: err{} = eni/ec/em/rec/rem/emx/sf/uf. sf/uf 프레임 키는 v2 계약(dc/mn/fn/ln).
    // ea/errorArgs는 v2 계약에 없어 출력하지 않는다(내부 필드/snapshotErrorArgs는 유지, params 버퍼링 유닛에서 pa로 재도입).
    private static void appendCompactError(StringBuilder builder, RuntimeTraceContext context) {
        Integer errorCallId = context.getErrorCallId();
        if (errorCallId == null) {
            return;
        }
        StringBuilder err = new StringBuilder(160);
        err.append('{');
        appendNumberField(err, "eni", errorCallId);
        appendStringField(err, "ec", context.getExceptionClass());
        appendStringField(err, "em", context.getExceptionMessage());
        appendStringField(err, "rec", rootClassOrSelf(context));
        appendStringField(err, "rem", rootMessageOrSelf(context));
        appendBooleanField(err, "emx", context.isErrorNodeMismatch());
        appendCompactStackFrames(err, context.snapshotStackFrames());
        appendCompactUserFrames(err, context.snapshotUserFrames());
        err.append('}');
        appendFieldName(builder, "err");
        builder.append(err);
    }

    private static void appendVerboseError(StringBuilder builder, RuntimeTraceContext context) {
        Integer errorCallId = context.getErrorCallId();
        if (errorCallId == null) {
            return;
        }
        StringBuilder err = new StringBuilder(192);
        err.append('{');
        appendNumberField(err, "errorNodeId", errorCallId);
        appendStringField(err, "exceptionClass", context.getExceptionClass());
        appendStringField(err, "exceptionMessage", context.getExceptionMessage());
        appendStringField(err, "rootExceptionClass", rootClassOrSelf(context));
        appendStringField(err, "rootExceptionMessage", rootMessageOrSelf(context));
        appendBooleanField(err, "errorNodeMismatch", context.isErrorNodeMismatch());
        appendVerboseStackFrames(err, context.snapshotStackFrames());
        appendVerboseUserFrames(err, context.snapshotUserFrames());
        err.append('}');
        appendFieldName(builder, "errorDetail");
        builder.append(err);
    }

    // rec/rem은 항상 출력한다. root cause가 없으면 주 예외 값으로 채운다(계약: "root 없으면 ec/em과 같은 값").
    private static String rootClassOrSelf(RuntimeTraceContext context) {
        String root = context.getRootExceptionClass();
        return root != null ? root : context.getExceptionClass();
    }

    private static String rootMessageOrSelf(RuntimeTraceContext context) {
        String root = context.getRootExceptionMessage();
        return root != null ? root : context.getExceptionMessage();
    }


    private static void appendCompactStackFrames(StringBuilder builder, List<StackFrame> stackFrames) {
        if (stackFrames == null || stackFrames.isEmpty()) {
            return;
        }
        appendFieldName(builder, "sf");
        builder.append('[');
        for (int i = 0; i < stackFrames.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            StackFrame frame = stackFrames.get(i);
            appendCompactFrame(builder, frame.getDeclaringClass(), frame.getMethodName(),
                    frame.getFileName(), frame.getLineNumber());
        }
        builder.append(']');
    }

    private static void appendVerboseStackFrames(StringBuilder builder, List<StackFrame> stackFrames) {
        if (stackFrames == null || stackFrames.isEmpty()) {
            return;
        }
        appendFieldName(builder, "stackFrames");
        builder.append('[');
        for (int i = 0; i < stackFrames.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            StackFrame frame = stackFrames.get(i);
            appendVerboseFrame(builder, frame.getDeclaringClass(), frame.getMethodName(),
                    frame.getFileName(), frame.getLineNumber());
        }
        builder.append(']');
    }

    private static void appendCompactFrame(
            StringBuilder builder,
            String declaringClass,
            String methodName,
            String fileName,
            int lineNumber
    ) {
        builder.append('{');
        builder.append("\"dc\":");
        appendJsonString(builder, declaringClass);
        builder.append(",\"mn\":");
        appendJsonString(builder, methodName);
        builder.append(",\"fn\":");
        appendJsonString(builder, fileName);
        builder.append(",\"ln\":").append(lineNumber);
        builder.append('}');
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
            appendCompactFrame(builder, frame.getDeclaringClass(), frame.getMethodName(),
                    frame.getFileName(), frame.getLineNumber());
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
            appendVerboseFrame(builder, frame.getDeclaringClass(), frame.getMethodName(),
                    frame.getFileName(), frame.getLineNumber());
        }
        builder.append(']');
    }

    private static void appendVerboseFrame(
            StringBuilder builder,
            String declaringClass,
            String methodName,
            String fileName,
            int lineNumber
    ) {
        builder.append('{');
        builder.append("\"declaringClass\":");
        appendJsonString(builder, declaringClass);
        builder.append(",\"methodName\":");
        appendJsonString(builder, methodName);
        builder.append(",\"fileName\":");
        appendJsonString(builder, fileName);
        builder.append(",\"lineNumber\":").append(lineNumber);
        builder.append('}');
    }

    private static void appendCompactNodes(StringBuilder builder, List<CompletedNode> nodes, long occurredAtMs) {
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
            builder.append(",\"st\":");
            appendJsonString(builder, formatNodeTime(occurredAtMs, node.getStartOffsetMs()));
            builder.append(",\"et\":");
            appendJsonString(builder, formatNodeTime(occurredAtMs, node.getEndOffsetMs()));
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendVerboseNodes(StringBuilder builder, List<CompletedNode> nodes, long occurredAtMs) {
        appendFieldName(builder, "nodes");
        builder.append('[');
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }

            CompletedNode node = nodes.get(i);
            builder.append('{');
            builder.append("\"nodeId\":").append(node.getCallId());
            builder.append(",\"parentNodeId\":").append(node.getParentCallId());
            builder.append(",\"methodId\":").append(node.getMethodId());
            builder.append(",\"startedAt\":");
            appendJsonString(builder, formatNodeTime(occurredAtMs, node.getStartOffsetMs()));
            builder.append(",\"endedAt\":");
            appendJsonString(builder, formatNodeTime(occurredAtMs, node.getEndOffsetMs()));
            builder.append('}');
        }
        builder.append(']');
    }

    private static String formatNodeTime(long occurredAtMs, long offsetMs) {
        return NODE_TIME_FORMAT.format(Instant.ofEpochMilli(occurredAtMs + offsetMs));
    }

    private static void appendStringField(StringBuilder builder, String key, String value) {
        appendFieldName(builder, key);
        appendJsonString(builder, value);
    }

    private static void appendOptionalStringField(StringBuilder builder, String key, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        appendStringField(builder, key, value.trim());
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
