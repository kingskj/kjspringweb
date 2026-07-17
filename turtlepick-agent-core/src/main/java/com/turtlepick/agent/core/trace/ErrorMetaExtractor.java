package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ErrorMetaExtractor {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_STACK_FRAMES = 30;
    private static final int MAX_CAUSE_DEPTH = 32;

    private ErrorMetaExtractor() {
    }

    public static ErrorMeta extract(Throwable throwable, String[] userFramePackages, int maxFrames) {
        if (throwable == null) {
            return new ErrorMeta(null, null, null, null, null, null);
        }

        List<Throwable> chain = causeChain(throwable);
        Throwable root = chain.isEmpty() ? throwable : chain.get(chain.size() - 1);

        return new ErrorMeta(
                safeClassName(throwable),
                truncate(safeMessage(throwable)),
                safeClassName(root),
                truncate(safeMessage(root)),
                extractStackFrames(throwable, MAX_STACK_FRAMES),
                extractUserFrames(chain, userFramePackages, maxFrames)
        );
    }

    private static List<StackFrame> extractStackFrames(Throwable throwable, int maxFrames) {
        List<StackFrame> frames = new ArrayList<StackFrame>();
        if (throwable == null || maxFrames <= 0) {
            return frames;
        }

        StackTraceElement[] stackTrace = safeStackTrace(throwable);
        for (int i = 0; i < stackTrace.length && frames.size() < maxFrames; i++) {
            StackTraceElement element = stackTrace[i];
            if (element != null) {
                frames.add(toStackFrame(element));
            }
        }
        return frames;
    }

    private static List<Throwable> causeChain(Throwable throwable) {
        List<Throwable> chain = new ArrayList<Throwable>();
        Throwable current = throwable;
        while (current != null && !chain.contains(current) && chain.size() < MAX_CAUSE_DEPTH) {
            chain.add(current);
            current = safeCause(current);
        }
        return chain;
    }

    private static List<UserFrame> extractUserFrames(
            List<Throwable> chain,
            String[] userFramePackages,
            int maxFrames
    ) {
        List<String> packages = normalizePackages(userFramePackages);
        if (chain.isEmpty() || packages.isEmpty() || maxFrames <= 0) {
            return new ArrayList<UserFrame>();
        }

        List<UserFrame> frames = new ArrayList<UserFrame>();
        Set<String> seen = new HashSet<String>();
        for (int i = chain.size() - 1; i >= 0 && frames.size() < maxFrames; i--) {
            StackTraceElement[] stackTrace = safeStackTrace(chain.get(i));
            for (int j = 0; j < stackTrace.length && frames.size() < maxFrames; j++) {
                StackTraceElement element = stackTrace[j];
                String className = element.getClassName();
                if (!isUserFrame(element, packages)) {
                    continue;
                }

                String key = className + "#" + element.getMethodName() + ":" + element.getFileName()
                        + ":" + element.getLineNumber();
                if (seen.add(key)) {
                    frames.add(new UserFrame(
                            className,
                            element.getMethodName(),
                            element.getFileName(),
                            element.getLineNumber()
                    ));
                }
            }
        }
        return frames;
    }

    private static List<String> normalizePackages(String[] userFramePackages) {
        List<String> packages = new ArrayList<String>();
        if (userFramePackages == null) {
            return packages;
        }
        for (int i = 0; i < userFramePackages.length; i++) {
            String packageName = trimToNull(userFramePackages[i]);
            if (packageName != null) {
                packages.add(packageName);
            }
        }
        return packages;
    }

    private static boolean isUserFrame(StackTraceElement element, List<String> packages) {
        if (element == null || element.getLineNumber() < 0) {
            return false;
        }
        String className = element.getClassName();
        if (className == null) {
            return false;
        }
        if (className.indexOf("$$") >= 0) {
            return false;
        }
        for (int i = 0; i < packages.size(); i++) {
            String packageName = packages.get(i);
            if (className.equals(packageName) || className.startsWith(packageName + ".")) {
                return true;
            }
        }
        return false;
    }

    private static Throwable safeCause(Throwable throwable) {
        try {
            return throwable.getCause();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static StackTraceElement[] safeStackTrace(Throwable throwable) {
        try {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            return stackTrace == null ? new StackTraceElement[0] : stackTrace;
        } catch (Throwable ignored) {
            return new StackTraceElement[0];
        }
    }

    private static StackFrame toStackFrame(StackTraceElement element) {
        return new StackFrame(
                element.getClassName(),
                element.getMethodName(),
                element.getFileName(),
                element.getLineNumber()
        );
    }

    private static String safeClassName(Throwable throwable) {
        try {
            return throwable.getClass().getName();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeMessage(Throwable throwable) {
        try {
            return throwable.getMessage();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
