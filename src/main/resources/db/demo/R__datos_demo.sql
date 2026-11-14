-- Datos de demostración (C4). Solo se cargan con el perfil "demo".
--
-- Es una migración REPETIBLE (R__): Flyway la vuelve a ejecutar cada vez que este archivo
-- cambia, siempre después de todas las migraciones versionadas (V1, V2, ...).
-- Por eso empieza borrando los datos demo: así se puede ejecutar varias veces sin duplicar nada.
--
-- Reglas para escribir los datos:
--   * No poner ids a mano: usar subconsultas, por ejemplo
--       (SELECT id FROM products WHERE barcode = '7702001000012')
--     Un id escrito a mano no mueve el contador de H2, y el siguiente INSERT de la app chocaría.
--   * Fechas relativas a hoy (DATEADD('DAY', 3, CURRENT_DATE)), para que los vencimientos y el
--     historial de ventas tengan sentido el día que se haga la demo.
--   * Nunca copiar este archivo a db/migration: ahí se cargaría también en producción.

-- 1. Limpiar los datos demo (hijos antes que padres, por las llaves foráneas)
DELETE FROM sale_items;
DELETE FROM sales;
DELETE FROM purchase_order_lines;
DELETE FROM batches;
DELETE FROM purchase_orders;
DELETE FROM products;
DELETE FROM suppliers;
DELETE FROM alerts;
DELETE FROM audit_logs;

-- 2. Cajero temporal: la pantalla de ventas registra las ventas a nombre del usuario 1
--    mientras no exista el login. Se reemplaza cuando A7 cree los usuarios reales.
MERGE INTO users (username, password_hash, role, active) KEY (username)
VALUES ('cajero', 'pendiente-login', 'CASHIER', TRUE);

-- 3. Configuración de la demo (la fila la crea V2 con presupuesto 0)
UPDATE settings SET weekly_budget = 500000.00 WHERE id = 1;

-- 4. Proveedores. Los números de WhatsApp son ficticios.
INSERT INTO suppliers (name, whatsapp_number, lead_time_days, min_order_amount,
                       min_order_units, active) VALUES
    ('Alpina', '+573005550001', 1, 150000.00, NULL, TRUE),
    ('Comercial Nutresa', '+573005550002', 3, 300000.00, NULL, TRUE),
    ('Postobón', '+573005550003', 2, NULL, 48, TRUE),
    ('Bavaria', '+573005550004', 2, 200000.00, NULL, TRUE),
    ('Mayorista Abarrotes La 13', '+573005550005', 4, 400000.00, NULL, TRUE);

-- 5. Productos de una tienda de barrio en Bogotá. El código es interno de la tienda, de 4 dígitos:
--    el primero es el proveedor (1 Alpina, 2 Nutresa, 3 Postobón, 4 Bavaria, 5 mayorista,
--    9 sin proveedor) y los otros tres, el número del producto.
--    Precios en pesos, de referencia para noviembre de 2026; el costo es el 78 % del precio.
--    Los tres últimos no tienen proveedor: el motor los deja en un pedido "por revisar".
INSERT INTO products (barcode, name, sale_price, unit_cost, perishable, supplier_id, safety_stock)
SELECT v.barcode, v.name, v.sale_price, v.unit_cost, v.perishable, s.id, v.safety_stock
FROM (VALUES
    ('1001', 'Leche entera Alpina 1 L', 7500, 5850, TRUE, 'Alpina', 6),
    ('1002', 'Leche deslactosada descremada Alpina 1 L', 8200, 6400, TRUE, 'Alpina', 4),
    ('1003', 'Bon Yurt Zucaritas Alpina 170 g', 4200, 3300, TRUE, 'Alpina', 4),
    ('1004', 'Bon Yurt Choco Alpina 170 g', 4200, 3300, TRUE, 'Alpina', 3),
    ('1005', 'Yogur griego Alpina 150 g', 4500, 3500, TRUE, 'Alpina', 2),
    ('1006', 'Quesito Alpina 185 g', 7200, 5600, TRUE, 'Alpina', 2),
    ('1007', 'Mantequilla con sal Alpina 250 g', 10500, 8200, TRUE, 'Alpina', 2),
    ('1008', 'Avena Alpina 250 g', 3500, 2750, TRUE, 'Alpina', 4),
    ('2001', 'Galletas Saltín Noel tradicional 320 g', 6500, 5050, FALSE, 'Comercial Nutresa', 3),
    ('2002', 'Galletas Tosh cremadas vainilla 24 g', 1300, 1000, FALSE, 'Comercial Nutresa', 6),
    ('2003', 'Galletas Tosh salvado y miel 27 g', 1300, 1000, FALSE, 'Comercial Nutresa', 4),
    ('2004', 'Galletas Festival recreo 33 g', 1100, 850, FALSE, 'Comercial Nutresa', 8),
    ('2005', 'Wafer Noel 24 g', 1100, 850, FALSE, 'Comercial Nutresa', 6),
    ('2006', 'Chocolatina Jet 12 g', 1500, 1150, FALSE, 'Comercial Nutresa', 10),
    ('2007', 'Chocolate de mesa Corona tradicional 250 g', 9900, 7700, FALSE, 'Comercial Nutresa', 2),
    ('2008', 'Chocolisto 1 kg', 23900, 18650, FALSE, 'Comercial Nutresa', 1),
    ('2009', 'Café Colcafé clásico 170 g', 17500, 13650, FALSE, 'Comercial Nutresa', 1),
    ('2010', 'Café Sello Rojo 250 g', 12500, 9750, FALSE, 'Comercial Nutresa', 3),
    ('2011', 'Spaghetti Doria 500 g', 4700, 3650, FALSE, 'Comercial Nutresa', 3),
    ('2012', 'Spaghetti Doria familiar 1 kg', 8600, 6700, FALSE, 'Comercial Nutresa', 1),
    ('2013', 'Salchichas Viena Zenú 150 g', 5200, 4050, TRUE, 'Comercial Nutresa', 3),
    ('2014', 'Salchicha Zenú 240 g', 8300, 6450, TRUE, 'Comercial Nutresa', 2),
    ('2015', 'Atún en agua Zenú 160 g', 6800, 5300, FALSE, 'Comercial Nutresa', 2),
    ('2016', 'Salchicha Ranchera premium 480 g', 15200, 11850, TRUE, 'Comercial Nutresa', 1),
    ('2017', 'Salchicha de pechuga Pietrán 350 g', 13500, 10550, TRUE, 'Comercial Nutresa', 1),
    ('3001', 'Agua Cristal 600 ml', 2000, 1550, FALSE, 'Postobón', 6),
    ('3002', 'Agua Cristal 1 L', 2800, 2200, FALSE, 'Postobón', 3),
    ('3003', 'Agua Cristal con gas 600 ml', 2300, 1800, FALSE, 'Postobón', 2),
    ('3004', 'Bretaña 300 ml', 1900, 1500, FALSE, 'Postobón', 3),
    ('3005', 'H2Oh! limón 250 ml', 1700, 1350, FALSE, 'Postobón', 3),
    ('3006', 'Speed Max 250 ml', 2600, 2050, FALSE, 'Postobón', 4),
    ('3007', 'Jugo Hit naranja 1 L', 5000, 3900, FALSE, 'Postobón', 2),
    ('3008', 'Gatorade frutos tropicales 500 ml', 4700, 3650, FALSE, 'Postobón', 2),
    ('3009', 'Gatorade mandarina 500 ml', 4700, 3650, FALSE, 'Postobón', 2),
    ('4001', 'Pony Malta 330 ml', 2600, 2050, FALSE, 'Bavaria', 8),
    ('4002', 'Pony Malta 1,5 L', 6800, 5300, FALSE, 'Bavaria', 2),
    ('4003', 'Pony Malta mini 200 ml', 1800, 1400, FALSE, 'Bavaria', 6),
    ('4004', 'Cerveza Club Colombia dorada 330 ml', 4000, 3100, FALSE, 'Bavaria', 6),
    ('4005', 'Cerveza Póker 330 ml', 3400, 2650, FALSE, 'Bavaria', 12),
    ('4006', 'Cerveza Azteca 330 ml', 2700, 2100, FALSE, 'Bavaria', 6),
    ('4007', 'Cerveza BBC Monserrate roja 330 ml', 6900, 5400, FALSE, 'Bavaria', 1),
    ('5001', 'Arroz Diana 500 g', 2700, 2100, FALSE, 'Mayorista Abarrotes La 13', 6),
    ('5002', 'Arroz Diana 1 kg', 5200, 4050, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5003', 'Arroz Roa 500 g', 2700, 2100, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5004', 'Lentejas Diana 500 g', 4600, 3600, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5005', 'Harina P.A.N. maíz blanco 1 kg', 6200, 4850, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5006', 'Harina Doñarepa maíz blanco 1 kg', 5600, 4350, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5007', 'Harina de trigo Haz de Oros 500 g', 3300, 2550, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5008', 'Maizena 90 g', 2700, 2100, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5009', 'Spaghetti La Muñeca 250 g', 2200, 1700, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5010', 'Azúcar Providencia 454 g', 2600, 2050, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5011', 'Azúcar Incauca 1 kg', 5400, 4200, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5012', 'Sal Refisal 1 kg', 2500, 1950, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5013', 'Sal Refisal 500 g', 1500, 1150, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5014', 'Aceite Premier 1 L', 14500, 11300, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5015', 'Aceite Z mezcla vegetal 500 ml', 7200, 5600, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5016', 'Atún Van Camp''s en agua 160 g', 7200, 5600, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5017', 'Atún Van Camp''s en aceite 160 g', 7200, 5600, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5018', 'Salsa de tomate Fruco 400 g', 6500, 5050, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5019', 'Mayonesa Fruco 380 g', 9300, 7250, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5020', 'Salsa de tomate La Constancia 400 g', 5900, 4600, FALSE, 'Mayorista Abarrotes La 13', 1),
    ('5021', 'Café Águila Roja 125 g', 6500, 5050, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5022', 'Nescafé fina selección 85 g', 15500, 12100, FALSE, 'Mayorista Abarrotes La 13', 1),
    ('5023', 'Panelada Quala 29 g', 700, 550, FALSE, 'Mayorista Abarrotes La 13', 8),
    ('5024', 'Gelatina Frutiño fresa 35 g', 1600, 1250, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5025', 'Bon Bon Bum 19 g', 500, 400, FALSE, 'Mayorista Abarrotes La 13', 20),
    ('5026', 'Nucita wafer avellana 20 g', 1000, 800, FALSE, 'Mayorista Abarrotes La 13', 6),
    ('5027', 'Galleta avena y granola Colombina 30 g', 1300, 1000, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5028', 'Chocoramo tajada 65 g', 3100, 2400, TRUE, 'Mayorista Abarrotes La 13', 6),
    ('5029', 'Chocoramo mini 20 g', 1300, 1000, TRUE, 'Mayorista Abarrotes La 13', 6),
    ('5030', 'Tostacos Ramo 30 g', 2100, 1650, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5031', 'Papas Margarita limón 30 g', 2300, 1800, FALSE, 'Mayorista Abarrotes La 13', 6),
    ('5032', 'Papas Margarita natural 30 g', 2300, 1800, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5033', 'Papas Margarita pollo 30 g', 2300, 1800, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5034', 'Doritos 34 g', 2600, 2050, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5035', 'Choclitos limón 28 g', 2100, 1650, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5036', 'Yupis 25 g', 1600, 1250, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('5037', 'Galletas Oreo 54 g', 2500, 1950, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5038', 'Coca-Cola 400 ml', 3200, 2500, FALSE, 'Mayorista Abarrotes La 13', 10),
    ('5039', 'Coca-Cola 1,5 L', 6900, 5400, FALSE, 'Mayorista Abarrotes La 13', 4),
    ('5040', 'Coca-Cola sin azúcar 600 ml', 4000, 3100, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5041', 'Agua Brisa con gas 600 ml', 2300, 1800, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5042', 'Pan tajado Vital Bimbo 200 g', 4200, 3300, TRUE, 'Mayorista Abarrotes La 13', 3),
    ('5043', 'Pan blanco extra largo Comapan 520 g', 7200, 5600, TRUE, 'Mayorista Abarrotes La 13', 4),
    ('5044', 'Queso crema Colanta 230 g', 8200, 6400, TRUE, 'Mayorista Abarrotes La 13', 2),
    ('5045', 'Queso campesino Colanta 250 g', 9200, 7200, TRUE, 'Mayorista Abarrotes La 13', 3),
    ('5046', 'Yagur Colanta 200 g', 2700, 2100, TRUE, 'Mayorista Abarrotes La 13', 4),
    ('5047', 'Leche deslactosada Alquería 1 L', 7200, 5600, TRUE, 'Mayorista Abarrotes La 13', 4),
    ('5048', 'Salchicha viena Blony 95 g', 2900, 2250, TRUE, 'Mayorista Abarrotes La 13', 3),
    ('5049', 'Crema dental Colgate Max White 75 ml', 10500, 8200, FALSE, 'Mayorista Abarrotes La 13', 2),
    ('5050', 'Shampoo Savital 550 ml', 14500, 11300, FALSE, 'Mayorista Abarrotes La 13', 1),
    ('5051', 'Papel higiénico Familia x12 rollos', 33900, 26450, FALSE, 'Mayorista Abarrotes La 13', 1),
    ('5052', 'Jabón Rey barra 300 g', 4300, 3350, FALSE, 'Mayorista Abarrotes La 13', 3),
    ('9001', 'Arepas de maíz caseras Sary 500 g', 5500, 4300, TRUE, NULL, 3),
    ('9002', 'Bocadillo de guayaba La Villa 300 g', 5800, 4500, TRUE, NULL, 1),
    ('9003', 'Panela La Gloria 1 kg', 5200, 4050, FALSE, NULL, 3)
) AS v(barcode, name, sale_price, unit_cost, perishable, supplier, safety_stock)
LEFT JOIN suppliers s ON s.name = v.supplier;

-- 6. Inventario actual. Los perecederos tienen un lote viejo que vence en 3 a 6 días
--    y uno nuevo; los demás, un solo lote sin vencimiento. Algunos productos quedan
--    por debajo de su punto de reorden a propósito, para que haya algo que pedir, y dos
--    tienen además un lote ya vencido que la venta no debe tocar.
INSERT INTO batches (product_id, quantity_received, quantity_remaining, expiration_date,
                     received_at, unit_cost)
SELECT p.id, v.quantity, v.quantity, DATEADD('DAY', v.expires_in_days, CURRENT_DATE),
       DATEADD('DAY', -v.received_days_ago, CURRENT_TIMESTAMP), p.unit_cost
FROM (VALUES
    ('1001', 4, 4, 10),
    ('1001', 6, 21, 3),
    ('1002', 12, 5, 10),
    ('1002', 16, 22, 3),
    ('1003', 2, 6, 10),
    ('1003', 2, 23, 3),
    ('1004', 8, 3, 10),
    ('1004', 10, 24, 3),
    ('1005', 5, 4, 10),
    ('1005', 6, 25, 3),
    ('1006', 6, 5, 10),
    ('1006', 7, 26, 3),
    ('1007', 4, 6, 10),
    ('1007', 6, 27, 3),
    ('1008', 2, 3, 10),
    ('1008', 3, 28, 3),
    ('2001', 5, NULL, 7),
    ('2002', 35, NULL, 7),
    ('2003', 21, NULL, 7),
    ('2004', 17, NULL, 7),
    ('2005', 29, NULL, 7),
    ('2006', 26, NULL, 7),
    ('2007', 12, NULL, 7),
    ('2008', 4, NULL, 7),
    ('2009', 9, NULL, 7),
    ('2010', 5, NULL, 7),
    ('2011', 19, NULL, 7),
    ('2012', 4, NULL, 7),
    ('2013', 9, 4, 10),
    ('2013', 13, 21, 3),
    ('2014', 2, 5, 10),
    ('2014', 1, 22, 3),
    ('2015', 14, NULL, 7),
    ('2016', 4, 3, 10),
    ('2016', 5, 24, 3),
    ('2017', 4, 4, 10),
    ('2017', 4, 25, 3),
    ('3001', 45, NULL, 7),
    ('3002', 4, NULL, 7),
    ('3003', 12, NULL, 7),
    ('3004', 18, NULL, 7),
    ('3005', 15, NULL, 7),
    ('3006', 6, NULL, 7),
    ('3007', 13, NULL, 7),
    ('3008', 14, NULL, 7),
    ('3009', 5, NULL, 7),
    ('4001', 14, NULL, 7),
    ('4002', 13, NULL, 7),
    ('4003', 36, NULL, 7),
    ('4004', 36, NULL, 7),
    ('4005', 24, NULL, 7),
    ('4006', 32, NULL, 7),
    ('4007', 4, NULL, 7),
    ('5001', 16, NULL, 7),
    ('5002', 28, NULL, 7),
    ('5003', 26, NULL, 7),
    ('5004', 16, NULL, 7),
    ('5005', 7, NULL, 7),
    ('5006', 15, NULL, 7),
    ('5007', 11, NULL, 7),
    ('5008', 10, NULL, 7),
    ('5009', 26, NULL, 7),
    ('5010', 10, NULL, 7),
    ('5011', 17, NULL, 7),
    ('5012', 13, NULL, 7),
    ('5013', 17, NULL, 7),
    ('5014', 4, NULL, 7),
    ('5015', 22, NULL, 7),
    ('5016', 20, NULL, 7),
    ('5017', 13, NULL, 7),
    ('5018', 13, NULL, 7),
    ('5019', 13, NULL, 7),
    ('5020', 9, NULL, 7),
    ('5021', 22, NULL, 7),
    ('5022', 4, NULL, 7),
    ('5023', 55, NULL, 7),
    ('5024', 16, NULL, 7),
    ('5025', 66, NULL, 7),
    ('5026', 42, NULL, 7),
    ('5027', 24, NULL, 7),
    ('5028', 10, 4, 10),
    ('5028', 14, 29, 3),
    ('5029', 15, 5, 10),
    ('5029', 22, 20, 3),
    ('5030', 28, NULL, 7),
    ('5031', 48, NULL, 7),
    ('5032', 35, NULL, 7),
    ('5033', 32, NULL, 7),
    ('5034', 29, NULL, 7),
    ('5035', 22, NULL, 7),
    ('5036', 20, NULL, 7),
    ('5037', 26, NULL, 7),
    ('5038', 40, NULL, 7),
    ('5039', 35, NULL, 7),
    ('5040', 16, NULL, 7),
    ('5041', 11, NULL, 7),
    ('5042', 8, 6, 10),
    ('5042', 12, 23, 3),
    ('5043', 6, 3, 10),
    ('5043', 8, 24, 3),
    ('5044', 6, 4, 10),
    ('5044', 7, 25, 3),
    ('5045', 9, 5, 10),
    ('5045', 13, 26, 3),
    ('5046', 12, 6, 10),
    ('5046', 16, 27, 3),
    ('5046', 2, -1, 12),
    ('5047', 12, 3, 10),
    ('5047', 17, 28, 3),
    ('5048', 7, 4, 10),
    ('5048', 10, 29, 3),
    ('5049', 11, NULL, 7),
    ('5050', 4, NULL, 7),
    ('5051', 9, NULL, 7),
    ('5052', 16, NULL, 7),
    ('9001', 1, 5, 10),
    ('9001', 2, -1, 12),
    ('9002', 2, 6, 10),
    ('9002', 2, 25, 3),
    ('9003', 1, NULL, 7)
) AS v(barcode, quantity, expires_in_days, received_days_ago)
JOIN products p ON p.barcode = v.barcode;
