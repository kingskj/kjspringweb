package com.turtlepick.agent.core.instrument;

import java.util.ArrayList;
import java.util.List;

public final class MethodSignatureParser {

    public ParsedMethodSignature parse(String fqcnMethod) {
        if (fqcnMethod == null) {
            throw new IllegalArgumentException("fqcnMethod must not be null");
        }

        String trimmed = fqcnMethod.trim();
        int hashIndex = trimmed.indexOf('#');
        int openParenIndex = trimmed.indexOf('(', hashIndex + 1);
        int closeParenIndex = trimmed.lastIndexOf(')');

        if (hashIndex <= 0 || openParenIndex <= hashIndex || closeParenIndex <= openParenIndex) {
            throw new IllegalArgumentException("invalid fqcnMethod: " + fqcnMethod);
        }

        String className = trimmed.substring(0, hashIndex).trim();
        String methodName = trimmed.substring(hashIndex + 1, openParenIndex).trim();
        String paramBody = trimmed.substring(openParenIndex + 1, closeParenIndex);

        if (className.length() == 0 || methodName.length() == 0) {
            throw new IllegalArgumentException("invalid fqcnMethod: " + fqcnMethod);
        }

        List<String> paramTypeNames = new ArrayList<String>();
        if (paramBody.trim().length() > 0) {
            String[] parts = paramBody.split(",");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (part.length() == 0) {
                    throw new IllegalArgumentException("blank parameter type in: " + fqcnMethod);
                }
                paramTypeNames.add(part);
            }
        }

        return new ParsedMethodSignature(trimmed, className, methodName, paramTypeNames);
    }
}
