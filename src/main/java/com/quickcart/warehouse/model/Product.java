package com.quickcart.warehouse.model;

import java.math.BigDecimal;

public class Product {
    private Integer skuId;
    private String name;
    private String category;
    private BigDecimal unitPrice;
    private int stockQty;

    public Product() {
    }

    public Product(Integer skuId, String name, String category, BigDecimal unitPrice, int stockQty) {
        this.skuId = skuId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQty = stockQty;
    }

    public Integer getSkuId() { return skuId; }
    public void setSkuId(Integer skuId) { this.skuId = skuId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }
}
