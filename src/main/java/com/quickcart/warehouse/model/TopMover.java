package com.quickcart.warehouse.model;

import java.math.BigDecimal;

public record TopMover(int skuId, String name, String category, long unitsSold, BigDecimal revenue) {
}
