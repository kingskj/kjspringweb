package com.turtlepick.agent.core.trace;

import java.util.List;

/**
 * 유닛4c: emx 정본 규칙(작업지시문_20260712 §8)의 "source 기준 userFrame"을 계산한다.
 *
 * 입력 userFrames는 이미 사용자 패키지 필터 + CGLIB($$) 제외를 거친 raw 배열이다.
 * 여기서는 배열을 지우지 않고, source userFrame 하나만 계산해서 돌려준다(emx 판정용).
 *
 * 규칙:
 * - lambda$X$... 프레임은 버리지 않고 enclosing method X로 정규화한다.
 *   같은 class/line의 enclosing 프레임이 뒤에 있으면 그 프레임을, 없으면 virtual 프레임(methodName=X)을 쓴다.
 * - access$... 컴파일러 생성 접근자 보조 프레임은 skip한다.
 * - 그 외 첫 프레임을 source userFrame으로 본다.
 */
public final class SourceUserFrameResolver {

    private SourceUserFrameResolver() {
    }

    public static UserFrame resolve(List<UserFrame> userFrames) {
        if (userFrames == null || userFrames.isEmpty()) {
            return null;
        }
        for (int i = 0; i < userFrames.size(); i++) {
            UserFrame frame = userFrames.get(i);
            String methodName = frame.getMethodName();
            if (methodName == null) {
                continue;
            }
            if (methodName.startsWith("access$")) {
                continue;
            }
            if (methodName.startsWith("lambda$")) {
                String enclosing = extractEnclosingMethod(methodName);
                if (enclosing == null) {
                    continue;
                }
                UserFrame enclosingFrame = findEnclosingFrame(
                        userFrames, i + 1, frame.getDeclaringClass(), frame.getLineNumber(), enclosing);
                if (enclosingFrame != null) {
                    return enclosingFrame;
                }
                return new UserFrame(frame.getDeclaringClass(), enclosing, frame.getFileName(), frame.getLineNumber());
            }
            return frame;
        }
        return null;
    }

    // lambda$getDetail$1 -> getDetail, lambda$foo$bar$1 -> foo
    private static String extractEnclosingMethod(String lambdaMethodName) {
        String rest = lambdaMethodName.substring("lambda$".length());
        int next = rest.indexOf('$');
        String enclosing = next >= 0 ? rest.substring(0, next) : rest;
        return enclosing.length() == 0 ? null : enclosing;
    }

    private static UserFrame findEnclosingFrame(
            List<UserFrame> frames, int fromIndex, String className, int lineNumber, String enclosing) {
        for (int i = fromIndex; i < frames.size(); i++) {
            UserFrame candidate = frames.get(i);
            if (className != null
                    && className.equals(candidate.getDeclaringClass())
                    && lineNumber == candidate.getLineNumber()
                    && enclosing.equals(candidate.getMethodName())) {
                return candidate;
            }
        }
        return null;
    }
}
