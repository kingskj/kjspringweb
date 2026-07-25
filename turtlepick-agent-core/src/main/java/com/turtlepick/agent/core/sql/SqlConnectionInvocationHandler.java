package com.turtlepick.agent.core.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

final class SqlConnectionInvocationHandler implements InvocationHandler {

    private final Connection delegate;

    SqlConnectionInvocationHandler(Connection delegate) {
        this.delegate = delegate;
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
        try {
            Object result = method.invoke(delegate, args);
            if (closeMethod) {
                SqlCaptureBridge.recordConnectionClosed();
            }
            if (result instanceof PreparedStatement && isPreparedFactory(name, args)) {
                return SqlCaptureBridge.wrapPreparedStatement((PreparedStatement) result, (String) args[0]);
            }
            if (result instanceof Statement && isStatementFactory(name, args)) {
                return SqlCaptureBridge.wrapStatement((Statement) result);
            }
            return result;
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private boolean isPreparedFactory(String name, Object[] args) {
        return ("prepareStatement".equals(name) || "prepareCall".equals(name))
                && args != null
                && args.length > 0
                && args[0] instanceof String;
    }

    private boolean isStatementFactory(String name, Object[] args) {
        return "createStatement".equals(name);
    }

    private boolean isNoArg(String name, Object[] args, String expectedName) {
        return expectedName.equals(name) && (args == null || args.length == 0);
    }
}
