package com.turtlepick.agent.core.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqlStatementInvocationHandler implements InvocationHandler {

    private final Statement delegate;
    private final String preparedStatement;
    private final Map<Integer, SqlBindValue> binds = new LinkedHashMap<Integer, SqlBindValue>();

    SqlStatementInvocationHandler(Statement delegate, String preparedStatement) {
        this.delegate = delegate;
        this.preparedStatement = preparedStatement;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            return SqlCaptureBridge.handleObjectMethod(proxy, delegate, name, args);
        }
        Object wrapperResult = SqlCaptureBridge.handleWrapperMethod(delegate, name, args);
        if (wrapperResult != null) {
            return wrapperResult;
        }

        boolean closeMethod = isNoArg(name, args, "close");
        boolean clearParameters = isNoArg(name, args, "clearParameters");
        boolean bindMethod = isBindMethod(name, args);
        boolean executeMethod = isExecuteMethod(name, args);
        String statement = resolveStatement(name, args);

        long startNanos = executeMethod ? System.nanoTime() : 0L;
        try {
            Object result = method.invoke(delegate, args);
            if (clearParameters) {
                binds.clear();
            } else if (bindMethod) {
                recordBind(name, args);
            }
            if (closeMethod) {
                SqlCaptureBridge.recordStatementClosed();
            }
            if (executeMethod) {
                SqlCaptureBridge.recordSql(
                        statement,
                        snapshotBinds(),
                        System.nanoTime() - startNanos,
                        rowCount(name, result),
                        null
                );
            }
            return result;
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (executeMethod) {
                SqlCaptureBridge.recordSql(
                        statement,
                        snapshotBinds(),
                        System.nanoTime() - startNanos,
                        null,
                        target
                );
            }
            throw target;
        }
    }

    private boolean isBindMethod(String name, Object[] args) {
        return preparedStatement != null
                && name.startsWith("set")
                && args != null
                && args.length >= 2
                && args[0] instanceof Integer;
    }

    private void recordBind(String setter, Object[] args) {
        int index = ((Integer) args[0]).intValue();
        Object value = args.length >= 2 ? args[1] : null;
        Integer sqlType = null;
        if ("setNull".equals(setter) && args.length >= 2 && args[1] instanceof Integer) {
            sqlType = (Integer) args[1];
            value = null;
        } else if ("setObject".equals(setter) && args.length >= 3 && args[2] instanceof Integer) {
            sqlType = (Integer) args[2];
        }
        binds.put(Integer.valueOf(index), SqlCaptureBridge.bindValue(index, setter, value, sqlType));
    }

    private boolean isExecuteMethod(String name, Object[] args) {
        if (!name.startsWith("execute")) {
            return false;
        }
        if (preparedStatement != null) {
            return true;
        }
        return args != null && args.length > 0 && args[0] instanceof String;
    }

    private String resolveStatement(String name, Object[] args) {
        if (preparedStatement != null) {
            return preparedStatement;
        }
        if (args != null && args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return null;
    }

    private Long rowCount(String methodName, Object result) {
        if (result instanceof Integer) {
            return Long.valueOf(((Integer) result).longValue());
        }
        if (result instanceof Long) {
            return (Long) result;
        }
        if (result instanceof int[]) {
            int[] rows = (int[]) result;
            long total = 0L;
            for (int i = 0; i < rows.length; i++) {
                if (rows[i] > 0) {
                    total += rows[i];
                }
            }
            return Long.valueOf(total);
        }
        if (result instanceof long[]) {
            long[] rows = (long[]) result;
            long total = 0L;
            for (int i = 0; i < rows.length; i++) {
                if (rows[i] > 0L) {
                    total += rows[i];
                }
            }
            return Long.valueOf(total);
        }
        return null;
    }

    private Map<Integer, SqlBindValue> snapshotBinds() {
        return new LinkedHashMap<Integer, SqlBindValue>(binds);
    }

    private boolean isNoArg(String name, Object[] args, String expectedName) {
        return expectedName.equals(name) && (args == null || args.length == 0);
    }
}
