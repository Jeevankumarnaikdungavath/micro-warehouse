package com.quickcart.warehouse.dao;

import com.quickcart.warehouse.model.TopMover;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** FR-5 queries. Only CONFIRMED and DISPATCHED orders count as fulfilled revenue. */
@Repository
public class ReportDao {

    private static final RowMapper<TopMover> TOP_MOVER_MAPPER = (rs, rowNum) -> new TopMover(
            rs.getInt("sku_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getLong("units_sold"),
            rs.getBigDecimal("revenue"));

    private final JdbcTemplate jdbc;

    public ReportDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal totalRevenue() {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(oi.subtotal), 0) "
                        + "FROM orders o JOIN order_items oi ON oi.order_id = o.order_id "
                        + "WHERE o.order_status IN ('CONFIRMED', 'DISPATCHED')", BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    public long fulfilledOrderCount() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE order_status IN ('CONFIRMED', 'DISPATCHED')", Long.class);
        return n == null ? 0 : n;
    }

    public List<TopMover> topMovers(int limit) {
        return jdbc.query(
                "SELECT p.sku_id, p.name, p.category, SUM(oi.quantity) AS units_sold, SUM(oi.subtotal) AS revenue "
                        + "FROM orders o "
                        + "JOIN order_items oi ON oi.order_id = o.order_id "
                        + "JOIN products p ON p.sku_id = oi.sku_id "
                        + "WHERE o.order_status IN ('CONFIRMED', 'DISPATCHED') "
                        + "GROUP BY p.sku_id, p.name, p.category "
                        + "ORDER BY units_sold DESC, revenue DESC LIMIT ?",
                TOP_MOVER_MAPPER, limit);
    }
}
