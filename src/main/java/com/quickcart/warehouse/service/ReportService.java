package com.quickcart.warehouse.service;

import com.quickcart.warehouse.dao.ReportDao;
import com.quickcart.warehouse.exception.WarehouseException;
import com.quickcart.warehouse.model.TopMover;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** FR-5: revenue and top-mover audit. */
@Service
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public BigDecimal totalRevenue() {
        try {
            return reportDao.totalRevenue();
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not compute revenue: " + InventoryService.rootMessage(e), e);
        }
    }

    public long fulfilledOrders() {
        try {
            return reportDao.fulfilledOrderCount();
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not count orders: " + InventoryService.rootMessage(e), e);
        }
    }

    public List<TopMover> topMovers(int limit) {
        if (limit <= 0) {
            throw new WarehouseException("Limit must be greater than zero.");
        }
        try {
            return reportDao.topMovers(limit);
        } catch (DataAccessException e) {
            throw new WarehouseException("Could not compute top movers: " + InventoryService.rootMessage(e), e);
        }
    }
}
