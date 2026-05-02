package com.turtlepick.agent.core.http;

final class JsonCodecSupport {

    private JsonCodecSupport() {
    }

    static String readNullableStringField(String json, String fieldName, int startIndex, int endIndex) {
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

    static Integer readIntField(String json, String fieldName, int startIndex, int endIndex) {
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

    static int findFieldValueStart(String json, String fieldName, int startIndex, int endIndex) {
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

    static int skipJsonValue(String json, int startIndex, int endIndex) {
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

    static int findMatchingBracket(String json, int startIndex, int endIndex, char open, char close) {
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

    static int findStringEnd(String json, int quoteStartIndex, int endIndex) {
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

    static String readJsonString(String json, int quoteStartIndex, int endIndex) {
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

    static int skipWhitespace(String json, int index, int endIndex) {
        int current = index;
        while (current < endIndex && isWhitespace(json.charAt(current))) {
            current++;
        }
        return current;
    }

    static int skipWhitespaceAndComma(String json, int index, int endIndex) {
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

    static String escapeJson(String value) {
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

    private static boolean startsWith(String json, int index, int endIndex, String token) {
        int tokenLength = token.length();
        if (index + tokenLength > endIndex) {
            return false;
        }
        return token.equals(json.substring(index, index + tokenLength));
    }

    private static boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
    }
}
