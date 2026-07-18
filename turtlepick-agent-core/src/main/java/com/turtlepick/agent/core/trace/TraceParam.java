package com.turtlepick.agent.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TraceParam {

    private final String name;
    private final String type;
    private final String value;
    private final List<TraceParamField> fields;

    public TraceParam(String name, String type, String value, List<TraceParamField> fields) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.fields = fields == null
                ? Collections.<TraceParamField>emptyList()
                : Collections.unmodifiableList(new ArrayList<TraceParamField>(fields));
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<TraceParamField> getFields() {
        return fields;
    }
}
