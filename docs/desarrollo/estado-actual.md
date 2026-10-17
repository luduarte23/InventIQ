# Estado actual

*Última actualización: 17 de octubre de 2026.*

## Qué funciona

| Área | Estado | Dónde verlo |
|---|---|---|
| Arranque con un solo comando, H2 en archivo, Flyway | Listo (Fase 1) | `./mvnw spring-boot:run` |
| Interfaz con barra lateral retráctil, inicio con indicadores | Lista | Cualquier página · [interfaz.md](interfaz.md) |
| **Venta por código de barras con FIFO** (FR-02, US-03) | Lista (Parte B) | Ventas (`/pos`) |
| **Inventario**: lotes en orden FIFO, vencimiento en Día/Mes/Año con colores, filtros | Listo | Inventario (`/inventory`) |
| Concurrencia en ventas (bloqueo optimista + reintento) | Lista (B7) | Prueba `SalesConcurrencyTest` |
| **Productos**: registrar por escáner o a mano, editar, pausar o habilitar la venta | Listo (parte de D4) | Administración → Productos |
| **Historial y análisis**: ventas, precios de compra y venta, 7 gráficas interactivas | Listo | Administración → Historial |
| Modelo de dominio y puertos completos (A2): pedidos con sus estados, parámetros, auditoría, alertas, usuarios | Listo; sin persistencia de pedidos, parámetros, auditoría ni alertas (D2) | `domain/model`, `domain/port` |
| Usuarios iniciales (admin y cajero) | En la base y con repositorio (`UserRepository`, A5); sin login todavía | Tabla `app_user` |
| CI con cobertura por capa en el PR, umbral de 80 % en `domain` y plantilla de PR (A3) | Listo; falta activar la protección de `main` en GitHub | [fase-1-esqueleto.md](fase-1-esqueleto.md#ci) |
| README v1 (A4): arranque en cada terminal, solución de problemas, pruebas, flujo de ramas y PR, uso de IA | Listo | `README.md` |
| Mensajes por WhatsApp simulados en el log (A6); Twilio en la Entrega 3 | Listo | `LogMessageNotifier` |
| Login y roles (FR-01) | Pendiente (A7) | — |
| ROP y borradores de pedido (FR-03, FR-04) | Pendiente (C) | — |
| Inactivar productos, proveedores, parámetros, recepción y aprobación (FR-08, FR-10) | Pendiente (D) | — |

## Cómo probarlo

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Con el perfil `demo` la base (`./data/inventiq-demo`) trae:
- 4 proveedores y 21 productos de 11 tipos, con sus lotes (vencidos, por vencer y vigentes);
- el café con la venta pausada;
- **90 días de ventas simuladas** y seis meses de historial de precios.

Códigos útiles:

| Código | Producto | Para qué sirve |
|---|---|---|
| `7702001000012` | Yogur de fresa | Caso de US-03: dos lotes con vencimientos distintos |
| `7702001000029` | Leche entera | Tiene un lote vencido que no se vende |
| `7703002000049` | Café molido | Venta pausada: el POS lo rechaza aunque hay 60 unidades |
| `7707006000029` | Papas fritas | Un lote vencido y otro por vencer |
| `7702001000043` | Kumis | Sin stock: muestra el error de stock insuficiente |
| `7709999999991` | — | Código nuevo y válido para registrar un producto |
| `7709999999999` | — | Código mal escrito: la pantalla de productos explica el error |

Qué revisar en cada pantalla:
- **Inicio**: los lotes que necesitan atención.
- **Ventas**: <kbd>Enter</kbd> agrega el código, <kbd>F2</kbd> cierra la venta y <kbd>Esc</kbd> limpia
  el campo.
- **Inventario**: filtra por tipo o estado y despliega los lotes de un producto.
- **Productos**: prueba los botones de cada fila (Editar, Pausar venta, Lotes, Historial).
- **Historial**: cambia el período, alterna métricas, usa "Ver tabla" y elige un producto en la
  evolución de precios.

Para empezar de cero, detén la aplicación y borra la carpeta `data/`. Una base demo creada antes
del 16 de octubre se actualiza sola al arrancar: Flyway aplica las migraciones nuevas.

## Pruebas

`./mvnw verify`: **257 pruebas en verde**. Cobertura de líneas:

| Capa | Cobertura | Detalle |
|---|---|---|
| `domain` | **99,4 %** | 522/525 líneas; 96,6 % de ramas |
| `application` | **99,7 %** | 338/339 líneas |

El mínimo exigido es 80 %. El detalle por módulo está en
[parte-b-pos-inventario.md](parte-b-pos-inventario.md#reporte-de-pruebas),
[admin-productos.md](admin-productos.md#reporte-de-pruebas) e
[historial-analisis.md](historial-analisis.md#pruebas).

## Lo que falta para la Entrega 2

Lo que sigue (tareas del reparto):
- login con roles (A7–A8);
- cálculo del ROP y borradores (C); el historial de ventas para el ROP ya está disponible en
  `SaleRepository.findItemsSoldBetween`;
- proveedores, parámetros, recepción y aprobación de pedidos (D);
- datos de demostración completos (C4);
- documento de arquitectura (A11);
- el día de integración (29 de octubre).
