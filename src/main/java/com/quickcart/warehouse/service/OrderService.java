package com.quickcart.warehouse.service;

import com.quickcart.warehouse.dao.OrderDao;
import com.quickcart.warehouse.dao.OrderItemDao;
import com.quickcart.warehouse.dao.ProductDao;
import com.quickcart.warehouse.exception.WarehouseException;
import com.quickcart.warehouse.model.Order;
import com.quickcart.warehouse.model.OrderItem;
import com.quickcart.warehouse.model.OrderLine;
import com.quickcart.warehouse.model.OrderResult;
import com.quickcart.warehouse.model.OrderStatus;
import com.quickcart.warehouse.model.Product;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** FR-2 (batch order creation) and FR-3 (atomic inventory deduction). */
@Service
public class OrderService {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,14}$");

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;

    public OrderService(OrderDao orderDao, OrderItemDao orderItemDao, ProductDao productDao) {
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
        this.productDao = productDao;
    }

    /**
     * Places a multi-item order in ONE transaction:
     * <ol>
     *   <li>create order as PENDING</li>
     *   <li>lock the requested product rows (SELECT ... FOR UPDATE) and check stock for every SKU</li>
     *   <li>any shortfall / unknown SKU: mark REJECTED and return, touching no stock</li>
     *   <li>otherwise batch-insert line items, batch-deduct stock, mark CONFIRMED</li>
     * </ol>
     * Any unexpected failure throws and rolls the whole transaction back, so stock is never partially deducted.
     */
    @Transactional
    public OrderResult placeOrder(String customerPhone, List<OrderLine> requested) {
        String phone = customerPhone == null ? "" : customerPhone.trim();
        if (!PHONE.matcher(phone).matches()) {
            throw new WarehouseException("Invalid phone number (expected 10-14 digits, optional leading +).");
        }
        if (requested == null || requested.isEmpty()) {
            throw new WarehouseException("An order needs at least one item.");
        }

        // Merge duplicate SKUs; TreeMap keeps a stable (sorted) order.
        Map<Integer, Integer> qtyBySku = new TreeMap<>();
        for (OrderLine line : requested) {
            if (line.quantity() <= 0) {
                throw new WarehouseException("Quantity for SKU " + line.skuId() + " must be greater than zero.");
            }
            qtyBySku.merge(line.skuId(), line.quantity(), Integer::sum);
        }

        try {
            int orderId = orderDao.insert(phone, OrderStatus.PENDING);

            Map<Integer, Product> locked = new TreeMap<>();
            for (Product p : productDao.findByIdsForUpdate(qtyBySku.keySet())) {
                locked.put(p.getSkuId(), p);
            }

            List<String> problems = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : qtyBySku.entrySet()) {
                Product p = locked.get(e.getKey());
                if (p == null) {
                    problems.add("SKU " + e.getKey() + " does not exist");
                } else if (p.getStockQty() < e.getValue()) {
                    problems.add(p.getName() + " (SKU " + p.getSkuId() + "): requested " + e.getValue()
                            + ", only " + p.getStockQty() + " in stock");
                }
            }

            if (!problems.isEmpty()) {
                orderDao.updateStatus(orderId, OrderStatus.REJECTED);
                return new OrderResult(orderId, OrderStatus.REJECTED, BigDecimal.ZERO, problems);
            }

            List<OrderItem> items = new ArrayList<>();
            List<Object[]> deductions = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> e : qtyBySku.entrySet()) {
                Product p = locked.get(e.getKey());
                int qty = e.getValue();
                BigDecimal subtotal = p.getUnitPrice().multiply(BigDecimal.valueOf(qty));
                total = total.add(subtotal);
                items.add(new OrderItem(p.getSkuId(), qty, subtotal));
                deductions.add(new Object[]{qty, p.getSkuId(), qty});
            }

            orderItemDao.batchInsert(orderId, items);

            int[] updated = productDao.deductStockBatch(deductions);
            for (int count : updated) {
                if (count == 0) {
                    // Defensive: the guarded UPDATE matched nothing -> abort and roll back everything.
                    throw new WarehouseException("Stock changed while packing the order; please retry.");
                }
            }

            orderDao.updateStatus(orderId, OrderStatus.CONFIRMED);
            return new OrderResult(orderId, OrderStatus.CONFIRMED, total, List.of());
        } catch (DataAccessException e) {
            throw new WarehouseException("Order could not be placed (nothing was changed): "
                    + InventoryService.rootMessage(e), e);
        }
    }

    /** Marks a CONFIRMED order as DISPATCHED. */
    @Transactional
    public void dispatch(int orderId) {
        try {
            if (orderDao.updateStatus(orderId, OrderStatus.CONFIRMED, OrderStatus.DISPATCHED) == 0) {
                Order order = orderDao.findById(orderId)
                        .orElseThrow(() -> new WarehouseException("Order " + orderId + " not found."));
                throw new WarehouseException("Order " + orderId + " is " + order.getStatus()
                        + "; only CONFIRMED orders can be dispatched.");
            }
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not dispatch order: " + InventoryService.rootMessage(e), e);
        }
    }

    public Order getOrder(int orderId) {
        try {
            return orderDao.findById(orderId)
                    .orElseThrow(() -> new WarehouseException("Order " + orderId + " not found."));
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not load order: " + InventoryService.rootMessage(e), e);
        }
    }

    public List<OrderItem> getItems(int orderId) {
        try {
            return orderItemDao.findByOrderId(orderId);
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not load order items: " + InventoryService.rootMessage(e), e);
        }
    }

    public List<Order> recentOrders(int limit) {
        try {
            return orderDao.findRecent(limit);
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not load orders: " + InventoryService.rootMessage(e), e);
        }
    }
}
