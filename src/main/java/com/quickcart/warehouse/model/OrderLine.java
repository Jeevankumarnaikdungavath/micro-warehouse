package com.quickcart.warehouse.model;

/** One requested line of a new order: which SKU and how many. */
public record OrderLine(int skuId, int quantity) {
}
