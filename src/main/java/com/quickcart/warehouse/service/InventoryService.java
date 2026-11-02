package com.quickcart.warehouse.service;

import com.quickcart.warehouse.dao.ProductDao;
import com.quickcart.warehouse.exception.WarehouseException;
import com.quickcart.warehouse.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/** FR-1 (catalog + replenishment) and FR-4 (restock alerts). */
@Service
public class InventoryService {

    public static final Set<String> CATEGORIES = Set.of("Dairy", "Produce", "Snacks", "Beverages", "Household");

    private final ProductDao productDao;
    private final int defaultThreshold;

    public InventoryService(ProductDao productDao,
                            @Value("${warehouse.restock.default-threshold:10}") int defaultThreshold) {
        this.productDao = productDao;
        this.defaultThreshold = defaultThreshold;
    }

    public int getDefaultThreshold() {
        return defaultThreshold;
    }

    @Transactional
    public Product registerProduct(String name, String category, BigDecimal unitPrice, int initialStock) {
        if (name == null || name.isBlank() || name.length() > 150) {
            throw new WarehouseException("Product name is required (max 150 characters).");
        }
        String normalizedCategory = CATEGORIES.stream()
                .filter(c -> c.equalsIgnoreCase(category == null ? "" : category.trim()))
                .findFirst()
                .orElseThrow(() -> new WarehouseException("Category must be one of " + CATEGORIES + "."));
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new WarehouseException("Unit price must be zero or positive.");
        }
        if (initialStock < 0) {
            throw new WarehouseException("Initial stock cannot be negative.");
        }
        Product product = new Product(null, name.trim(), normalizedCategory,
                unitPrice.setScale(2, RoundingMode.HALF_UP), initialStock);
        try {
            product.setSkuId(productDao.insert(product));
            return product;
        } catch (DuplicateKeyException e) {
            throw new WarehouseException("A product named '" + name.trim() + "' already exists.", e);
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not register product: " + rootMessage(e), e);
        }
    }

    @Transactional
    public Product replenish(int skuId, int quantity, BigDecimal newPrice) {
        if (quantity <= 0) {
            throw new WarehouseException("Replenishment quantity must be greater than zero.");
        }
        if (newPrice != null && newPrice.signum() < 0) {
            throw new WarehouseException("Unit price must be zero or positive.");
        }
        try {
            if (productDao.addStock(skuId, quantity) == 0) {
                throw new WarehouseException("No product with SKU " + skuId + ".");
            }
            if (newPrice != null) {
                productDao.updatePrice(skuId, newPrice.setScale(2, RoundingMode.HALF_UP));
            }
            return productDao.findById(skuId)
                    .orElseThrow(() -> new WarehouseException("No product with SKU " + skuId + "."));
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not replenish stock: " + rootMessage(e), e);
        }
    }

    public List<Product> listCatalog() {
        try {
            return productDao.findAll();
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not load catalog: " + rootMessage(e), e);
        }
    }

    /** FR-4: all products whose stock is strictly below the threshold. */
    public List<Product> restockAlerts(int threshold) {
        if (threshold < 0) {
            throw new WarehouseException("Threshold cannot be negative.");
        }
        try {
            return productDao.findBelowThreshold(threshold);
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not load restock alerts: " + rootMessage(e), e);
        }
    }

    static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
