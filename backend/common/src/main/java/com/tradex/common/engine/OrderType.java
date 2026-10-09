package com.tradex.common.engine;

public enum OrderType {
    MARKET,
    LIMIT,
    STOP_LOSS;

    public static OrderType fromString(String val) {
        if (val == null) return MARKET;
        try {
            return OrderType.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MARKET;
        }
    }
}
