package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MethodFrame {

    private final int callId;
    private final int parentCallId;
    private final int methodId;
    private final String fqcnMethod;
    private final long startNanoTime;
    private final Object[] args;
    // SQL attach 자격은 frame 생성 경로로 결정한다: Repository(enterInterfaceMethod) /
    // Mapper(enterDeclaredInterfaceMethod) frame만 true. 일반 method probe(Controller/Service)는 false.
    // registry id-set(declaredMethods 전체 적재) 판정은 Service까지 오염시켜 폐기했다.
    private final boolean sqlAttachAllowed;
    private final List<TraceSql> sqlPayloads = new ArrayList<TraceSql>();

    public MethodFrame(int callId, int parentCallId, int methodId, String fqcnMethod, long startNanoTime, Object[] args, boolean sqlAttachAllowed) {
        this.callId = callId;
        this.parentCallId = parentCallId;
        this.methodId = methodId;
        this.fqcnMethod = fqcnMethod;
        this.startNanoTime = startNanoTime;
        this.args = args;
        this.sqlAttachAllowed = sqlAttachAllowed;
    }

    public boolean isSqlAttachAllowed() {
        return sqlAttachAllowed;
    }

    public int getCallId() {
        return callId;
    }

    public int getParentCallId() {
        return parentCallId;
    }

    public int getMethodId() {
        return methodId;
    }

    public String getFqcnMethod() {
        return fqcnMethod;
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public Object[] getArgs() {
        return args;
    }

    public boolean appendSql(TraceSql sql, int perNodeLimit) {
        if (sql == null || perNodeLimit <= 0 || sqlPayloads.size() >= perNodeLimit) {
            return false;
        }
        sqlPayloads.add(sql);
        return true;
    }

    public List<TraceSql> snapshotSqlPayloads() {
        if (sqlPayloads.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<TraceSql>(sqlPayloads);
    }

    public int getSqlPayloadCount() {
        return sqlPayloads.size();
    }
}
