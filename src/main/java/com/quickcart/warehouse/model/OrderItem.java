package com.quickcart.warehouse.model;

import java.math.BigDecimal;

public class OrderItem {
    private Integer itemId;
    private Integer orderId;
    private int skuId;
    private int quantity;
    private BigDecimal subtotal;
    /** Joined from products for display only (not a column of order_items). */
    private String productName;

    public OrderItem() {
    }

    public OrderItem(int skuId, int quantity, BigDecimal subtotal) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public int getSkuId() { return skuId; }
    public void setSkuId(int skuId) { this.skuId = skuId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
