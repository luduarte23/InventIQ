# Bitácora

## 17–23 oct 2026: esqueleto y Parte B

- Esqueleto del proyecto: `pom.xml` (Spring Boot 4.1.1, Java 21), wrapper de Maven, configuración
  de H2 + Flyway, plantilla base, CI y prueba de arquitectura.
- Parte B completa (B1–B8): regla `FifoAllocator`, `SalesService`, persistencia de ventas, pantalla de
  ventas que se usa solo con el teclado, vista de inventario, concurrencia y aceptación de US-03.
- Además se hicieron registro de productos, catálogo ampliado, pausa de ventas, historial con
  gráficas, inicio con indicadores y un generador de datos demo.

## 24 oct 2026: versión compacta para la Entrega 2

El equipo pidió entregar menos líneas (meta: 12–15 mil entre los cuatro). Se rehízo la entrega desde
`main` con solo la estructura y la Parte B (≈5 mil líneas):

- Se quitaron el registro de productos (le corresponde a D4), el catálogo ampliado (tipo, marca,
  contenido), la pausa de ventas, el historial con gráficas (y Chart.js), el inicio con indicadores
  y el generador de datos demo.
- `Product` volvió a las columnas del esquema inicial. Los puertos quedaron solo con lo que usa B.
- La hoja de estilos pasó de ≈1.400 a ≈300 líneas, solo con las clases que se usan.
- `CLAUDE.md` quedó fuera del repositorio (`.gitignore`); cada integrante conserva su copia local.
- Lo que se quitó queda en la rama local de respaldo `feature/base-entrega2` (commit `8d377d2`)
  para retomarlo en la Entrega 3.
