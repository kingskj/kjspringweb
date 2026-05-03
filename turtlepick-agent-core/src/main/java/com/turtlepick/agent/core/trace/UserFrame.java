package com.turtlepick.agent.core.trace;

public final class UserFrame {

    private final String className;
    private final String methodName;
    private final int lineNumber;

    public UserFrame(String className, String methodName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
