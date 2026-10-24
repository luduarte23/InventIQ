-- Datos de demostración PROVISIONALES de la Parte B, para probar la venta y el inventario.
-- C4 arma el conjunto completo (80–100 productos e historial de ventas) en migraciones
-- posteriores; puede conservar estos productos o desactivarlos con un UPDATE.
-- Las fechas son relativas al día en que se crea la base, así la demo no "envejece".

INSERT INTO supplier (name, whatsapp, lead_time_days, min_order_type, min_order_value) VALUES
    ('Lácteos del Valle', '+573001112233', 2, 'AMOUNT', 300000),
    ('Distribuidora La 14', '+573004445566', 5, 'UNITS', 48);

INSERT INTO product (barcode, name, sale_price, cost, perishable, supplier_id, safety_stock) VALUES
    ('7702001000012', 'Yogur de fresa 200 g',   3200,  2100, TRUE,  1, 4),
    ('7702001000029', 'Leche entera 1 L',       4500,  3300, TRUE,  1, 10),
    ('7702001000036', 'Queso campesino 500 g', 12500,  9000, TRUE,  1, 3),
    ('7702001000043', 'Kumis 1 L',              5200,  3800, TRUE,  1, 4),
    ('7703002000018', 'Arroz 1 kg',             4800,  3600, FALSE, 2, 12),
    ('7703002000025', 'Aceite de girasol 1 L', 11900,  8900, FALSE, 2, 6),
    ('7703002000032', 'Panela 500 g',           3000,  2000, FALSE, 2, 8),
    ('7703002000049', 'Café molido 250 g',      9800,  7000, FALSE, 2, 5),
    ('7704003000014', 'Pan tajado',             6500,  4700, TRUE,  NULL, 3);

-- Yogur: el caso de US-03. Dos lotes de 3 unidades; se vende primero el que vence antes.
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 3, 3, DATEADD('DAY', 3, CURRENT_DATE),  DATEADD('DAY', -10, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7702001000012';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 3, 3, DATEADD('DAY', 25, CURRENT_DATE), DATEADD('DAY', -2, CURRENT_TIMESTAMP)  FROM product WHERE barcode = '7702001000012';

-- Leche: un lote vencido (no se vende) y dos vigentes.
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 2, DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', -15, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7702001000029';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 8, DATEADD('DAY', 4, CURRENT_DATE),  DATEADD('DAY', -6, CURRENT_TIMESTAMP)  FROM product WHERE barcode = '7702001000029';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 24, 24, DATEADD('DAY', 12, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7702001000029';

INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 6, 5, DATEADD('DAY', 20, CURRENT_DATE), DATEADD('DAY', -4, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7702001000036';

-- Kumis: sin stock, para ver el error de stock insuficiente.

-- No perecederos: sin fecha de vencimiento.
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 30, 18, NULL, DATEADD('DAY', -20, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000018';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 30, 30, NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP)  FROM product WHERE barcode = '7703002000018';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 7, NULL, DATEADD('DAY', -9, CURRENT_TIMESTAMP)  FROM product WHERE barcode = '7703002000025';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 20, 15, NULL, DATEADD('DAY', -7, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000032';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 10, 4, NULL, DATEADD('DAY', -12, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000049';

-- Pan tajado: sin proveedor asignado (caso "por revisar" de US-01 para C).
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 10, 6, DATEADD('DAY', 2, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7704003000014';
