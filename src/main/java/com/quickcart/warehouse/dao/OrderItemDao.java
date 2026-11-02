package com.quickcart.warehouse.dao;

import com.quickcart.warehouse.model.OrderItem;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class OrderItemDao {

    private static final RowMapper<OrderItem> ITEM_MAPPER = (rs, rowNum) -> {
        OrderItem item = new OrderItem();
        item.setItemId(rs.getInt("item_id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setSkuId(rs.getInt("sku_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        item.setProductName(rs.getString("name"));
        return item;
    };

    private final JdbcTemplate jdbc;

    public OrderItemDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** FR-2: inserts every line item of an order in one JDBC batch (single round-trip). */
    public int[] batchInsert(int orderId, List<OrderItem> items) {
        return jdbc.batchUpdate(
                "INSERT INTO order_items (order_id, sku_id, quantity, subtotal) VALUES (?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        OrderItem item = items.get(i);
                        ps.setInt(1, orderId);
                        ps.setInt(2, item.getSkuId());
                        ps.setInt(3, item.getQuantity());
                        ps.setBigDecimal(4, item.getSubtotal());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });
    }

    public List<OrderItem> findByOrderId(int orderId) {
        return jdbc.query(
                "SELECT oi.item_id, oi.order_id, oi.sku_id, oi.quantity, oi.subtotal, p.name "
                        + "FROM order_items oi JOIN products p ON p.sku_id = oi.sku_id "
                        + "WHERE oi.order_id = ? ORDER BY oi.item_id", ITEM_MAPPER, orderId);
    }
}
