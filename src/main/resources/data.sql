-- Seed data. Idempotent: products use INSERT IGNORE (unique name), sample orders only load into an empty orders table.
-- Includes low-stock (< 10) and out-of-stock SKUs so restock alerts and rejections can be demoed.

INSERT IGNORE INTO products (name, category, unit_price, stock_qty) VALUES
('Amul Taza Milk 1L',           'Dairy',     56.00, 120),
('Amul Butter 100g',            'Dairy',     58.00,  45),
('Mother Dairy Curd 400g',      'Dairy',     40.00,   8),
('Britannia Cheese Slices 200g','Dairy',    140.00,  25),
('Farm Eggs (Tray of 6)',       'Dairy',     55.00,   6),
('Banana Robusta 1kg',          'Produce',   60.00,  80),
('Tomato Hybrid 500g',          'Produce',   25.00,  60),
('Onion 1kg',                   'Produce',   42.00, 150),
('Potato 1kg',                  'Produce',   38.00, 140),
('Fresh Coriander Bunch',       'Produce',   15.00,   5),
('Lay''s Classic Salted 52g',   'Snacks',    20.00, 200),
('Kurkure Masala Munch 90g',    'Snacks',    20.00, 180),
('Oreo Biscuits 120g',          'Snacks',    35.00,  90),
('Haldiram Aloo Bhujia 200g',   'Snacks',    60.00,  70),
('Dairy Milk Silk 60g',         'Snacks',    90.00,   9),
('Coca-Cola 750ml',             'Beverages', 40.00, 100),
('Real Fruit Juice Mango 1L',   'Beverages', 110.00, 35),
('Bisleri Water 1L',            'Beverages', 20.00, 300),
('Red Bull Energy 250ml',       'Beverages', 125.00,  7),
('Tata Tea Gold 250g',          'Beverages', 145.00, 40),
('Vim Dishwash Liquid 500ml',   'Household', 105.00, 30),
('Surf Excel Matic 1kg',        'Household', 235.00, 18),
('Harpic Toilet Cleaner 500ml', 'Household', 99.00,   0),
('Scotch-Brite Scrub Pad 3pk',  'Household', 55.00,  22),
('Colin Glass Cleaner 500ml',   'Household', 98.00,   4);

-- Historical sample orders (only if the orders table is empty)
INSERT INTO orders (order_id, customer_phone, order_status, created_at)
SELECT t.* FROM (
    SELECT 1 AS order_id, '9876543210' AS customer_phone, 'DISPATCHED' AS order_status, TIMESTAMP('2026-10-30 09:15:00') AS created_at
    UNION ALL SELECT 2, '9123456780', 'CONFIRMED',  TIMESTAMP('2026-10-31 18:40:00')
    UNION ALL SELECT 3, '9988776655', 'REJECTED',   TIMESTAMP('2026-11-01 07:05:00')
    UNION ALL SELECT 4, '9876543210', 'DISPATCHED', TIMESTAMP('2026-11-01 20:30:00')
) t
WHERE NOT EXISTS (SELECT 1 FROM orders);

INSERT INTO order_items (order_id, sku_id, quantity, subtotal)
SELECT t.order_id, p.sku_id, t.qty, t.qty * p.unit_price FROM (
    SELECT 1 AS order_id, 'Amul Taza Milk 1L' AS pname, 4 AS qty
    UNION ALL SELECT 1, 'Banana Robusta 1kg', 2
    UNION ALL SELECT 1, 'Onion 1kg', 3
    UNION ALL SELECT 2, 'Lay''s Classic Salted 52g', 6
    UNION ALL SELECT 2, 'Coca-Cola 750ml', 3
    UNION ALL SELECT 3, 'Harpic Toilet Cleaner 500ml', 2
    UNION ALL SELECT 4, 'Amul Taza Milk 1L', 6
    UNION ALL SELECT 4, 'Bisleri Water 1L', 10
    UNION ALL SELECT 4, 'Oreo Biscuits 120g', 5
) t
JOIN products p ON p.name = t.pname
WHERE NOT EXISTS (SELECT 1 FROM order_items);
