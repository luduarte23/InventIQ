# Estado actual (24 oct 2026)

## Qué funciona

- **Estructura completa** por capas (`domain`, `application`, `infrastructure`, `web`). Cada paquete
  tiene su `package-info.java`. ArchUnit hace cumplir las dependencias, JaCoCo exige el 80 % del
  dominio y el CI corre `./mvnw verify` en cada PR.
- **Esquema inicial** (Flyway) con todas las tablas de la Entrega 2 y los usuarios `admin` y `cajero`.
- **Ventas** (`/pos`): escanear o escribir el código, carrito en la sesión y cierre con deducción
  FIFO. Si dos cajeros venden a la vez, la venta se reintenta.
- **Inventario** (`/inventory`): stock vendible por producto, lotes en orden de venta y vencimiento
  en Día/Mes/Año con color y etiqueta. Tiene buscador y filtro por estado.
- 80 pruebas: dominio, servicios con fakes, H2, concurrencia y aceptación de US-03.

## Cómo probarlo

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Abre <http://localhost:8080/pos> y prueba con estos códigos:

| Código | Producto | Qué muestra |
|---|---|---|
| `7702001000012` | Yogur de fresa | Dos lotes: se descuenta primero el que vence en 3 días |
| `7702001000029` | Leche entera | Un lote vencido que no se vende |
| `7702001000043` | Kumis | Sin stock: error de stock insuficiente |
| `7703002000018` | Arroz | No perecedero, sin fecha de vencimiento |
| `0000000000000` | — | Código desconocido: aviso, y la venta continúa |

Si la app no arranca porque Flyway no encuentra migraciones que ya se aplicaron, borra la carpeta
`./data` (quedó de una versión anterior).

## Qué sigue

- Login y roles (A7). Con eso, `/inventory` queda solo para el administrador.
- ROP y borradores de pedido (C), pedidos y recepción (D).
- B9: correcciones tras integrar con A, C y D. B10: sección del documento con capturas.
- Para la Entrega 3 se pueden retomar las funciones que se quitaron para esta entrega (ver la
  bitácora).
