# Decisiones pendientes

Lo que el equipo debe revisar antes de fusionar. Según las reglas comunes, cambiar el dominio o los
puertos requiere el acuerdo de los cuatro integrantes.

## Hechas en la Fase 1

1. **Esquema V1 frente a `ACUERDOS.md` §3.** El esquema se dedujo de los documentos porque
   `ACUERDOS.md` no estaba disponible. Hay que compararlo y ajustarlo antes de fusionar la primera
   migración. *Responsable: A.*
2. **Protección de `main` en GitHub.** Exigir un PR con una aprobación y el check `CI / build`.
   *Responsable: A.*

## Hechas para la Parte B (trabajo que era de A)

B depende de A2, A5 y A7, que aún no existían. Para no bloquearla se hizo lo mínimo, marcado
para revisión:

3. **Modelo y puertos (A2).** Se crearon `Product`, `Batch`, `Sale`, `SaleItem`, `Cart` y los
   puertos `ProductRepository`, `BatchRepository` y `SaleRepository`, solo con lo que usa B. Si el
   modelo acordado el 17 de octubre difiere, hay que alinearlo.
   - `Product` es un `record` de solo lectura; D4 agregará la escritura.
   - `Batch` lleva la versión de bloqueo optimista como un `long`. Sigue siendo Java puro, pero así
     el adaptador puede detectar conflictos.
4. **`TransactionRunner`** (en `application`). Es la forma de tener transacciones sin `@Transactional`
   en los servicios. Si se acepta, C y D lo usan igual.
5. **Persistencia de Product y Batch (A5).** `ProductEntity`, `BatchEntity` (con `@Version`), mappers y
   adaptadores, solo con las consultas de B. `findAvailableByProduct` ya viene en orden FIFO.
6. **Usuarios iniciales** (`V20261023_1000__usuarios_iniciales.sql`): `admin/admin123` y
   `cajero/cajero123`, en BCrypt **sin prefijo** `{bcrypt}`. A7 debe usar `BCryptPasswordEncoder`, o
   agregar el prefijo si prefiere el codificador delegante. Están en la migración base (no solo en
   demo) para que cualquiera pueda entrar siguiendo el README. Hay que cambiarlas si la app se
   despliega.
7. **Cajero sin login.** Mientras no exista A7, las ventas quedan a nombre de `cajero`
   (`PosController.DEFAULT_CASHIER`). Al agregar Spring Security se usará el `Principal` sin cambiar
   el controlador. Las consultas nativas sobre `app_user` en `SaleJpaRepository` se pueden
   reemplazar cuando A7 cree la entidad de usuario.
8. **Datos demo provisionales** (`db/demo/V20261023_1100__datos_demo_pos.sql`): 9 productos y sus
   lotes. C4 decide si los conserva y agrega los 80–100 productos y el historial en migraciones
   posteriores.

## Hechas para Administración → Productos (trabajo que era de D4)

9. **Cambios en puertos.** `ProductRepository.create` y `BatchRepository.create` (insertar), y un
    `SupplierRepository` nuevo de solo lectura (`findAll`, `findById`). El modelo `Supplier` es
    mínimo y aún no tiene el pedido mínimo (MOQ, FR-05).
10. **`CatalogService`** quedó en `application/admin`, donde el reparto ubica el trabajo de D, con su
    `AdminConfig`. D4 agrega ahí la edición y la inactivación.
11. **Reglas de validación**, todas en el dominio. Conviene confirmarlas con el usuario entrevistado:
    - Solo códigos GTIN (EAN-8, UPC-A, EAN-13, GTIN-14) con dígito de verificación válido. Es
      coherente con el alcance de la Entrega 1 ("productos con código estándar"). Los productos
      ya registrados con otros códigos se siguen encontrando.
    - Precios en **pesos enteros**.
    - **Precio de venta ≥ costo**. Si la tienda quiere registrar productos "gancho" por debajo del
      costo, esta regla se puede relajar a una advertencia.
12. **Stock inicial al registrar.** Crea un lote sin pedido (`order_line_id` vacío). Hoy no queda
    en la auditoría porque D3 aún no existe; cuando exista, registrarlo como `STOCK_ADJUSTED` (D5).
13. **Códigos demo corregidos** a EAN-13 válidos (migración demo aún no fusionada). C4 debe generar
    sus códigos con `GtinValidator.checkDigit`, como explica `db/demo/README.md`.

## Hechas con la interfaz nueva, el catálogo ampliado y el historial

14. **Más cambios en el dominio y en los puertos**:
    - `Product` ahora tiene tipo, marca, contenido, descripción y `saleEnabled`;
    - `ProductRepository` suma `findAll` y `update`;
    - `SaleRepository` suma `findItemsSoldBetween`, que C puede usar para la velocidad de ventas;
    - nuevo `PriceHistoryRepository`.
15. **Tipos de producto como lista fija** (`ProductCategory`, 13 tipos más "Otros"). Si la tienda
    quiere crear los suyos, hay que pasarlos a una tabla administrable.
16. **Venta pausada**: el producto queda en el inventario, pero no se vende. Hay que confirmar con
    el usuario entrevistado si además se necesita "ocultar" productos del inventario. Eso sería la
    inactivación de D4.
17. **"Por vencer" = 7 días o menos**, fijo en `ExpirationStatus.EXPIRING_SOON_DAYS`. Podría ser un
    parámetro de la tienda (D5).
18. **Generador de historial demo en Java** (`db/demo/V20261023_1400__HistorialVentasDemo.java`).
    Adelanta parte de C4 (historial de ventas para el ROP). C puede ajustar sus parámetros (días,
    ritmo de ventas) en lugar de escribir SQL a mano.
19. **Chart.js como WebJar** (librería de JavaScript servida desde el JAR, no una API externa). No
    cuenta como la "única API externa" que permite el curso, pero conviene mencionarlo en el
    documento de arquitectura.
20. **Ediciones simultáneas de un producto**: hoy gana la última. Si se quiere detectar el conflicto,
    el formulario debe llevar la versión (`@Version` ya existe en la tabla).

## Abiertas

21. **Restringir `/inventory` y `/admin/**` al rol ADMIN** cuando exista el login (A7).
22. **Historial de ventas para C.** Ya existe `SaleRepository.findItemsSoldBetween(desde, hasta)`,
    que devuelve las líneas vendidas con su fecha. `SalesVelocityCalculator` puede partir de ahí
    para la ventana de 30 días; falta que C confirme si le sirve tal cual.
