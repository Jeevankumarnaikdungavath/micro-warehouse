package com.quickcart.warehouse.dao;

import com.quickcart.warehouse.model.Product;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductDao {

    private static final String COLS = "sku_id, name, category, unit_price, stock_qty";

    /** Custom RowMapper: ResultSet row -> Product. */
    private static final RowMapper<Product> PRODUCT_MAPPER = (rs, rowNum) -> new Product(
            rs.getInt("sku_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBigDecimal("unit_price"),
            rs.getInt("stock_qty"));

    private final JdbcTemplate jdbc;

    public ProductDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Product p) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO products (name, category, unit_price, stock_qty) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setBigDecimal(3, p.getUnitPrice());
            ps.setInt(4, p.getStockQty());
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("No generated key returned for product insert");
        }
        return key.intValue();
    }

    public Optional<Product> findById(int skuId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT " + COLS + " FROM products WHERE sku_id = ?", PRODUCT_MAPPER, skuId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Product> findAll() {
        return jdbc.query("SELECT " + COLS + " FROM products ORDER BY category, name", PRODUCT_MAPPER);
    }

    /**
     * Reads the given SKUs and takes row locks (SELECT ... FOR UPDATE) so concurrent orders
     * cannot oversell. Ordered by sku_id so concurrent transactions lock in the same order
     * (prevents deadlocks). Must be called inside a transaction.
     */
    public List<Product> findByIdsForUpdate(Collection<Integer> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", skuIds.stream().map(id -> "?").toList());
        return jdbc.query(
                "SELECT " + COLS + " FROM products WHERE sku_id IN (" + placeholders + ") ORDER BY sku_id FOR UPDATE",
                PRODUCT_MAPPER, skuIds.toArray());
    }

    /** @return rows updated (0 if SKU does not exist) */
    public int addStock(int skuId, int quantity) {
        return jdbc.update("UPDATE products SET stock_qty = stock_qty + ? WHERE sku_id = ?", quantity, skuId);
    }

    public int updatePrice(int skuId, java.math.BigDecimal price) {
        return jdbc.update("UPDATE products SET unit_price = ? WHERE sku_id = ?", price, skuId);
    }

    /**
     * Batch deduction. The guard "stock_qty >= ?" means a row is only updated when stock is sufficient;
     * the returned array lets the caller detect any line that did not update.
     *
     * @param lines each entry: {quantity, skuId, quantity}
     */
    public int[] deductStockBatch(List<Object[]> lines) {
        return jdbc.batchUpdate(
                "UPDATE products SET stock_qty = stock_qty - ? WHERE sku_id = ? AND stock_qty >= ?", lines);
    }

    /** FR-4: products strictly below the threshold, lowest stock first. */
    public List<Product> findBelowThreshold(int threshold) {
        return jdbc.query("SELECT " + COLS + " FROM products WHERE stock_qty < ? ORDER BY stock_qty, name",
                PRODUCT_MAPPER, threshold);
    }
}
