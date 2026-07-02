package com.turtlepick.agent.core.http;

import static com.turtlepick.agent.core.http.JsonCodecSupport.escapeJson;
import static com.turtlepick.agent.core.http.JsonCodecSupport.findFieldValueStart;
import static com.turtlepick.agent.core.http.JsonCodecSupport.findMatchingBracket;
import static com.turtlepick.agent.core.http.JsonCodecSupport.readIntField;
import static com.turtlepick.agent.core.http.JsonCodecSupport.readNullableStringField;
import static com.turtlepick.agent.core.http.JsonCodecSupport.readStringArrayField;
import static com.turtlepick.agent.core.http.JsonCodecSupport.skipWhitespace;
import static com.turtlepick.agent.core.http.JsonCodecSupport.skipWhitespaceAndComma;

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
        List<RepositoryMethodDto> repositoryMethods = readRepositoryMethods(json, start, end);

        return new MetaResponse(status, reason, agentId, commitHash, methods, endpoints, repositoryMethods);
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

    private List<RepositoryMethodDto> readRepositoryMethods(String json, int startIndex, int endIndex) {
        int valueStart = findFieldValueStart(json, "repositoryMethods", startIndex, endIndex);
        if (valueStart < 0) {
            return new ArrayList<RepositoryMethodDto>();
        }
        if (json.charAt(valueStart) != '[') {
            throw new IllegalArgumentException("repositoryMethods field is not an array");
        }

        int arrayEnd = findMatchingBracket(json, valueStart, endIndex, '[', ']');
        List<RepositoryMethodDto> result = new ArrayList<RepositoryMethodDto>();

        int index = valueStart + 1;
        while (index < arrayEnd) {
            index = skipWhitespaceAndComma(json, index, arrayEnd);
            if (index >= arrayEnd) {
                break;
            }
            if (json.charAt(index) != '{') {
                throw new IllegalArgumentException("repositoryMethods array must contain objects");
            }

            int objectEnd = findMatchingBracket(json, index, arrayEnd, '{', '}');
            result.add(parseRepositoryMethodObject(json, index, objectEnd + 1));
            index = objectEnd + 1;
        }

        return result;
    }

    private RepositoryMethodDto parseRepositoryMethodObject(String json, int startIndex, int endIndex) {
        Integer methodId = readIntField(json, "methodId", startIndex, endIndex);
        String owner = readNullableStringField(json, "owner", startIndex, endIndex);
        String methodName = readNullableStringField(json, "methodName", startIndex, endIndex);
        List<String> params = readStringArrayField(json, "params", startIndex, endIndex);
        List<String> runtimeParams = readStringArrayField(json, "runtimeParams", startIndex, endIndex);
        String returnType = readNullableStringField(json, "returnType", startIndex, endIndex);
        String fqcnMethod = readNullableStringField(json, "fqcnMethod", startIndex, endIndex);

        if (methodId == null) {
            throw new IllegalArgumentException("repositoryMethods.methodId is required");
        }
        if (owner == null || owner.trim().length() == 0) {
            throw new IllegalArgumentException("repositoryMethods.owner is required");
        }
        if (methodName == null || methodName.trim().length() == 0) {
            throw new IllegalArgumentException("repositoryMethods.methodName is required");
        }
        if (findFieldValueStart(json, "params", startIndex, endIndex) < 0) {
            throw new IllegalArgumentException("repositoryMethods.params field is missing");
        }
        if (findFieldValueStart(json, "runtimeParams", startIndex, endIndex) < 0) {
            throw new IllegalArgumentException("repositoryMethods.runtimeParams field is missing");
        }
        if (returnType == null || returnType.trim().length() == 0) {
            throw new IllegalArgumentException("repositoryMethods.returnType is required");
        }
        if (fqcnMethod == null || fqcnMethod.trim().length() == 0) {
            throw new IllegalArgumentException("repositoryMethods.fqcnMethod is required");
        }

        return new RepositoryMethodDto(
                methodId.intValue(),
                owner,
                methodName,
                params,
                runtimeParams,
                returnType,
                fqcnMethod
        );
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
}
