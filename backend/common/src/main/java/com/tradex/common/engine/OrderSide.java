package com.tradex.common.engine;

public enum OrderSide {
    BUY,
    SELL;

    public static OrderSide fromString(String val) {
        if (val == null) return null;
        return OrderSide.valueOf(val.trim().toUpperCase());
    }
}
