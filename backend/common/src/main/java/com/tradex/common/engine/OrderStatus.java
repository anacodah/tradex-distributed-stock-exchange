package com.tradex.common.engine;

import java.util.Set;

public enum OrderStatus {
    NEW,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCEL_PENDING,
    CANCELLED,
    REJECTED,
    EXPIRED,
    EXECUTED; // Kept for backwards compatibility with legacy tests

    /**
     * Checks if transitioning from 'current' to 'next' is logically allowed.
     */
    public static boolean isValidTransition(OrderStatus current, OrderStatus next) {
        if (current == null || next == null) return false;
        if (current == next) return true;

        // Terminal states cannot transition to anything
        if (current == FILLED || current == EXECUTED || current == CANCELLED || current == REJECTED || current == EXPIRED) {
            return false;
        }

        return switch (current) {
            case NEW -> Set.of(OPEN, PARTIALLY_FILLED, FILLED, EXECUTED, CANCELLED, REJECTED, EXPIRED).contains(next);
            case OPEN -> Set.of(PARTIALLY_FILLED, FILLED, EXECUTED, CANCEL_PENDING, CANCELLED, EXPIRED).contains(next);
            case PARTIALLY_FILLED -> Set.of(FILLED, EXECUTED, CANCEL_PENDING, CANCELLED, EXPIRED).contains(next);
            case CANCEL_PENDING -> Set.of(CANCELLED, FILLED, EXECUTED).contains(next);
            default -> false;
        };
    }

    public boolean isTerminal() {
        return this == FILLED || this == EXECUTED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }

    public boolean isEligibleForCancellation() {
        return this == NEW || this == OPEN || this == PARTIALLY_FILLED;
    }

    public boolean isEligibleForModification() {
        return this == NEW || this == OPEN || this == PARTIALLY_FILLED;
    }
}
