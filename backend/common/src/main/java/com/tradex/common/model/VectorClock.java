package com.tradex.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VectorClock implements Serializable {

    private final Map<String, Long> clockMap;

    public VectorClock() {
        this.clockMap = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public VectorClock(@JsonProperty("clockMap") Map<String, Long> clockMap) {
        this.clockMap = new ConcurrentHashMap<>(clockMap != null ? clockMap : Collections.emptyMap());
    }

    public void increment(String nodeId) {
        clockMap.compute(nodeId, (k, v) -> (v == null ? 1L : v + 1L));
    }

    public void update(String nodeId, long value) {
        clockMap.put(nodeId, value);
    }

    public void merge(VectorClock other) {
        if (other == null || other.clockMap == null) return;
        for (Map.Entry<String, Long> entry : other.clockMap.entrySet()) {
            clockMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    public Long getClock(String nodeId) {
        return clockMap.getOrDefault(nodeId, 0L);
    }

    public Map<String, Long> getClockMap() {
        return Collections.unmodifiableMap(clockMap);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder("{");
        List<String> keys = new ArrayList<>(clockMap.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            sb.append("\"").append(k).append("\":").append(clockMap.get(k));
            if (i < keys.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static VectorClock deserialize(String json) {
        VectorClock vc = new VectorClock();
        if (json == null || json.trim().isEmpty() || !json.startsWith("{") || !json.endsWith("}")) {
            return vc;
        }
        try {
            String inner = json.substring(1, json.length() - 1);
            if (inner.trim().isEmpty()) return vc;
            String[] pairs = inner.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    long v = Long.parseLong(kv[1].trim());
                    vc.update(k, v);
                }
            }
        } catch (Exception ignored) {}
        return vc;
    }

    public enum CausalOrder {
        EQUAL,
        BEFORE,      // this -> other (this happened before other)
        AFTER,       // other -> this (this happened after other)
        CONCURRENT   // this || other (concurrent / independent)
    }

    /**
     * Compare this vector clock with another according to vector clock causality rules:
     * - V1 <= V2 iff for all k, V1[k] <= V2[k]
     * - V1 < V2 (BEFORE) iff V1 <= V2 and exists k such that V1[k] < V2[k]
     * - V1 > V2 (AFTER) iff V2 < V1
     * - V1 == V2 (EQUAL) iff for all k, V1[k] == V2[k]
     * - Otherwise V1 || V2 (CONCURRENT)
     */
    public CausalOrder compareCausality(VectorClock other) {
        if (other == null) return CausalOrder.CONCURRENT;

        Set<String> allKeys = new HashSet<>(this.clockMap.keySet());
        allKeys.addAll(other.clockMap.keySet());

        boolean hasLess = false;
        boolean hasGreater = false;

        for (String k : allKeys) {
            long v1 = this.getClock(k);
            long v2 = other.getClock(k);

            if (v1 < v2) {
                hasLess = true;
            } else if (v1 > v2) {
                hasGreater = true;
            }
        }

        if (!hasLess && !hasGreater) {
            return CausalOrder.EQUAL;
        } else if (hasLess && !hasGreater) {
            return CausalOrder.BEFORE;
        } else if (!hasLess && hasGreater) {
            return CausalOrder.AFTER;
        } else {
            return CausalOrder.CONCURRENT;
        }
    }

    public boolean happenedBefore(VectorClock other) {
        return compareCausality(other) == CausalOrder.BEFORE;
    }

    public boolean isConcurrentWith(VectorClock other) {
        return compareCausality(other) == CausalOrder.CONCURRENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorClock that = (VectorClock) o;
        return compareCausality(that) == CausalOrder.EQUAL;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clockMap);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
