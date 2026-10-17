# Parte B: POS e inventario

Venta por código de barras con deducción FIFO (FR-02, US-03) y vista de inventario por lotes.
Este documento sirve de base para la sección B10 de la Entrega 2.

## Estado de las tareas

| ID | Tarea | Estado | Dónde |
|---|---|---|---|
| B1 | Repositorios falsos y pruebas de US-03 primero | Hecho | `test/.../fakes/`, `SalesServiceTest` |
| B2 | Regla pura `FifoAllocator` | Hecho | `domain/rules/FifoAllocator.java` |
| B3 | Repositorios de Sale y SaleItem | Hecho | `infrastructure/persistence/` |
| B4 | `SalesService` (iniciar, escanear, cerrar) | Hecho | `application/sales/SalesService.java` |
| B5 | Vista de inventario del admin | Hecho (sin restricción de rol hasta A7) | `web/inventory/`, `templates/inventory/` |
| B6 | Pantalla de ventas para usar solo con teclado | Hecho | `web/pos/`, `templates/pos/`, `static/js/scanner-focus.js` |
| B7 | Concurrencia: reintento y prueba con 2 hilos | Hecho | `SalesService.close`, `SalesConcurrencyTest` |
| B8 | Integración con H2 y aceptación de US-03 | Hecho | `SalesPersistenceTest`, `SalesAcceptanceTest` |
| B9 | Correcciones tras la integración | Pendiente (29–31 oct) | — |
| B10 | Sección del documento | Base lista (este archivo); faltan capturas | — |

Para que B funcionara se creó la parte mínima de A2, A5 y A7 que necesita. Ver
[decisiones-pendientes.md](decisiones-pendientes.md).

## La regla FIFO

El código de barras identifica al **producto**, no al lote. Al vender, `FifoAllocator` decide de qué
lotes salen las unidades:

1. Solo cuentan los lotes con unidades y **no vencidos**. Un lote que vence hoy todavía se vende; uno
   que venció ayer, no.
2. Orden: primero el que **vence antes**. A igual vencimiento, el **recibido antes**. Los lotes sin
   vencimiento (no perecederos) van al final, en orden de recepción.
3. Si una cantidad no cabe en un lote, sigue con el siguiente: queda **una línea de venta por lote**
   tocado.
4. Si las unidades vendibles no alcanzan, falla con `InsufficientStockException` sin asignar nada a
   medias.

Ejemplo (US-03, escenario 1): el yogur tiene 3 unidades que vencen el 08/10 y 3 que vencen el 30/10.
Se escanea dos veces: salen 2 del lote del 08/10, que queda en 1, y el del 30/10 sigue con 3. Si se
vendieran 5, la venta tendría dos líneas: 3 del lote del 08/10 y 2 del lote del 30/10.

## Diseño

```
web/pos/PosController ──────────┐        web/inventory/InventoryController
  (carrito en la sesión HTTP)    │                 │
                                 ▼                 ▼
application/sales/SalesService        application/sales/InventoryQueryService
  │ usa FifoAllocator (domain/rules)     │
  │ usa los puertos ProductRepository, BatchRepository, SaleRepository (domain/port)
  │ usa TransactionRunner (application)
  ▼
infrastructure/persistence: adaptadores JPA, SpringTransactionRunner
```

Decisiones de diseño:

- **El carrito (`Cart`) vive en la sesión y no toca el stock.** El stock se descuenta solo al cerrar
  la venta. Así una venta abandonada no deja unidades apartadas, y la tabla `sale` solo guarda ventas
  terminadas. Al escanear sí se valida que haya stock vendible para lo acumulado en el carrito.
- **El precio se fija al escanear** (`CartLine.unitPrice`) y es el que se cobra.
- **Transacción sin anotaciones.** `SalesService` no usa `@Transactional`: recibe un
  `TransactionRunner` para poder armarse en las pruebas con fakes. En producción lo implementa
  `SpringTransactionRunner` con un `TransactionTemplate`.
- **Concurrencia.** `BatchEntity` tiene `@Version`. Si dos cajeros cierran a la vez sobre el mismo lote,
  el segundo UPDATE no encuentra la versión esperada y la infraestructura lo reporta como
  `ConcurrentStockUpdateException`. `SalesService` reintenta hasta 3 veces, releyendo el stock en cada
  intento. Como segunda barrera, la base tiene `CHECK (quantity_available >= 0)`.
- **Errores que no cortan la venta.** Todas las excepciones de negocio heredan de
  `BusinessException` y su mensaje está pensado para el cajero. El controlador lo muestra y redirige a
  `/pos`, sin perder lo escaneado.
- **Cajero.** Mientras no exista el login (A7), las ventas se registran a nombre del usuario `cajero`.
  Con Spring Security se usará automáticamente el usuario autenticado (`Principal`).

## Secuencia de una venta

```mermaid
sequenceDiagram
    actor Cajero
    participant Lector as Lector USB / teclado
    participant POS as PosController
    participant S as SalesService
    participant F as FifoAllocator
    participant R as Repositorios (JPA)
    participant DB as H2

    Cajero->>Lector: escanea el producto
    Lector->>POS: POST /pos/scan (código + Enter)
    POS->>S: scan(carrito, código, 1)
    S->>R: findByBarcode(código)
    alt código no registrado
        S-->>POS: UnknownBarcodeException
        POS-->>Cajero: mensaje de error; la venta sigue
    else producto activo
        S->>R: findAvailableByProduct(producto)
        S->>F: sellableQuantity(lotes, hoy)
        S-->>POS: carrito con la línea agregada
        POS-->>Cajero: redirect a /pos con el total
    end

    Cajero->>POS: F2 · POST /pos/close
    POS->>S: close(carrito, cajero)
    loop hasta 3 intentos si hay conflicto
        S->>R: [transacción] findAvailableByProduct por cada línea
        S->>F: allocate(lotes, cantidad, hoy)
        F-->>S: asignaciones por lote (FIFO)
        S->>R: saveAll(lotes descontados)
        R->>DB: UPDATE batch ... WHERE version = ?
        S->>R: save(venta con una línea por lote)
        R->>DB: INSERT sale, sale_item
    end
    S-->>POS: venta registrada
    POS-->>Cajero: "Venta registrada: N unidades, total $ X"
```

## Pantallas

- **Ventas (`/pos`)**: el campo del código tiene foco automático. El lector USB escribe el código y
  envía Enter, que agrega el producto. También se puede escribir el código a mano y, si hace falta,
  una cantidad. Atajos: <kbd>F2</kbd> cierra la venta y <kbd>Esc</kbd> limpia el campo. Un clic en una
  zona vacía devuelve el foco al campo. La última línea escaneada se resalta.
- **Inventario (`/inventory`)**: cada producto activo con sus unidades vendibles, las vencidas aparte,
  el punto de reorden (cuando C lo calcule) y los lotes en el orden en que se venderán. Los lotes
  vencidos se ven en rojo. Las filas en o por debajo del punto de reorden se resaltan.

*Pendiente: capturas de pantalla para el documento de la entrega.*

## Reporte de pruebas

`./mvnw verify` corre 73 pruebas en verde (16 de octubre de 2026). Las de la Parte B:

| Clase | Tipo | Pruebas | Qué cubre |
|---|---|---|---|
| `FifoAllocatorTest` | Unitaria pura | 13 | Orden por vencimiento y recepción, lotes vencidos, vacíos, no perecederos, stock insuficiente |
| `BatchTest`, `CartTest`, `SaleTest` | Unitaria pura | 16 | Descuento de lotes, vencimiento, carrito y totales |
| `SalesServiceTest` | Servicio con fakes, sin Spring | 14 | US-03 escenarios 1 y 2, stock insuficiente, venta en varios lotes, reintento ante conflicto |
| `InventoryQueryServiceTest` | Servicio con fakes, sin Spring | 3 | Orden FIFO de la vista, vencidas, punto de reorden |
| `SalesPersistenceTest` | `@DataJpaTest` contra H2 | 6 | Consulta FIFO, guardado con versión, conflicto de versión, venta con líneas |
| `SalesConcurrencyTest` | Integración, 2 hilos | 10 | Dos ventas simultáneas sobre el mismo lote (5 repeticiones por caso) |
| `SalesAcceptanceTest` | Aceptación por HTTP (MockMvc) | 7 | US-03 completo, 10 productos sin mouse, cancelar, cantidad inválida, vista de inventario |

Cobertura del paquete `domain` (JaCoCo): **99,1 % de líneas** (109/110) y **96,9 % de ramas**
(31/32). La capa `application` queda al 100 % de líneas.
