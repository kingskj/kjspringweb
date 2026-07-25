package com.turtlepick.agent.core.sql;

import com.turtlepick.agent.core.config.SqlCaptureConfig;
import com.turtlepick.agent.core.trace.RuntimeSqlCaptureBridge;
import com.turtlepick.agent.core.trace.TraceSqlBind;
import com.turtlepick.agent.core.util.AgentLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class SqlCaptureBridge {

    private static final AtomicLong CONNECTION_WRAPPED = new AtomicLong();
    private static final AtomicLong CONNECTION_CLOSED = new AtomicLong();
    private static final AtomicLong STATEMENT_WRAPPED = new AtomicLong();
    private static final AtomicLong STATEMENT_CLOSED = new AtomicLong();
    private static final AtomicBoolean WRAP_WARNED = new AtomicBoolean();

    private static volatile SqlCaptureConfig config = SqlCaptureConfig.disabled();

    private SqlCaptureBridge() {
    }

    public static void install(SqlCaptureConfig value) {
        config = value == null ? SqlCaptureConfig.disabled() : value;
        AgentLog.info("sql capture bridge installed"
                + " enabled=" + config.isEnabled()
                + " maxBindValueLength=" + config.getMaxBindValueLength());
    }

    public static Connection wrapConnection(Connection connection) {
        if (connection == null || !config.isEnabled()) {
            return connection;
        }
        if (isProxyOf(connection, SqlConnectionInvocationHandler.class)) {
            return connection;
        }
        try {
            Connection proxy = (Connection) Proxy.newProxyInstance(
                    resolveProxyClassLoader(connection.getClass()),
                    new Class[]{Connection.class},
                    new SqlConnectionInvocationHandler(connection)
            );
            CONNECTION_WRAPPED.incrementAndGet();
            return proxy;
        } catch (Throwable t) {
            rethrowFatal(t);
            warnOnce("connection wrap skipped cause=" + t.getClass().getSimpleName()
                    + ":" + safeMessage(t));
            return connection;
        }
    }

    static Object wrapPreparedStatement(PreparedStatement statement, String sql) {
        if (statement == null || !config.isEnabled()) {
            return statement;
        }
        if (isProxyOf(statement, SqlStatementInvocationHandler.class)) {
            return statement;
        }
        try {
            Class<?> primaryInterface = statement instanceof CallableStatement
                    ? CallableStatement.class
                    : PreparedStatement.class;
            Object proxy = Proxy.newProxyInstance(
                    resolveProxyClassLoader(statement.getClass()),
                    new Class[]{primaryInterface},
                    new SqlStatementInvocationHandler(statement, sql)
            );
            STATEMENT_WRAPPED.incrementAndGet();
            return proxy;
        } catch (Throwable t) {
            rethrowFatal(t);
            warnOnce("prepared statement wrap skipped cause=" + t.getClass().getSimpleName()
                    + ":" + safeMessage(t));
            return statement;
        }
    }

    static Object wrapStatement(Statement statement) {
        if (statement == null || !config.isEnabled()) {
            return statement;
        }
        if (isProxyOf(statement, SqlStatementInvocationHandler.class)) {
            return statement;
        }
        try {
            Object proxy = Proxy.newProxyInstance(
                    resolveProxyClassLoader(statement.getClass()),
                    new Class[]{Statement.class},
                    new SqlStatementInvocationHandler(statement, null)
            );
            STATEMENT_WRAPPED.incrementAndGet();
            return proxy;
        } catch (Throwable t) {
            rethrowFatal(t);
            warnOnce("statement wrap skipped cause=" + t.getClass().getSimpleName()
                    + ":" + safeMessage(t));
            return statement;
        }
    }

    static SqlBindValue bindValue(int index, String setter, Object value, Integer sqlType) {
        return SqlBindValue.of(index, setter, value, sqlType, config.getMaxBindValueLength());
    }

    static void recordConnectionClosed() {
        CONNECTION_CLOSED.incrementAndGet();
    }

    static void recordStatementClosed() {
        STATEMENT_CLOSED.incrementAndGet();
    }

    static void recordSql(
            String statement,
            Map<Integer, SqlBindValue> binds,
            long elapsedNanos,
            Long rowCount,
            Throwable error) {
        RuntimeSqlCaptureBridge.onSqlExecuted(
                statement,
                toTraceSqlBinds(binds),
                elapsedNanos / 1000000L,
                rowCount,
                error == null ? null : errorName(error)
        );
    }

    private static List<TraceSqlBind> toTraceSqlBinds(Map<Integer, SqlBindValue> binds) {
        if (binds == null || binds.isEmpty()) {
            return new ArrayList<TraceSqlBind>(0);
        }
        TreeMap<Integer, SqlBindValue> sorted = new TreeMap<Integer, SqlBindValue>(binds);
        ArrayList<TraceSqlBind> result = new ArrayList<TraceSqlBind>(sorted.size());
        for (SqlBindValue bind : sorted.values()) {
            result.add(new TraceSqlBind(
                    bind.getIndex(),
                    bind.getSetter(),
                    bind.getValueClassName(),
                    bind.getValueText(),
                    bind.getSqlType(),
                    bind.isNullValue()
            ));
        }
        return result;
    }

    static Object handleObjectMethod(Object proxy, Object delegate, String name, Object[] args) {
        if ("toString".equals(name)) {
            return "TurtlePickSqlProxy(" + delegate + ")";
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(System.identityHashCode(proxy));
        }
        if ("equals".equals(name)) {
            return Boolean.valueOf(args != null && args.length == 1 && proxy == args[0]);
        }
        return null;
    }

    static Object handleWrapperMethod(Object delegate, String name, Object[] args) throws Throwable {
        if ("unwrap".equals(name) && args != null && args.length == 1 && args[0] instanceof Class) {
            Class<?> type = (Class<?>) args[0];
            if (type.isInstance(delegate)) {
                return delegate;
            }
        }
        if ("isWrapperFor".equals(name) && args != null && args.length == 1 && args[0] instanceof Class) {
            Class<?> type = (Class<?>) args[0];
            if (type.isInstance(delegate)) {
                return Boolean.TRUE;
            }
        }
        return null;
    }

    private static boolean isProxyOf(Object value, Class<?> handlerClass) {
        if (value == null || !Proxy.isProxyClass(value.getClass())) {
            return false;
        }
        try {
            InvocationHandler handler = Proxy.getInvocationHandler(value);
            return handlerClass.isInstance(handler);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static ClassLoader resolveProxyClassLoader(Class<?> delegateClass) {
        ClassLoader loader = delegateClass == null ? null : delegateClass.getClassLoader();
        if (loader != null) {
            return loader;
        }
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            return context;
        }
        return SqlCaptureBridge.class.getClassLoader();
    }

    private static void warnOnce(String message) {
        if (WRAP_WARNED.compareAndSet(false, true)) {
            AgentLog.warn("sql capture " + message);
        }
    }

    private static void rethrowFatal(Throwable throwable) {
        if (throwable instanceof VirtualMachineError || throwable instanceof ThreadDeath) {
            throw (Error) throwable;
        }
    }

    private static String errorName(Throwable throwable) {
        return throwable.getClass().getName();
    }

    private static String safeMessage(Throwable throwable) {
        return throwable.getMessage() == null ? "" : throwable.getMessage();
    }
}
