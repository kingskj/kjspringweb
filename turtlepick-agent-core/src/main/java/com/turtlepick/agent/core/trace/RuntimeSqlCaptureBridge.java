package com.turtlepick.agent.core.trace;

import com.turtlepick.agent.core.util.AgentLog;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RuntimeSqlCaptureBridge {

    static final int PER_NODE_LIMIT = 50;
    static final int PER_TRACE_LIMIT = 200;

    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private RuntimeSqlCaptureBridge() {
    }

    public static void onSqlExecuted(
            String statement,
            List<TraceSqlBind> binds,
            long elapsedMs,
            Long rowCount,
            String errorClass) {
        try {
            String normalizedStatement = trimToNull(statement);
            if (normalizedStatement == null) {
                return;
            }
            RuntimeTraceContext context = TraceContextHolder.get();
            if (context == null) {
                return;
            }
            // SQL attach 자격은 top frame의 sqlAttachAllowed(생성 경로)로 판정한다. registry 불필요.
            TraceSql sql = new TraceSql(normalizedStatement, binds, elapsedMs, rowCount, errorClass);
            context.tryAttachSqlToCurrentFrame(
                    sql,
                    PER_NODE_LIMIT,
                    PER_TRACE_LIMIT
            );
        } catch (Throwable t) {
            if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                throw (Error) t;
            }
            warnOnce("runtime sql capture failed cause=" + t.getClass().getSimpleName());
        }
    }

    private static void warnOnce(String message) {
        if (WARNED.compareAndSet(false, true)) {
            AgentLog.warn(message);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
