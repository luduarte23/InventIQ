# Administración → Historial y análisis

Pestaña con el historial de ventas, los precios de compra y venta de cada producto y gráficas
interactivas. Solo lee datos; no cambia nada.

## Qué muestra

**Ventas**: el período elegido (últimos 7, 30 o 90 días) filtra todo lo que hay debajo.

| Bloque | Contenido |
|---|---|
| Indicadores | Ingresos, tickets y unidades, con la variación frente al período anterior de igual duración; ticket promedio; margen bruto estimado |
| Ingresos por día | Línea con área. Se puede cambiar a unidades o a tickets por día |
| Ingresos por tipo de producto | Barras horizontales, de mayor a menor |
| Productos más vendidos | Los 10 primeros, por ingresos o por unidades |
| Ingresos promedio por día de la semana | Columnas: qué días se vende más |
| Ingresos por franja del día | Dona con mañana, tarde y noche (3 partes de un todo) |
| Margen y rotación por producto | Burbujas: rotación en el eje X (unidades por día), margen en el eje Y y tamaño según los ingresos. Ayuda a priorizar compras (FR-06) |
| Historial de ventas | Las 30 ventas más recientes del período: fecha en Día/Mes/Año, hora, productos, unidades y total |

**Precios de compra y venta**:
- **Tabla** con costo y precio actuales, margen, variación del costo y del precio en el período y
  fecha del último cambio.
- **Gráfica escalonada** del precio de venta y el costo de un producto. La distancia entre las dos
  líneas es el margen. Se elige el producto en el selector, con "Ver evolución" en la tabla o con
  el botón "Historial" de cada fila en Productos.

## Cálculos

Están en `application/reports/SalesReportService` y en la regla `domain/rules/PriceTimeline`. Todo se
prueba con fakes, sin Spring.

- **Margen bruto estimado** = ingresos − costo vigente **el día de cada venta**. Usa
  `PriceTimeline.costAt`, no el costo de hoy. Así, si el costo subió la semana pasada, las ventas
  anteriores conservan su margen real.
- **Variación** = (período actual − período anterior) / período anterior. Si el período anterior no
  tuvo ventas no se muestra, para no inventar un porcentaje.
- **Día de la semana**: se suman los ingresos de cada día y se dividen por cuántas veces cae ese día
  en el período.
- **Franjas**: mañana antes de las 12 m., tarde de 12 m. a 5 p. m., noche desde las 5 p. m.
- **Rotación** = unidades vendidas / días del período.

## Historial de precios

Cada producto guarda su precio de venta y su costo cada vez que cambian (tabla `price_history`):
- al registrarlo;
- al editarlo, si cambió el precio o el costo. Una edición que solo cambia el nombre no agrega
  registro.

## Diseño de las gráficas

Se siguió una guía de visualización de datos. Las decisiones principales:

- **Nunca dos ejes Y en una gráfica.** Por eso ingresos, unidades y tickets se alternan con botones
  en lugar de superponerse.
- **La forma la decide el dato**:
  - barras para comparar magnitudes;
  - línea para tendencias;
  - dona solo para parte de un todo con pocas partes ordenadas (3 franjas);
  - burbujas para relacionar dos medidas.
- **Una serie, un color.** Las barras de un mismo dato llevan todas el azul de la serie 1; el color
  no repite lo que ya dice el largo. Solo la gráfica de precios tiene dos series (azul = venta,
  naranja = costo), con leyenda y etiquetas directas al final de cada línea.
- **Paleta validada** con el validador de la guía contra el fondo blanco. Los dos colores
  categóricos dan ΔE 24,7 bajo daltonismo (mínimo 8) y la rampa de las franjas es monótona.
- **Marcas finas**:
  - barras de 24 px como máximo, con la punta redondeada y la base recta;
  - líneas de 2 px;
  - rejilla tenue y sólida.
- **Interacción**:
  - línea vertical que sigue el cursor en las gráficas de línea;
  - tooltip con el valor destacado;
  - zonas de toque más grandes que la marca.
- **"Ver tabla"** en cada gráfica muestra los mismos datos como tabla, para leer todos los valores
  sin pasar el cursor.
- **Sin internet**: Chart.js 4.5.1 se sirve desde el JAR (WebJar), así la app sigue arrancando con un
  solo comando.
- **Sin API REST**: los datos llegan en la misma página como JSON (`window.inventiqHistory`), armados
  en `HistoryChartData` solo con mapas y listas.

## Datos demo

`db/demo/V20261023_1400__HistorialVentasDemo.java` es una migración de Flyway en Java que solo corre
con el perfil `demo`. Genera:

- **Precios**: cuatro cambios por producto en los últimos 6 meses, que terminan en el precio actual.
- **90 días de ventas** con un patrón realista: más ventas el fin de semana, picos a media mañana, al
  almuerzo y al final de la tarde, y productos de diario (lácteos, pan, huevos) más frecuentes.
- **Precio cobrado**: el que estaba vigente ese día.
- **Lotes históricos ya agotados** de los que salen esas ventas, así el inventario actual no cambia.

Usa una semilla fija, así la demo es igual en todas las máquinas. `DemoDataTest` comprueba que todo
cuadre:
- los totales de cada venta coinciden con sus líneas;
- cada venta sale de un lote del mismo producto;
- el historial de precios termina en el precio actual;
- cada venta usó el precio vigente ese día.

## Pruebas

| Clase | Tipo | Pruebas |
|---|---|---|
| `PriceTimelineTest` | Regla pura | 7 |
| `SalesReportServiceTest` | Servicio con fakes | 12 |
| `HistoryAcceptanceTest` | Aceptación por HTTP | 5 |
| `DemoDataTest` | Integración con los datos demo | 6 |
