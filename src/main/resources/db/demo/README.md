# Datos de demostración (tarea C4)

Migraciones Flyway que solo se aplican con el perfil `demo`
(`./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`), sobre la base `./data/inventiq-demo`.

Hoy solo existe `V20261023_1100__datos_demo_pos.sql`: 2 proveedores, 9 productos y sus lotes para
probar la venta FIFO y el inventario. Meta de C4: 1 admin, 1 cajero, 4–5 proveedores, 80–100
productos, lotes por vencer y 60–90 días de ventas simuladas.

Reglas:
- Nombra los archivos con fecha y hora: `V20261024_1200__datos_demo.sql`.
- Usa códigos de barras EAN-13 con su dígito de verificación correcto.
- Pon las fechas relativas al día en que se crea la base (`DATEADD('DAY', n, CURRENT_DATE)`), así
  la demo no envejece.
