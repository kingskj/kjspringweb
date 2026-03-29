package com.turtlepick.agent.core.http;

import java.util.ArrayList;
import java.util.List;

public final class MetaJsonCodec {

    public String encodeMetaRequest(MetaRequest request) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('{');
        builder.append("\"serverId\":\"").append(escapeJson(request.getServerId())).append("\",");
        builder.append("\"appName\":\"").append(escapeJson(request.getAppName())).append("\",");
        builder.append("\"commitHash\":\"").append(escapeJson(request.getCommitHash())).append("\"");
        builder.append('}');
        return builder.toString();
    }

    public MetaResponse decodeMetaResponse(String json) {
        if (json == null || json.trim().length() == 0) {
            return MetaResponse.logOff("EMPTY_RESPONSE");
        }

        int start = skipWhitespace(json, 0, json.length());
        if (start >= json.length() || json.charAt(start) != '{') {
            throw new IllegalArgumentException("meta response must be a JSON object");
        }

        int objectEnd = findMatchingBracket(json, start, json.length(), '{', '}');
        int end = objectEnd + 1;

        String status = readNullableStringField(json, "status", start, end);
        String reason = readNullableStringField(json, "reason", start, end);
        String agentId = readNullableStringField(json, "agentId", start, end);
        String commitHash = readNullableStringField(json, "commitHash", start, end);
        List<MethodMapping> methods = readMethods(json, start, end);
        List<EndpointInfo> endpoints = readEndpoints(json, start, end);

        return new MetaResponse(status, reason, agentId, commitHash, methods, endpoints);
    }

    private List<MethodMapping> readMethods(String json, int startIndex, int endIndex) {
        int valueStart = findFieldValueStart(json, "methods", startIndex, endIndex);
        if (valueStart < 0) {
            return new ArrayList<MethodMapping>();
        }
        if (json.charAt(valueStart) != '[') {
            throw new IllegalArgumentException("methods field is not an array");
        }

        int arrayEnd = findMatchingBracket(json, valueStart, endIndex, '[', ']');
        List<MethodMapping> result = new ArrayList<MethodMapping>();

        int index = valueStart + 1;
        while (index < arrayEnd) {
            index = skipWhitespaceAndComma(json, index, arrayEnd);
            if (index >= arrayEnd) {
                break;
            }
            if (json.charAt(index) != '{') {
                throw new IllegalArgumentException("methods array must contain objects");
            }

            int objectEnd = findMatchingBracket(json, index, arrayEnd, '{', '}');
            result.add(parseMethodObject(json, index, objectEnd + 1));
            index = objectEnd + 1;
        }

        return result;
    }

    private MethodMapping parseMethodObject(String json, int startIndex, int endIndex) {
        Integer methodId = readIntField(json, "methodId", startIndex, endIndex);
        String fqcnMethod = readNullableStringField(json, "fqcnMethod", startIndex, endIndex);

        if (methodId == null) {
            throw new IllegalArgumentException("methodId is required");
        }
        if (fqcnMethod == null || fqcnMethod.trim().length() == 0) {
            throw new IllegalArgumentException("fqcnMethod is required");
        }

        return new MethodMapping(methodId.intValue(), fqcnMethod);
    }

    private List<EndpointInfo> readEndpoints(String json, int startIndex, int endIndex) {
        int valueStart = findFieldValueStart(json, "endpoints", startIndex, endIndex);
        if (valueStart < 0) {
            return new ArrayList<EndpointInfo>();
        }
        if (json.charAt(valueStart) != '[') {
            throw new IllegalArgumentException("endpoints field is not an array");
        }

        int arrayEnd = findMatchingBracket(json, valueStart, endIndex, '[', ']');
        List<EndpointInfo> result = new ArrayList<EndpointInfo>();

        int index = valueStart + 1;
        while (index < arrayEnd) {
            index = skipWhitespaceAndComma(json, index, arrayEnd);
            if (index >= arrayEnd) {
                break;
            }
            if (json.charAt(index) != '{') {
                throw new IllegalArgumentException("endpoints array must contain objects");
            }

            int objectEnd = findMatchingBracket(json, index, arrayEnd, '{', '}');
            result.add(parseEndpointObject(json, index, objectEnd + 1));
            index = objectEnd + 1;
        }

        return result;
    }

    private EndpointInfo parseEndpointObject(String json, int startIndex, int endIndex) {
        Integer endpointId = readIntField(json, "endpointId", startIndex, endIndex);
        String entryType = readNullableStringField(json, "entryType", startIndex, endIndex);
        String entryKey = readNullableStringField(json, "entryKey", startIndex, endIndex);
        String httpMethod = readNullableStringField(json, "httpMethod", startIndex, endIndex);
        Integer entryMethodId = readIntField(json, "entryMethodId", startIndex, endIndex);

        if (endpointId == null || endpointId.intValue() <= 0) {
            throw new IllegalArgumentException("endpointId must be positive");
        }
        if (entryMethodId == null || entryMethodId.intValue() <= 0) {
            throw new IllegalArgumentException("entryMethodId must be positive");
        }
        if (entryType == null || entryType.trim().length() == 0) {
            throw new IllegalArgumentException("entryType is required");
        }
        if (entryKey == null || entryKey.trim().length() == 0) {
            throw new IllegalArgumentException("entryKey is required");
        }

        return new EndpointInfo(
                endpointId.intValue(),
                entryType,
                entryKey,
                httpMethod,
                entryMethodId.intValue()
        );
    }

    private String readNullableStringField(String json, String fieldName, int startIndex, int endIndex) {
        int valueStart = findFieldValueStart(json, fieldName, startIndex, endIndex);
        if (valueStart < 0) {
            return null;
        }

        if (startsWith(json, valueStart, endIndex, "null")) {
            return null;
        }
        if (json.charAt(valueStart) != '"') {
            throw new IllegalArgumentException(fieldName + " is not a string");
        }

        return readJsonString(json, valueStart, endIndex);
    }

    private Integer readIntField(String json, String fieldName, int startIndex, int endIndex) {
        int valueStart = findFieldValueStart(json, fieldName, startIndex, endIndex);
        if (valueStart < 0) {
            return null;
        }

        int valueEnd = valueStart;
        while (valueEnd < endIndex) {
            char ch = json.charAt(valueEnd);
            if ((ch >= '0' && ch <= '9') || ch == '-') {
                valueEnd++;
            } else {
                break;
            }
        }
        if (valueEnd == valueStart) {
            throw new IllegalArgumentException(fieldName + " is not an int");
        }

        return Integer.valueOf(Integer.parseInt(json.substring(valueStart, valueEnd)));
    }

    private int findFieldValueStart(String json, String fieldName, int startIndex, int endIndex) {
        int index = skipWhitespace(json, startIndex, endIndex);
        if (index >= endIndex || json.charAt(index) != '{') {
            throw new IllegalArgumentException("expected object while looking for field: " + fieldName);
        }

        index++;
        while (index < endIndex) {
            index = skipWhitespaceAndComma(json, index, endIndex);
            if (index >= endIndex || json.charAt(index) == '}') {
                return -1;
            }
            if (json.charAt(index) != '"') {
                throw new IllegalArgumentException("invalid object field at index " + index);
            }

            int nameEnd = findStringEnd(json, index, endIndex);
            String currentField = readJsonString(json, index, nameEnd + 1);

            int colonIndex = skipWhitespace(json, nameEnd + 1, endIndex);
            if (colonIndex >= endIndex || json.charAt(colonIndex) != ':') {
                throw new IllegalArgumentException("missing colon for field: " + currentField);
            }

            int valueStart = skipWhitespace(json, colonIndex + 1, endIndex);
            if (fieldName.equals(currentField)) {
                return valueStart;
            }

            index = skipJsonValue(json, valueStart, endIndex);
        }

        return -1;
    }

    private int skipJsonValue(String json, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return startIndex;
        }

        char ch = json.charAt(startIndex);
        if (ch == '"') {
            return findStringEnd(json, startIndex, endIndex) + 1;
        }
        if (ch == '{') {
            return findMatchingBracket(json, startIndex, endIndex, '{', '}') + 1;
        }
        if (ch == '[') {
            return findMatchingBracket(json, startIndex, endIndex, '[', ']') + 1;
        }

        int index = startIndex;
        while (index < endIndex) {
            ch = json.charAt(index);
            if (ch == ',' || ch == '}' || ch == ']' || isWhitespace(ch)) {
                break;
            }
            index++;
        }
        return index;
    }

    private int findMatchingBracket(String json, int startIndex, int endIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = startIndex; i < endIndex; i++) {
            char ch = json.charAt(i);

            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (ch == '\\') {
                    escaping = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw new IllegalArgumentException("unmatched bracket: " + open);
    }

    private int findStringEnd(String json, int quoteStartIndex, int endIndex) {
        boolean escaping = false;

        for (int i = quoteStartIndex + 1; i < endIndex; i++) {
            char ch = json.charAt(i);
            if (escaping) {
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '"') {
                return i;
            }
        }

        throw new IllegalArgumentException("unterminated string");
    }

    private String readJsonString(String json, int quoteStartIndex, int endIndex) {
        if (quoteStartIndex >= endIndex || json.charAt(quoteStartIndex) != '"') {
            throw new IllegalArgumentException("string must start with quote");
        }

        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = quoteStartIndex + 1; i < endIndex; i++) {
            char ch = json.charAt(i);
            if (escaping) {
                switch (ch) {
                    case '\\':
                        builder.append('\\');
                        break;
                    case '"':
                        builder.append('"');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '"') {
                return builder.toString();
            } else {
                builder.append(ch);
            }
        }

        throw new IllegalArgumentException("unterminated string");
    }

    private int skipWhitespace(String json, int index, int endIndex) {
        int current = index;
        while (current < endIndex && isWhitespace(json.charAt(current))) {
            current++;
        }
        return current;
    }

    private int skipWhitespaceAndComma(String json, int index, int endIndex) {
        int current = index;
        while (current < endIndex) {
            char ch = json.charAt(current);
            if (isWhitespace(ch) || ch == ',') {
                current++;
            } else {
                break;
            }
        }
        return current;
    }

    private boolean startsWith(String json, int index, int endIndex, String token) {
        int tokenLength = token.length();
        if (index + tokenLength > endIndex) {
            return false;
        }
        return token.equals(json.substring(index, index + tokenLength));
    }

    private boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(ch);
                    break;
            }
        }
        return builder.toString();
    }

}
