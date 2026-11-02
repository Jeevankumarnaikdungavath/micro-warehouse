package com.quickcart.warehouse.service;

import com.quickcart.warehouse.dao.OrderDao;
import com.quickcart.warehouse.dao.OrderItemDao;
import com.quickcart.warehouse.dao.ProductDao;
import com.quickcart.warehouse.exception.WarehouseException;
import com.quickcart.warehouse.model.OrderLine;
import com.quickcart.warehouse.model.OrderResult;
import com.quickcart.warehouse.model.OrderStatus;
import com.quickcart.warehouse.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderDao orderDao;
    private OrderItemDao itemDao;
    private ProductDao productDao;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderDao = mock(OrderDao.class);
        itemDao = mock(OrderItemDao.class);
        productDao = mock(ProductDao.class);
        service = new OrderService(orderDao, itemDao, productDao);
        when(orderDao.insert(any(), eq(OrderStatus.PENDING))).thenReturn(42);
        when(productDao.findByIdsForUpdate(anyCollection())).thenReturn(List.of(
                new Product(1, "Milk", "Dairy", new BigDecimal("50.00"), 10),
                new Product(2, "Chips", "Snacks", new BigDecimal("20.00"), 2)));
    }

    @Test
    void confirmsOrderAndDeductsStockWhenEverythingIsAvailable() {
        when(productDao.deductStockBatch(anyList())).thenReturn(new int[]{1, 1});

        OrderResult r = service.placeOrder("9876543210",
                List.of(new OrderLine(1, 3), new OrderLine(2, 2), new OrderLine(1, 1)));

        assertEquals(OrderStatus.CONFIRMED, r.status());
        assertEquals(new BigDecimal("240.00"), r.total()); // Milk 4*50 + Chips 2*20
        verify(itemDao).batchInsert(eq(42), anyList());
        verify(orderDao).updateStatus(42, OrderStatus.CONFIRMED);
    }

    @Test
    void rejectsWholeOrderWithoutDeductingAnythingIfOneSkuIsShort() {
        OrderResult r = service.placeOrder("9876543210",
                List.of(new OrderLine(1, 1), new OrderLine(2, 5)));

        assertEquals(OrderStatus.REJECTED, r.status());
        assertEquals(1, r.rejectionReasons().size());
        assertTrue(r.rejectionReasons().get(0).contains("Chips"));
        verify(productDao, never()).deductStockBatch(anyList());
        verify(itemDao, never()).batchInsert(anyInt(), anyList());
        verify(orderDao).updateStatus(42, OrderStatus.REJECTED);
    }

    @Test
    void rejectsUnknownSku() {
        OrderResult r = service.placeOrder("9876543210", List.of(new OrderLine(99, 1)));
        assertEquals(OrderStatus.REJECTED, r.status());
        verify(productDao, never()).deductStockBatch(anyList());
    }

    @Test
    void abortsIfGuardedDeductionMatchesNoRow() {
        when(productDao.deductStockBatch(anyList())).thenReturn(new int[]{1, 0});
        assertThrows(WarehouseException.class,
                () -> service.placeOrder("9876543210", List.of(new OrderLine(1, 1), new OrderLine(2, 1))));
        verify(orderDao, never()).updateStatus(42, OrderStatus.CONFIRMED);
    }

    @Test
    void validatesInput() {
        assertThrows(WarehouseException.class, () -> service.placeOrder("abc", List.of(new OrderLine(1, 1))));
        assertThrows(WarehouseException.class, () -> service.placeOrder("9876543210", List.of()));
        assertThrows(WarehouseException.class, () -> service.placeOrder("9876543210", List.of(new OrderLine(1, 0))));
    }
}
