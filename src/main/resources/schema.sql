-- Micro-Warehouse schema (MySQL 8.x). Idempotent: safe to run on every startup.

CREATE TABLE IF NOT EXISTS products (
    sku_id      INT            NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)   NOT NULL,
    category    VARCHAR(50)    NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL,
    stock_qty   INT            NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (sku_id),
    CONSTRAINT uq_products_name UNIQUE (name),
    CONSTRAINT chk_products_price CHECK (unit_price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock_qty >= 0),
    CONSTRAINT chk_products_category CHECK (category IN ('Dairy', 'Produce', 'Snacks', 'Beverages', 'Household'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    order_id        INT          NOT NULL AUTO_INCREMENT,
    customer_phone  VARCHAR(15)  NOT NULL,
    order_status    VARCHAR(30)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT chk_orders_status CHECK (order_status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'DISPATCHED'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
    item_id   INT            NOT NULL AUTO_INCREMENT,
    order_id  INT            NOT NULL,
    sku_id    INT            NOT NULL,
    quantity  INT            NOT NULL,
    subtotal  DECIMAL(10,2)  NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (item_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT fk_items_product FOREIGN KEY (sku_id) REFERENCES products (sku_id),
    CONSTRAINT chk_items_qty CHECK (quantity > 0),
    CONSTRAINT chk_items_subtotal CHECK (subtotal >= 0),
    INDEX idx_items_order (order_id),
    INDEX idx_items_sku (sku_id)
) ENGINE = InnoDB;
