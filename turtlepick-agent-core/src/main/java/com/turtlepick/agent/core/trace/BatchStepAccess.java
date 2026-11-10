package com.turtlepick.agent.core.trace;

import java.lang.reflect.Method;
import java.util.List;

// StepExecution을 반사로 읽어 agent가 Spring Batch에 컴파일 의존을 갖지 않게 한다.
// ClassValue 캐시라 classloader를 붙잡지 않고, 접근자를 못 찾은 클래스는 다시 찾지 않는다.
final class BatchStepAccess {

    private static final BatchStepAccess UNSUPPORTED = new BatchStepAccess(null, null);

    static final ClassValue<BatchStepAccess> CACHE = new ClassValue<BatchStepAccess>() {
        @Override
        protected BatchStepAccess computeValue(Class<?> type) {
            return resolve(type);
        }
    };

    private final Method getStatus;
    private final Method getFailureExceptions;

    private BatchStepAccess(Method getStatus, Method getFailureExceptions) {
        this.getStatus = getStatus;
        this.getFailureExceptions = getFailureExceptions;
    }

    boolean isSupported() {
        return getStatus != null && getFailureExceptions != null;
    }

    String statusName(Object stepExecution) throws Exception {
        Object status = getStatus.invoke(stepExecution);
        return status instanceof Enum ? ((Enum<?>) status).name() : null;
    }

    Throwable firstFailure(Object stepExecution) throws Exception {
        Object failures = getFailureExceptions.invoke(stepExecution);
        if (!(failures instanceof List)) {
            return null;
        }
        List<?> list = (List<?>) failures;
        if (list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        return first instanceof Throwable ? (Throwable) first : null;
    }

    private static BatchStepAccess resolve(Class<?> type) {
        try {
            return new BatchStepAccess(type.getMethod("getStatus"), type.getMethod("getFailureExceptions"));
        } catch (NoSuchMethodException e) {
            return UNSUPPORTED;
        } catch (RuntimeException e) {
            return UNSUPPORTED;
        }
    }
}
