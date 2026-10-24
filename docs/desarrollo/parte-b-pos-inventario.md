# Parte B: POS e inventario

Venta por código de barras con deducción FIFO (FR-02, US-03) y vista de inventario por lotes.
Sirve de base para la sección B10 de la Entrega 2.

## Tareas

| ID | Tarea | Estado | Dónde |
|---|---|---|---|
| B1 | Fakes y pruebas de US-03 primero | Hecho | `test/.../fakes/`, `SalesServiceTest` |
| B2 | Regla pura `FifoAllocator` | Hecho | `domain/rules/` |
| B3 | Repositorios de Sale y SaleItem | Hecho | `infrastructure/persistence/` |
| B4 | `SalesService` (iniciar, escanear, cerrar) | Hecho | `application/sales/` |
| B5 | Vista de inventario | Hecho (sin rol hasta A7) | `web/inventory/`, `templates/inventory/` |
| B6 | Pantalla de ventas solo con teclado | Hecho | `web/pos/`, `templates/pos/`, `scanner-focus.js` |
| B7 | Concurrencia: reintento y prueba con 2 hilos | Hecho | `SalesService.close`, `SalesConcurrencyTest` |
| B8 | Integración con H2 y aceptación de US-03 | Hecho | `SalesPersistenceTest`, `SalesAcceptanceTest` |
| B9 | Correcciones tras la integración | Pendiente | — |
| B10 | Sección del documento | Pendiente (faltan capturas) | — |

## La regla FIFO

El código de barras identifica al **producto**, no al lote. Al vender, `FifoAllocator` decide de qué
lotes salen las unidades:

1. Solo cuentan los lotes con unidades y **no vencidos**. Un lote que vence hoy todavía se vende.
2. Primero el que **vence antes**; a igual vencimiento, el **recibido antes**. Los lotes sin
   vencimiento van al final.
3. Si la cantidad no cabe en un lote, sigue con el siguiente: queda **una línea de venta por lote**.
4. Si no alcanzan las unidades, falla con `InsufficientStockException` sin asignar nada a medias.

Ejemplo (US-03): el yogur tiene 3 unidades que vencen el 08/10 y 3 el 30/10. Al escanearlo dos
veces salen 2 del lote del 08/10, que queda en 1; el del 30/10 sigue con 3.

## Diseño

```
web/pos/PosController (carrito en la sesión)     web/inventory/InventoryController
        │                                                  │
application/sales/SalesService                   application/sales/InventoryQueryService
        │  FifoAllocator (domain/rules) · puertos (domain/port) · TransactionRunner
infrastructure/persistence: adaptadores JPA y SpringTransactionRunner
```

- **El carrito no toca el stock.** Al escanear solo se valida que haya stock vendible. El stock se
  descuenta al cerrar la venta, así una venta abandonada no aparta unidades.
- **El precio se fija al escanear** y es el que se cobra.
- **Concurrencia.** `Batch` tiene `@Version`. Si dos cajeros cierran a la vez sobre el mismo lote,
  el segundo recibe `ConcurrentStockUpdateException` y `SalesService` reintenta hasta 3 veces con el
  stock actualizado. Además, la base tiene `CHECK (quantity_available >= 0)`.
- **Errores que no cortan la venta.** Las excepciones de negocio heredan de `BusinessException` con
  un mensaje para el cajero. El controlador lo muestra y vuelve a `/pos` sin perder lo escaneado.
- **Atajos de teclado.** Enter agrega, F2 cierra la venta y Esc limpia el campo. Un clic en una zona
  vacía devuelve el foco al campo del código.
