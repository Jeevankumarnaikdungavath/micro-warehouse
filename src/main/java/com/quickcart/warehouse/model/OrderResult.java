package com.quickcart.warehouse.model;

import java.math.BigDecimal;
import java.util.List;

/** Outcome of an order placement: CONFIRMED (with total) or REJECTED (with reasons). */
public record OrderResult(int orderId, OrderStatus status, BigDecimal total, List<String> rejectionReasons) {
    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }
}
