# Micro-Warehouse Inventory & Fulfillment Hub

Spring Boot + Spring JDBC (`JdbcTemplate`) + MySQL 8 console application for a quick-commerce dark store.
Built for *Spring Mini Hackathon 2026 – Problem Statement #3*.

## Tech stack
Java 17 (runs on 21) · Maven · Spring Boot 3.3 (`spring-boot-starter-jdbc`) · MySQL 8.x + `mysql-connector-j` · JUnit 5 / Mockito

## Project structure
```
src/main/java/com/quickcart/warehouse
├── WarehouseApplication.java      Boot entry point
├── config/DatabaseConfig.java     @Configuration: DataSource (Hikari), JdbcTemplate, TransactionManager
├── dao/                           @Repository + RowMappers, parameterized SQL only
│   ├── ProductDao                 insert, find, SELECT..FOR UPDATE, guarded batch stock deduction, low-stock query
│   ├── OrderDao                   order header CRUD + status transitions
│   ├── OrderItemDao               JdbcTemplate.batchUpdate() for line items
│   └── ReportDao                  revenue + top-mover joins (orders x order_items x products)
├── service/                       @Service business rules + @Transactional
│   ├── InventoryService           FR-1, FR-4
│   ├── OrderService               FR-2, FR-3
│   └── ReportService              FR-5
├── model/                         Product, Order, OrderItem, OrderStatus, records
├── exception/WarehouseException   user-facing wrapper for DataAccessException / rule violations
└── cli/ConsoleApp.java            interactive menu (no SQL, no business logic)
src/main/resources
├── application.properties
├── schema.sql                     tables, PK/FK, CHECK + UNIQUE constraints, indexes (idempotent)
└── data.sql                       25 SKUs in 5 categories (incl. low/zero stock) + 4 sample orders (idempotent)
```

## Requirements → implementation
| Req | Where |
|---|---|
| FR-1 register SKU / replenish | `InventoryService.registerProduct / replenish` |
| FR-2 multi-item batch insert | `OrderItemDao.batchInsert` → `jdbcTemplate.batchUpdate` (one batch) |
| FR-3 atomic deduction | `OrderService.placeOrder` – one `@Transactional` unit: locks rows with `SELECT … FOR UPDATE`, validates **all** SKUs first; any shortfall ⇒ order `REJECTED`, **zero** stock touched. Deduction uses guarded `UPDATE … WHERE stock_qty >= ?`; if any row fails the transaction rolls back. |
| FR-4 restock alerts | `ProductDao.findBelowThreshold` (`stock_qty < ?`) |
| FR-5 revenue & top movers | `ReportDao` – revenue counts `CONFIRMED` + `DISPATCHED` orders only |

## How to run
1. Install JDK 17+ and Maven 3.9+, and have MySQL 8.x running.
2. Set credentials (defaults: `root` / `root` on `localhost:3306`):
   ```bash
   export DB_USER=root
   export DB_PASSWORD=your_password
   # optional: export DB_URL="jdbc:mysql://localhost:3306/micro_warehouse?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   ```
   The `micro_warehouse` database is created automatically; `schema.sql` and `data.sql` run on every start and are idempotent.
3. Run:
   ```bash
   mvn spring-boot:run
   # or
   mvn clean package && java -jar target/micro-warehouse-1.0.0.jar
   ```
4. Run unit tests (no DB needed): `mvn test`

To run the SQL manually instead: `mysql -u root -p < src/main/resources/schema.sql` (after `CREATE DATABASE micro_warehouse; USE micro_warehouse;`), then `data.sql`.

## Demo script (suggested)
1. `3` – view catalog (note SKU ids).
2. `7` – restock alerts at threshold 10 → Curd, Eggs, Coriander, Dairy Milk Silk, Red Bull, Colin, Harpic (0 stock).
3. `4` – place order for `9876543210` with `1 2` and `6 3` → **CONFIRMED**, stock deducted.
4. `4` – place order with an in-stock SKU **and** Harpic (out of stock) → **REJECTED**, check with `3` that the in-stock SKU was *not* deducted.
5. `2` – replenish Harpic, `6` – dispatch a confirmed order, `8` – revenue & top movers.
6. Try bad input (letters, negative qty, unknown SKU) – the app reports errors and keeps running.

## Team workflow
See [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) for the 6-member ownership split, branch names and PR checklist.
