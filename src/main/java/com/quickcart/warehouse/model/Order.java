package com.quickcart.warehouse.model;

import java.time.LocalDateTime;

public class Order {
    private Integer orderId;
    private String customerPhone;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public Order() {
    }

    public Order(Integer orderId, String customerPhone, OrderStatus status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerPhone = customerPhone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
