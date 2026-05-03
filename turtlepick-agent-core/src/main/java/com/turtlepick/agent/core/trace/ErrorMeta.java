package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ErrorMeta {

    private final String exceptionClass;
    private final String exceptionMessage;
    private final String rootExceptionClass;
    private final String rootExceptionMessage;
    private final List<UserFrame> userFrames;

    public ErrorMeta(
            String exceptionClass,
            String exceptionMessage,
            String rootExceptionClass,
            String rootExceptionMessage,
            List<UserFrame> userFrames
    ) {
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.rootExceptionClass = rootExceptionClass;
        this.rootExceptionMessage = rootExceptionMessage;
        if (userFrames == null || userFrames.isEmpty()) {
            this.userFrames = Collections.emptyList();
        } else {
            this.userFrames = Collections.unmodifiableList(new ArrayList<UserFrame>(userFrames));
        }
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getRootExceptionClass() {
        return rootExceptionClass;
    }

    public String getRootExceptionMessage() {
        return rootExceptionMessage;
    }

    public List<UserFrame> getUserFrames() {
        return userFrames;
    }
}
