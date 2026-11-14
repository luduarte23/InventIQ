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

-- 2. Usuarios de la demo: admin / admin123 y cajero / cajero123.
--    Se crean aquí (y no solo con InitialUsers) porque Flyway corre antes de que arranque la
--    aplicación y las ventas de ejemplo del bloque 4 necesitan que el cajero ya exista.
--    El valor es el hash BCrypt de la contraseña, nunca la contraseña en texto.
--    Cada vez que este script se vuelve a aplicar, las contraseñas de la demo vuelven a estas.
MERGE INTO users (username, password_hash, role, active) KEY (username)
    VALUES ('admin', '$2a$10$rEcS19Xtu477raqhtirMG.jSb2q/a2Zil1Oh5068ZfLLIfEFG0c9e', 'ADMIN', TRUE);
MERGE INTO users (username, password_hash, role, active) KEY (username)
    VALUES ('cajero', '$2a$10$wNW7ETwcnOrARwdG4idmWuagoTBrp/3UjEeEU54GKS6yXS3gH/ehO', 'CASHIER', TRUE);

-- 3. Configuración de la demo (la fila la crea V2 con presupuesto 0)
UPDATE settings SET weekly_budget = 500000.00 WHERE id = 1;

-- 4. Datos de C4: proveedores, productos, lotes y ventas pasadas.
--    (C escribe aquí sus INSERT, siguiendo las reglas de arriba)

