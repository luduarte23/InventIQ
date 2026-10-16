-- Datos demo del catálogo ampliado: tipo, marca, contenido y descripción de los productos
-- provisionales, 12 productos más de otras categorías y un producto con la venta pausada.
-- Fechas relativas al día en que se crea la base, para que haya lotes vencidos, por vencer
-- (7 días o menos) y vigentes.

-- Proveedores adicionales.
INSERT INTO supplier (name, whatsapp, lead_time_days, min_order_type, min_order_value) VALUES
    ('Bebidas del Norte', '+573007778899', 3, 'AMOUNT', 250000),
    ('Aseo Hogar', '+573012223344', 7, 'UNITS', 24);

-- Productos que ya existían: el tamaño pasa del nombre a la columna de contenido.
UPDATE product SET name = 'Yogur de fresa', category = 'LACTEOS', brand = 'La Pradera',
    net_content = 200, content_unit = 'G', description = 'Yogur batido con trozos de fresa.'
    WHERE barcode = '7702001000012';
UPDATE product SET name = 'Leche entera', category = 'LACTEOS', brand = 'La Pradera',
    net_content = 1, content_unit = 'L', description = 'Leche entera ultrapasteurizada en bolsa.'
    WHERE barcode = '7702001000029';
UPDATE product SET name = 'Queso campesino', category = 'LACTEOS', brand = 'Campo Verde',
    net_content = 500, content_unit = 'G', description = 'Queso fresco semigraso, ideal para arepas.'
    WHERE barcode = '7702001000036';
UPDATE product SET name = 'Kumis', category = 'LACTEOS', brand = 'La Pradera',
    net_content = 1, content_unit = 'L', description = 'Kumis tradicional endulzado.'
    WHERE barcode = '7702001000043';
UPDATE product SET name = 'Arroz blanco', category = 'GRANOS', brand = 'El Molino',
    net_content = 1, content_unit = 'KG', description = 'Arroz blanco de grano largo.'
    WHERE barcode = '7703002000018';
UPDATE product SET name = 'Aceite de girasol', category = 'ACEITES', brand = 'Dorado',
    net_content = 1, content_unit = 'L', description = 'Aceite 100 % de girasol.'
    WHERE barcode = '7703002000025';
UPDATE product SET name = 'Panela', category = 'ENDULZANTES', brand = 'Trapiche Real',
    net_content = 500, content_unit = 'G', description = 'Panela en bloque, de caña.'
    WHERE barcode = '7703002000032';
UPDATE product SET name = 'Pan tajado', category = 'PANADERIA', brand = 'Panadería San José',
    net_content = 500, content_unit = 'G', description = 'Pan blanco tajado, horneado cada mañana.'
    WHERE barcode = '7704003000014';

-- Café: se compró mucho antes de un alza de precio y todavía no se pone a la venta.
UPDATE product SET name = 'Café molido', category = 'CAFE', brand = 'Altos del Café',
    net_content = 250, content_unit = 'G', sale_enabled = FALSE,
    description = 'Café tostado y molido, tueste medio. Stock comprado antes del alza.'
    WHERE barcode = '7703002000049';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 60, 60, NULL, DATEADD('DAY', -2, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000049';

-- Productos nuevos.
INSERT INTO product (barcode, name, category, brand, net_content, content_unit, description,
                     sale_price, cost, perishable, supplier_id, safety_stock) VALUES
    ('7703002000056', 'Fríjol rojo', 'GRANOS', 'El Molino', 500, 'G', 'Fríjol cargamanto rojo seleccionado.',
        5400, 4000, FALSE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 6),
    ('7703002000063', 'Lenteja', 'GRANOS', 'El Molino', 500, 'G', 'Lenteja verde de cocción rápida.',
        3900, 2800, FALSE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 6),
    ('7703002000070', 'Azúcar blanca', 'ENDULZANTES', 'Dulce Valle', 1, 'KG', 'Azúcar refinada.',
        4600, 3500, FALSE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 8),
    ('7705004000010', 'Gaseosa cola', 'BEBIDAS', 'Burbuja', 1.5, 'L', 'Bebida gaseosa sabor a cola, botella retornable.',
        5500, 3900, TRUE, (SELECT id FROM supplier WHERE name = 'Bebidas del Norte'), 12),
    ('7705004000027', 'Agua sin gas', 'BEBIDAS', 'Manantial', 600, 'ML', 'Agua potable tratada.',
        2000, 1100, TRUE, (SELECT id FROM supplier WHERE name = 'Bebidas del Norte'), 24),
    ('7705004000034', 'Jugo de naranja', 'BEBIDAS', 'Huerta Fresca', 1, 'L', 'Jugo de naranja sin azúcar añadida.',
        6200, 4500, TRUE, (SELECT id FROM supplier WHERE name = 'Bebidas del Norte'), 6),
    ('7706005000016', 'Atún en aceite', 'ENLATADOS', 'Mar Azul', 160, 'G', 'Lomitos de atún en aceite vegetal.',
        7900, 5900, TRUE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 6),
    ('7707006000012', 'Galletas de soda', 'SNACKS', 'Crocante', 6, 'UND', 'Paquete de 6 tacos de galletas de soda.',
        4500, 3100, TRUE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 5),
    ('7707006000029', 'Papas fritas', 'SNACKS', 'Crocante', 110, 'G', 'Papas fritas en hojuelas, sabor natural.',
        4200, 2800, TRUE, (SELECT id FROM supplier WHERE name = 'Distribuidora La 14'), 5),
    ('7708007000018', 'Jabón de baño', 'ASEO', 'Espuma', 3, 'UND', 'Paquete de 3 jabones de tocador.',
        8900, 6200, FALSE, (SELECT id FROM supplier WHERE name = 'Aseo Hogar'), 4),
    ('7708007000025', 'Detergente en polvo', 'ASEO', 'Blanco Total', 1, 'KG', 'Detergente para ropa blanca y de color.',
        12900, 9400, FALSE, (SELECT id FROM supplier WHERE name = 'Aseo Hogar'), 3),
    ('7709008000014', 'Huevos AA', 'CARNES', 'Granja Feliz', 12, 'UND', 'Bandeja de 12 huevos frescos tipo AA.',
        9500, 7200, TRUE, NULL, 10);

-- Lotes vigentes de los productos nuevos, con algunos vencidos o por vencer.
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 30, 25, NULL, DATEADD('DAY', -8, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000056';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 20, 5, NULL, DATEADD('DAY', -15, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000063';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 40, 30, NULL, DATEADD('DAY', -6, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7703002000070';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 24, 24, DATEADD('DAY', 60, CURRENT_DATE), DATEADD('DAY', -3, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7705004000010';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 48, 40, DATEADD('DAY', 200, CURRENT_DATE), DATEADD('DAY', -10, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7705004000027';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 6, DATEADD('DAY', 5, CURRENT_DATE), DATEADD('DAY', -9, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7705004000034';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 12, DATEADD('DAY', 20, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7705004000034';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 24, 20, DATEADD('DAY', 400, CURRENT_DATE), DATEADD('DAY', -20, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7706005000016';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 20, 15, DATEADD('DAY', 90, CURRENT_DATE), DATEADD('DAY', -12, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7707006000012';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 4, DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', -40, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7707006000029';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 10, DATEADD('DAY', 3, CURRENT_DATE), DATEADD('DAY', -25, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7707006000029';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 24, 18, NULL, DATEADD('DAY', -30, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7708007000018';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 12, 9, NULL, DATEADD('DAY', -14, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7708007000025';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 30, 22, DATEADD('DAY', 6, CURRENT_DATE), DATEADD('DAY', -12, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7709008000014';
INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)
SELECT id, 30, 30, DATEADD('DAY', 18, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_TIMESTAMP) FROM product WHERE barcode = '7709008000014';
