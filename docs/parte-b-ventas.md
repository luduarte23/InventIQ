# Parte B: venta con FIFO (POS)

Venta por código de barras con descuento de inventario FIFO (FR-02, US-03). Cubre las tareas B2, B3, B4 y B6 del reparto.

## Cómo funciona

1. El cajero abre `/pos`, escanea (o escribe) un código y presiona Enter.
2. `SalesServiceImpl.scan` busca el producto y le pide a `FifoAllocator` de qué lotes salen las unidades: primero el que vence antes; a igual fecha, el recibido antes; los que no vencen, al final. Los vencidos no se venden.
3. Cada lote se descuenta en ese momento (US-03: "stock updated instantly"). Queda una línea de venta por lote, con el precio y el descuento de ese lote.
4. **Terminar venta** la cierra. **Cancelar venta** devuelve las unidades a sus lotes y borra la venta.

Los errores de negocio (código inexistente, stock insuficiente, cantidad no válida) se muestran como mensaje y no cortan la venta.

## Archivos por capa (rutas desde `src/main/java/com/inventiq/`)

| Capa | Archivos | Qué hacen |
|---|---|---|
| Dominio | `domain/rules/FifoAllocator`, `BatchAllocation` | La regla FIFO, en Java puro |
| Dominio | `domain/model/exception/InvalidSaleException` | Errores de la venta |
| Negocio | `application/pos/SalesServiceImpl` | Iniciar, escanear, terminar y cancelar (`@Service`, `@Transactional`) |
| Configuración | `infrastructure/config/ClockConfig` | `Clock` de la aplicación (America/Bogota) |
| Persistencia | `infrastructure/persistence/entity/SaleEntity`, `SaleItemEntity`, `jpa/SaleJpaRepository`, `adapter/SaleRepositoryAdapter` | Ventas y sus ítems con JPA |
| Persistencia | `ProductEntity`, `BatchEntity`, `ProductJpaRepository`, `BatchJpaRepository`, `ProductRepositoryAdapter`, `BatchRepositoryAdapter` | Productos y lotes (provisional de A5, ver abajo) |
| Presentación | `web/pos/PosController`, `ScanForm`, `SaleScreen`, `SaleLineView` y `templates/pos/sale.html` | Pantalla de ventas mínima (`@Controller` y DTOs) |

## Cambios en archivos de otras letras (revisar en el PR)

- **A2.** Se agregaron tres métodos para poder cancelar una venta: `Batch.restore(int)`, `SaleRepository.delete(Long)` y `SalesService.cancelSale(Long)`. No se cambió nada de lo que ya existía.
- **A5.** La persistencia de Product y Batch se hizo de forma provisional: sin ella la aplicación no arranca, porque `SalesServiceImpl` necesita esos repositorios. A puede revisarla, quedársela o reemplazarla.
- **C.** `SaleRepository.unitsSoldSince` cuenta solo ventas cerradas.

## Pruebas

`mvnw.cmd test` en Windows o `./mvnw test`. Son 11 pruebas.

| Clase | Qué prueba |
|---|---|
| `FifoAllocatorTest` | US-03 esc. 1, una línea por lote, lote vencido y stock insuficiente, sin Spring |
| `SalesServiceImplTest` | US-03 esc. 1 y 2, terminar y cancelar, con los repositorios falsos de `fakes/` |
| `SalesPersistenceTest` | Los adaptadores contra H2 con el esquema de Flyway (`@DataJpaTest`) |

## Probarlo a mano

Mientras no existan los datos demo (C4) ni la pantalla de productos (D4), los datos se cargan desde `http://localhost:8080/h2-console`. Usa la JDBC URL `jdbc:h2:file:./data/inventiqdb` y el usuario `sa`, sin contraseña:

```sql
INSERT INTO users (username, password_hash, role) VALUES ('cajero', 'pendiente-login', 'CASHIER');
INSERT INTO products (barcode, name, sale_price, unit_cost, perishable, safety_stock)
VALUES ('7702001000012', 'Yogur de fresa 200 g', 3200, 2100, TRUE, 4);
INSERT INTO batches (product_id, quantity_received, quantity_remaining, expiration_date, received_at, unit_cost)
VALUES (1, 3, 3, DATEADD('DAY', 3, CURRENT_DATE), CURRENT_TIMESTAMP, 2100),
       (1, 3, 3, DATEADD('DAY', 25, CURRENT_DATE), CURRENT_TIMESTAMP, 2100);
```

Luego abre `http://localhost:8080/pos` y escanea `7702001000012` dos veces. Se descuentan 2 unidades del lote que vence en 3 días.

## Pendiente

- **B5:** vista de inventario.
- **B7:** reintento cuando dos cajeros descuentan el mismo lote a la vez. Hoy `@Version` evita que se pise el stock, pero se muestra una página de error.
- **B8:** prueba de aceptación completa.
- **Login (A7):** mientras no exista, las ventas quedan a nombre del usuario con id 1 (`PosController.CASHIER_ID`).
- **Layout y estilos (D1):** la página de ventas es HTML simple, sin el layout común.

**Uso de IA:** esta parte se desarrolló con ayuda de Claude Code. El autor revisó cada cambio antes de subirlo.
