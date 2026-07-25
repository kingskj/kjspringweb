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
        boolean traceError = context.isTraceError();
        StringBuilder builder = new StringBuilder(192);
        builder.append('{');
        appendStringField(builder, "f", "t");
        appendStringField(builder, "rid", context.getTraceId());
        appendNumberField(builder, "ts", Long.valueOf(context.getOccurredAtMs()));
        appendNumberField(builder, "ep", context.getEndpointId());
        appendBooleanField(builder, "e", traceError);
        if (traceError) {
            appendStringField(builder, "erk", context.getErrorKind());
        }
        if (context.hasSlowObserved()) {
            appendNumberField(builder, "du", Long.valueOf(context.getDurationMs()));
            appendNumberField(builder, "th", Integer.valueOf(context.getThresholdMs()));
        }
        appendCompactError(builder, context);
        if (shouldWriteHttpStatus(context, traceError)) {
            appendNumberField(builder, "hs", context.getHttpStatus());
        }
        appendCompactNodes(builder, nodes, context.getOccurredAtMs(), traceError);
        builder.append('}');
        return builder.toString();
    }

    private static String serializeVerbose(RuntimeTraceContext context, List<CompletedNode> nodes) {
        boolean traceError = context.isTraceError();
        StringBuilder builder = new StringBuilder(256);
        builder.append('{');
        appendStringField(builder, "recordKind", "trace");
        appendStringField(builder, "requestId", context.getTraceId());
        appendNumberField(builder, "occurredAt", Long.valueOf(context.getOccurredAtMs()));
        appendNumberField(builder, "endpointId", context.getEndpointId());
        appendBooleanField(builder, "error", traceError);
        if (traceError) {
            appendStringField(builder, "errorKind", context.getErrorKind());
        }
        if (context.hasSlowObserved()) {
            appendNumberField(builder, "durationMs", Long.valueOf(context.getDurationMs()));
            appendNumberField(builder, "thresholdMs", Integer.valueOf(context.getThresholdMs()));
        }
        appendVerboseError(builder, context);
        if (shouldWriteHttpStatus(context, traceError)) {
            appendNumberField(builder, "httpStatus", context.getHttpStatus());
        }
        appendVerboseNodes(builder, nodes, context.getOccurredAtMs(), traceError);
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

    private static void appendCompactNodes(StringBuilder builder, List<CompletedNode> nodes, long occurredAtMs, boolean traceError) {
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
            appendCompactParams(builder, node.getParams());
            appendCompactSql(builder, node.getSqlPayloads(), traceError);
            builder.append('}');
        }
        builder.append(']');
    }

    private static void appendVerboseNodes(StringBuilder builder, List<CompletedNode> nodes, long occurredAtMs, boolean traceError) {
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
            appendVerboseParams(builder, node.getParams());
            appendVerboseSql(builder, node.getSqlPayloads(), traceError);
            builder.append('}');
        }
        builder.append(']');
    }

    private static boolean shouldWriteHttpStatus(RuntimeTraceContext context, boolean traceError) {
        return context.getHttpStatus() != null
                && (traceError || context.shouldEmitHttpStatus());
    }

    private static void appendCompactParams(StringBuilder builder, List<TraceParam> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        appendFieldName(builder, "pa");
        builder.append('[');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceParam param = params.get(i);
            StringBuilder paramBuilder = new StringBuilder(96);
            paramBuilder.append('{');
            appendStringField(paramBuilder, "pn", param.getName());
            appendStringField(paramBuilder, "pt", param.getType());
            appendStringField(paramBuilder, "pv", param.getValue());
            appendCompactParamFields(paramBuilder, param.getFields());
            paramBuilder.append('}');
            builder.append(paramBuilder);
        }
        builder.append(']');
    }

    private static void appendCompactParamFields(StringBuilder builder, List<TraceParamField> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        appendFieldName(builder, "pf");
        builder.append('{');
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceParamField field = fields.get(i);
            appendJsonString(builder, field.getName());
            builder.append(':');
            StringBuilder fieldBuilder = new StringBuilder(64);
            fieldBuilder.append('{');
            appendStringField(fieldBuilder, "pt", field.getType());
            appendStringField(fieldBuilder, "pv", field.getValue());
            fieldBuilder.append('}');
            builder.append(fieldBuilder);
        }
        builder.append('}');
    }

    private static void appendVerboseParams(StringBuilder builder, List<TraceParam> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        appendFieldName(builder, "params");
        builder.append('[');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceParam param = params.get(i);
            StringBuilder paramBuilder = new StringBuilder(128);
            paramBuilder.append('{');
            appendStringField(paramBuilder, "name", param.getName());
            appendStringField(paramBuilder, "type", param.getType());
            appendStringField(paramBuilder, "value", param.getValue());
            appendVerboseParamFields(paramBuilder, param.getFields());
            paramBuilder.append('}');
            builder.append(paramBuilder);
        }
        builder.append(']');
    }

    private static void appendVerboseParamFields(StringBuilder builder, List<TraceParamField> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        appendFieldName(builder, "fields");
        builder.append('{');
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceParamField field = fields.get(i);
            appendJsonString(builder, field.getName());
            builder.append(':');
            StringBuilder fieldBuilder = new StringBuilder(80);
            fieldBuilder.append('{');
            appendStringField(fieldBuilder, "type", field.getType());
            appendStringField(fieldBuilder, "value", field.getValue());
            fieldBuilder.append('}');
            builder.append(fieldBuilder);
        }
        builder.append('}');
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

    // 유닛2: SQL payload는 e:true(exception/slow) trace에만 출력한다(traceError). e:false면 미출력.
    // 키는 trace v2 §5-3: compact q/qs/qb/qe/qr. bind는 typed object({i,s,vc,v,t,n}).
    // errorClass는 trace에 출력하지 않는다(실패 여부는 요청 err{}가 담당). rowCount null이면 qr 생략.
    private static void appendCompactSql(StringBuilder builder, List<TraceSql> sqlPayloads, boolean traceError) {
        if (!traceError || sqlPayloads == null || sqlPayloads.isEmpty()) {
            return;
        }
        appendFieldName(builder, "q");
        builder.append('[');
        for (int i = 0; i < sqlPayloads.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceSql sql = sqlPayloads.get(i);
            StringBuilder sqlBuilder = new StringBuilder(128);
            sqlBuilder.append('{');
            appendStringField(sqlBuilder, "qs", sql.getStatement());
            appendCompactBinds(sqlBuilder, sql.getBinds());
            appendFieldName(sqlBuilder, "qe");
            sqlBuilder.append(sql.getElapsedMs());
            if (sql.getRowCount() != null) {
                appendFieldName(sqlBuilder, "qr");
                sqlBuilder.append(sql.getRowCount().longValue());
            }
            sqlBuilder.append('}');
            builder.append(sqlBuilder);
        }
        builder.append(']');
    }

    private static void appendCompactBinds(StringBuilder builder, List<TraceSqlBind> binds) {
        appendFieldName(builder, "qb");
        builder.append('[');
        if (binds != null) {
            for (int i = 0; i < binds.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                TraceSqlBind bind = binds.get(i);
                StringBuilder bindBuilder = new StringBuilder(64);
                bindBuilder.append('{');
                appendFieldName(bindBuilder, "i");
                bindBuilder.append(bind.getIndex());
                appendStringField(bindBuilder, "s", bind.getSetter());
                if (!bind.isNullValue()) {
                    appendStringField(bindBuilder, "vc", bind.getValueClassName());
                    appendStringField(bindBuilder, "v", bind.getValueText());
                }
                if (bind.getSqlType() != null) {
                    appendFieldName(bindBuilder, "t");
                    bindBuilder.append(bind.getSqlType().intValue());
                }
                appendFieldName(bindBuilder, "n");
                bindBuilder.append(bind.isNullValue());
                bindBuilder.append('}');
                builder.append(bindBuilder);
            }
        }
        builder.append(']');
    }

    private static void appendVerboseSql(StringBuilder builder, List<TraceSql> sqlPayloads, boolean traceError) {
        if (!traceError || sqlPayloads == null || sqlPayloads.isEmpty()) {
            return;
        }
        appendFieldName(builder, "sql");
        builder.append('[');
        for (int i = 0; i < sqlPayloads.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            TraceSql sql = sqlPayloads.get(i);
            StringBuilder sqlBuilder = new StringBuilder(160);
            sqlBuilder.append('{');
            appendStringField(sqlBuilder, "statement", sql.getStatement());
            appendVerboseBinds(sqlBuilder, sql.getBinds());
            appendFieldName(sqlBuilder, "elapsedMs");
            sqlBuilder.append(sql.getElapsedMs());
            if (sql.getRowCount() != null) {
                appendFieldName(sqlBuilder, "rowCount");
                sqlBuilder.append(sql.getRowCount().longValue());
            }
            sqlBuilder.append('}');
            builder.append(sqlBuilder);
        }
        builder.append(']');
    }

    private static void appendVerboseBinds(StringBuilder builder, List<TraceSqlBind> binds) {
        appendFieldName(builder, "binds");
        builder.append('[');
        if (binds != null) {
            for (int i = 0; i < binds.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                TraceSqlBind bind = binds.get(i);
                StringBuilder bindBuilder = new StringBuilder(96);
                bindBuilder.append('{');
                appendFieldName(bindBuilder, "index");
                bindBuilder.append(bind.getIndex());
                appendStringField(bindBuilder, "setter", bind.getSetter());
                if (!bind.isNullValue()) {
                    appendStringField(bindBuilder, "valueClass", bind.getValueClassName());
                    appendStringField(bindBuilder, "value", bind.getValueText());
                }
                if (bind.getSqlType() != null) {
                    appendFieldName(bindBuilder, "sqlType");
                    bindBuilder.append(bind.getSqlType().intValue());
                }
                appendFieldName(bindBuilder, "nullValue");
                bindBuilder.append(bind.isNullValue());
                bindBuilder.append('}');
                builder.append(bindBuilder);
            }
        }
        builder.append(']');
    }

    private static void appendFieldName(StringBuilder builder, String key) {
        if (builder.length() > 1) {
            builder.append(',');
        }
        builder.append('"').append(key).append("\":");
    }
}
