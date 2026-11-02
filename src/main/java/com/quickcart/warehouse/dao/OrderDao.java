package com.quickcart.warehouse.dao;

import com.quickcart.warehouse.model.Order;
import com.quickcart.warehouse.model.OrderStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDao {

    private static final RowMapper<Order> ORDER_MAPPER = (rs, rowNum) -> new Order(
            rs.getInt("order_id"),
            rs.getString("customer_phone"),
            OrderStatus.valueOf(rs.getString("order_status")),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public OrderDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(String customerPhone, OrderStatus status) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (customer_phone, order_status) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, customerPhone);
            ps.setString(2, status.name());
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("No generated key returned for order insert");
        }
        return key.intValue();
    }

    public int updateStatus(int orderId, OrderStatus status) {
        return jdbc.update("UPDATE orders SET order_status = ? WHERE order_id = ?", status.name(), orderId);
    }

    /** Conditional transition, e.g. CONFIRMED -> DISPATCHED. Returns 0 if the order is not in the expected state. */
    public int updateStatus(int orderId, OrderStatus from, OrderStatus to) {
        return jdbc.update("UPDATE orders SET order_status = ? WHERE order_id = ? AND order_status = ?",
                to.name(), orderId, from.name());
    }

    public Optional<Order> findById(int orderId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT order_id, customer_phone, order_status, created_at FROM orders WHERE order_id = ?",
                    ORDER_MAPPER, orderId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Order> findRecent(int limit) {
        return jdbc.query(
                "SELECT order_id, customer_phone, order_status, created_at FROM orders ORDER BY order_id DESC LIMIT ?",
                ORDER_MAPPER, limit);
    }
}
