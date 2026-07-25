package com.turtlepick.agent.core.sql;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;

final class SqlBindValue {

    private final int index;
    private final String setter;
    private final String valueClassName;
    private final String valueText;
    private final Integer sqlType;
    private final boolean nullValue;

    private SqlBindValue(
            int index,
            String setter,
            String valueClassName,
            String valueText,
            Integer sqlType,
            boolean nullValue) {
        this.index = index;
        this.setter = setter;
        this.valueClassName = valueClassName;
        this.valueText = valueText;
        this.sqlType = sqlType;
        this.nullValue = nullValue;
    }

    static SqlBindValue of(int index, String setter, Object value, Integer sqlType, int maxValueLength) {
        if (value == null) {
            return new SqlBindValue(index, setter, null, null, sqlType, true);
        }
        return new SqlBindValue(
                index,
                setter,
                value.getClass().getName(),
                summarize(value, maxValueLength),
                sqlType,
                false
        );
    }

    int getIndex() {
        return index;
    }

    String getSetter() {
        return setter;
    }

    String getValueClassName() {
        return valueClassName;
    }

    String getValueText() {
        return valueText;
    }

    Integer getSqlType() {
        return sqlType;
    }

    boolean isNullValue() {
        return nullValue;
    }

    private static String summarize(Object value, int maxValueLength) {
        if (value instanceof byte[]) {
            return "<byte[" + ((byte[]) value).length + "]>";
        }
        if (value instanceof char[]) {
            return "<char[" + ((char[]) value).length + "]>";
        }
        if (value instanceof InputStream || value instanceof Reader
                || value instanceof Blob || value instanceof Clob) {
            return "<" + value.getClass().getName() + ">";
        }
        String text = String.valueOf(value);
        int limit = maxValueLength > 0 ? maxValueLength : 512;
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit) + "...";
    }
}
