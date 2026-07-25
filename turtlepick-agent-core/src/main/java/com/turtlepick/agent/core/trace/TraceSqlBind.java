package com.turtlepick.agent.core.trace;

public final class TraceSqlBind {

    private final int index;
    private final String setter;
    private final String valueClassName;
    private final String valueText;
    private final Integer sqlType;
    private final boolean nullValue;

    public TraceSqlBind(
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

    public int getIndex() {
        return index;
    }

    public String getSetter() {
        return setter;
    }

    public String getValueClassName() {
        return valueClassName;
    }

    public String getValueText() {
        return valueText;
    }

    public Integer getSqlType() {
        return sqlType;
    }

    public boolean isNullValue() {
        return nullValue;
    }
}
