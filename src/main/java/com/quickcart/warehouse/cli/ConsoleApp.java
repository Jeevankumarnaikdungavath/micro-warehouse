package com.quickcart.warehouse.cli;

import com.quickcart.warehouse.exception.WarehouseException;
import com.quickcart.warehouse.model.Order;
import com.quickcart.warehouse.model.OrderItem;
import com.quickcart.warehouse.model.OrderLine;
import com.quickcart.warehouse.model.OrderResult;
import com.quickcart.warehouse.model.Product;
import com.quickcart.warehouse.model.TopMover;
import com.quickcart.warehouse.service.InventoryService;
import com.quickcart.warehouse.service.OrderService;
import com.quickcart.warehouse.service.ReportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Interactive console UI. Contains no SQL and no business rules - it only talks to the services. */
@Component
public class ConsoleApp implements CommandLineRunner {

    private final InventoryService inventory;
    private final OrderService orders;
    private final ReportService reports;
    private Scanner in;

    public ConsoleApp(InventoryService inventory, OrderService orders, ReportService reports) {
        this.inventory = inventory;
        this.orders = orders;
        this.reports = reports;
    }

    @Override
    public void run(String... args) {
        in = new Scanner(System.in);
        System.out.println("\n=== Micro-Warehouse Inventory & Fulfillment Hub ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option");
            if (choice == null) {
                break; // end of input
            }
            try {
                switch (choice.trim()) {
                    case "1" -> registerProduct();
                    case "2" -> replenish();
                    case "3" -> listCatalog();
                    case "4" -> placeOrder();
                    case "5" -> viewOrder();
                    case "6" -> dispatchOrder();
                    case "7" -> restockAlerts();
                    case "8" -> salesReport();
                    case "0" -> running = false;
                    default -> System.out.println("Unknown option, please pick from the menu.");
                }
            } catch (WarehouseException e) {
                System.out.println("\n[!] " + e.getMessage());
            } catch (InputAbortedException e) {
                break;
            } catch (RuntimeException e) {
                System.out.println("\n[!] Unexpected error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMenu() {
        System.out.println("""

                ---------------- MENU ----------------
                 1. Register new SKU
                 2. Replenish stock
                 3. View catalog / inventory
                 4. Place multi-item order
                 5. View order details
                 6. Dispatch a confirmed order
                 7. Restock alerts (low stock)
                 8. Sales revenue & top movers
                 0. Exit
                --------------------------------------""");
    }

    // ---------- FR-1 ----------
    private void registerProduct() {
        System.out.println("\n-- Register new SKU --");
        String name = ask("Name");
        String category = ask("Category " + InventoryService.CATEGORIES);
        BigDecimal price = askDecimal("Unit price");
        int stock = askInt("Initial stock");
        Product p = inventory.registerProduct(name, category, price, stock);
        System.out.printf("Registered SKU %d: %s (%s) @ %s, stock %d%n",
                p.getSkuId(), p.getName(), p.getCategory(), p.getUnitPrice(), p.getStockQty());
    }

    private void replenish() {
        System.out.println("\n-- Replenish stock --");
        int sku = askInt("SKU id");
        int qty = askInt("Quantity to add");
        String priceText = ask("New unit price (blank to keep current)", true);
        BigDecimal price = priceText.isBlank() ? null : parseDecimal(priceText);
        Product p = inventory.replenish(sku, qty, price);
        System.out.printf("%s now has %d units at %s%n", p.getName(), p.getStockQty(), p.getUnitPrice());
    }

    private void listCatalog() {
        List<Product> products = inventory.listCatalog();
        System.out.println();
        printProducts(products);
    }

    // ---------- FR-2 / FR-3 ----------
    private void placeOrder() {
        System.out.println("\n-- Place order --");
        String phone = ask("Customer phone");
        List<OrderLine> lines = new ArrayList<>();
        System.out.println("Enter items as '<skuId> <quantity>'. Leave blank to finish.");
        while (true) {
            String line = ask("Item", true);
            if (line.isBlank()) {
                break;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 2) {
                System.out.println("  Please enter exactly two numbers, e.g. '3 2'.");
                continue;
            }
            try {
                lines.add(new OrderLine(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
            } catch (NumberFormatException e) {
                System.out.println("  Both values must be whole numbers.");
            }
        }
        OrderResult result = orders.placeOrder(phone, lines);
        if (result.isConfirmed()) {
            System.out.printf("%nOrder #%d CONFIRMED. Total: %s%n", result.orderId(), result.total());
            showOrderItems(result.orderId());
        } else {
            System.out.printf("%nOrder #%d REJECTED - no stock was deducted:%n", result.orderId());
            result.rejectionReasons().forEach(r -> System.out.println("  - " + r));
        }
    }

    private void viewOrder() {
        int id = askInt("Order id");
        Order o = orders.getOrder(id);
        System.out.printf("%nOrder #%d | %s | %s | %s%n", o.getOrderId(), o.getCustomerPhone(), o.getStatus(), o.getCreatedAt());
        showOrderItems(id);
    }

    private void showOrderItems(int orderId) {
        List<OrderItem> items = orders.getItems(orderId);
        if (items.isEmpty()) {
            System.out.println("  (no line items)");
            return;
        }
        System.out.printf("  %-6s %-32s %5s %10s%n", "SKU", "Product", "Qty", "Subtotal");
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            System.out.printf("  %-6d %-32s %5d %10s%n", i.getSkuId(), i.getProductName(), i.getQuantity(), i.getSubtotal());
            total = total.add(i.getSubtotal());
        }
        System.out.printf("  %-39s %5s %10s%n", "", "Total", total);
    }

    private void dispatchOrder() {
        int id = askInt("Order id to dispatch");
        orders.dispatch(id);
        System.out.println("Order " + id + " marked DISPATCHED.");
    }

    // ---------- FR-4 ----------
    private void restockAlerts() {
        String text = ask("Minimum threshold (blank = " + inventory.getDefaultThreshold() + ")", true);
        int threshold = text.isBlank() ? inventory.getDefaultThreshold() : parseInt(text);
        List<Product> low = inventory.restockAlerts(threshold);
        System.out.println();
        if (low.isEmpty()) {
            System.out.println("All SKUs have at least " + threshold + " units. No restock needed.");
        } else {
            System.out.println("RESTOCK ALERT: " + low.size() + " SKU(s) below " + threshold + " units");
            printProducts(low);
        }
    }

    // ---------- FR-5 ----------
    private void salesReport() {
        int limit = 5;
        String text = ask("How many top movers? (blank = 5)", true);
        if (!text.isBlank()) {
            limit = parseInt(text);
        }
        System.out.printf("%nFulfilled orders : %d%nTotal revenue    : %s%n",
                reports.fulfilledOrders(), reports.totalRevenue());
        List<TopMover> movers = reports.topMovers(limit);
        if (movers.isEmpty()) {
            System.out.println("No fulfilled sales yet.");
            return;
        }
        System.out.printf("%nTop %d movers%n  %-4s %-6s %-32s %-10s %8s %10s%n", movers.size(), "#", "SKU", "Product", "Category", "Units", "Revenue");
        int rank = 1;
        for (TopMover m : movers) {
            System.out.printf("  %-4d %-6d %-32s %-10s %8d %10s%n", rank++, m.skuId(), m.name(), m.category(), m.unitsSold(), m.revenue());
        }
    }

    // ---------- helpers ----------
    private void printProducts(List<Product> products) {
        System.out.printf("%-6s %-32s %-10s %10s %7s%n", "SKU", "Name", "Category", "Price", "Stock");
        for (Product p : products) {
            System.out.printf("%-6d %-32s %-10s %10s %7d%n", p.getSkuId(), p.getName(), p.getCategory(), p.getUnitPrice(), p.getStockQty());
        }
    }

    /** Thrown when stdin closes mid-prompt so the app exits cleanly instead of crashing. */
    private static class InputAbortedException extends RuntimeException {
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return in.hasNextLine() ? in.nextLine() : null;
    }

    private String ask(String label) {
        return ask(label, false);
    }

    private String ask(String label, boolean allowBlank) {
        String value = prompt(label);
        if (value == null) {
            throw new InputAbortedException();
        }
        if (!allowBlank && value.isBlank()) {
            throw new WarehouseException(label + " is required.");
        }
        return value.trim();
    }

    private int askInt(String label) {
        return parseInt(ask(label));
    }

    private BigDecimal askDecimal(String label) {
        return parseDecimal(ask(label));
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new WarehouseException("'" + s + "' is not a valid whole number.");
        }
    }

    private BigDecimal parseDecimal(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            throw new WarehouseException("'" + s + "' is not a valid amount.");
        }
    }
}
