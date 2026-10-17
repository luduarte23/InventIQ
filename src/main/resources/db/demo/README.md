# Datos de demostración (tarea C4)

Migraciones Flyway que solo se aplican con el perfil `demo`
(`./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`), sobre la base `./data/inventiq-demo`.

| Migración | Contenido |
|---|---|
| `V20261023_1100__datos_demo_pos.sql` | Productos y lotes iniciales (Parte B) |
| `V20261023_1300__catalogo_demo.sql` | Tipo, marca y contenido de esos productos; 12 productos y 2 proveedores más; café con la venta pausada |
| `src/main/java/db/demo/V20261023_1400__HistorialVentasDemo.java` | 90 días de ventas simuladas y seis meses de precios, con semilla fija |

Meta de C4: 1 admin, 1 cajero, 4–5 proveedores, 80–100 productos con código de barras, lotes con
algunos por vencer, productos sin historial y 60–90 días de ventas simuladas. Hoy hay 21 productos;
para llegar a la meta basta con agregar productos y lotes: el generador en Java crea historial para
todos los productos que encuentre.

Reglas:
- Nombra los archivos con fecha y hora, como el resto de migraciones: `V20261024_1200__datos_demo.sql`.
- Los códigos de barras deben ser GTIN válidos (EAN-13 con su dígito de verificación), porque la
  pantalla de productos rechaza los que no lo son. Para calcular el último dígito usa
  `GtinValidator.checkDigit("770200100001")`, que devuelve `2`.
- Las fechas van relativas al día en que se crea la base (`DATEADD('DAY', n, CURRENT_DATE)`), así
  la demo no envejece.
- `DemoDataTest` comprueba que los datos cuadren. Si agregas datos, córrelo.
