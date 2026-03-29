package com.turtlepick.agent.core.state;

import com.turtlepick.agent.core.http.EndpointInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class EndpointRegistry {

    private final AtomicReference<Map<Integer, List<EndpointInfo>>> mappings =
            new AtomicReference<Map<Integer, List<EndpointInfo>>>(Collections.<Integer, List<EndpointInfo>>emptyMap());

    public void replaceAll(List<EndpointInfo> endpoints) {
        HashMap<Integer, List<EndpointInfo>> next = new HashMap<Integer, List<EndpointInfo>>();
        HashSet<Integer> endpointIds = new HashSet<Integer>();

        if (endpoints != null) {
            for (EndpointInfo endpoint : endpoints) {
                if (endpoint == null) {
                    throw new IllegalArgumentException("endpoint must not be null");
                }
                if (endpoint.getEndpointId() <= 0) {
                    throw new IllegalArgumentException("endpointId must be positive: " + endpoint.getEndpointId());
                }
                if (endpoint.getEntryMethodId() <= 0) {
                    throw new IllegalArgumentException("entryMethodId must be positive: " + endpoint.getEntryMethodId());
                }
                if (trimToNull(endpoint.getEntryType()) == null) {
                    throw new IllegalArgumentException("entryType must not be blank");
                }
                if (trimToNull(endpoint.getEntryKey()) == null) {
                    throw new IllegalArgumentException("entryKey must not be blank");
                }
                if (!endpointIds.add(Integer.valueOf(endpoint.getEndpointId()))) {
                    throw new IllegalArgumentException("duplicate endpointId detected: " + endpoint.getEndpointId());
                }

                Integer key = Integer.valueOf(endpoint.getEntryMethodId());
                List<EndpointInfo> bucket = next.get(key);
                if (bucket == null) {
                    bucket = new ArrayList<EndpointInfo>();
                    next.put(key, bucket);
                }
                bucket.add(endpoint);
            }
        }

        for (Map.Entry<Integer, List<EndpointInfo>> entry : next.entrySet()) {
            entry.setValue(Collections.unmodifiableList(new ArrayList<EndpointInfo>(entry.getValue())));
        }

        mappings.set(Collections.unmodifiableMap(next));
    }

    public void clear() {
        mappings.set(Collections.<Integer, List<EndpointInfo>>emptyMap());
    }

    public List<EndpointInfo> findByEntryMethodId(int entryMethodId) {
        List<EndpointInfo> endpoints = mappings.get().get(Integer.valueOf(entryMethodId));
        return endpoints == null ? Collections.<EndpointInfo>emptyList() : endpoints;
    }

    public int size() {
        int count = 0;
        for (List<EndpointInfo> endpoints : mappings.get().values()) {
            count += endpoints.size();
        }
        return count;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
