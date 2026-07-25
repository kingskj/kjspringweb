package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TraceSql {

    private final String statement;
    private final List<TraceSqlBind> binds;
    private final long elapsedMs;
    private final Long rowCount;
    private final String errorClass;

    public TraceSql(
            String statement,
            List<TraceSqlBind> binds,
            long elapsedMs,
            Long rowCount,
            String errorClass) {
        this.statement = statement;
        this.binds = immutableCopy(binds);
        this.elapsedMs = elapsedMs;
        this.rowCount = rowCount;
        this.errorClass = errorClass;
    }

    public String getStatement() {
        return statement;
    }

    public List<TraceSqlBind> getBinds() {
        return binds;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public String getErrorClass() {
        return errorClass;
    }

    private static List<TraceSqlBind> immutableCopy(List<TraceSqlBind> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<TraceSqlBind>(source));
    }
}
