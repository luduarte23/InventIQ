# Interfaz: navegación y sistema visual

Cómo está armada la interfaz de InventIQ y qué reglas sigue, para que las pantallas nuevas de C y
D se vean igual.

## Estructura de cada página

- **Cabecera azul fija.** Lleva el botón del menú, el logo InventIQ (en el mismo lugar de siempre) y
  la fecha de hoy.
- **Barra lateral retráctil**, con secciones:
  - Operación: Ventas, Inventario.
  - Abastecimiento: Reabastecimiento y Pedidos, marcados "Pronto" hasta que C y D los hagan.
  - Administración: Productos, Historial.
- **Contenido**: mensajes, encabezado de la página (título, subtítulo y acciones) y secciones en
  tarjetas.

La barra lateral funciona así:

| Pantalla | Comportamiento |
|---|---|
| Computador (más de 900 px) | El botón ☰ la contrae a solo íconos (68 px) o la expande con texto (236 px). El navegador recuerda la elección. Contraída, cada ícono muestra su nombre al pasar el cursor |
| Celular o tableta | La barra se abre encima del contenido y se cierra tocando fuera o con <kbd>Esc</kbd> |

El ítem de la página actual se resalta y lleva `aria-current="page"`. La ruta la entrega
`NavigationAdvice`, porque Thymeleaf 3.1 ya no expone el request a las plantillas.

## Piezas reutilizables

| Pieza | Archivo | Uso |
|---|---|---|
| Plantilla base | `templates/layout.html` | `th:replace="~{layout :: layout(~{::title}, ~{::main})}"` |
| Íconos SVG propios | `templates/fragments/icons.html` | `th:replace="~{fragments/icons :: icon('cart')}"` (sin dependencias externas) |
| Subpestañas de Administración | `templates/fragments/admin-nav.html` | D agrega Proveedores y Parámetros aquí |
| Fecha en Día/Mes/Año con color | `templates/fragments/expiration.html` | `cells(view)` da las tres celdas; `badge(view, hasStock)` da la etiqueta |
| Formatos en español | `web/ViewFormat.java` (`@fmt`) | `${@fmt.money(x)}`, `${@fmt.month(d)}`, `${@fmt.expiration(view)}`… |
| Buscador y filtros de tablas | `static/js/table-tools.js` | Atributos `data-filter-table`, `data-filter-text`, `data-filter-key`, `data-row`, `data-details` |
| Barra lateral y mensajes | `static/js/app.js` | Se carga en todas las páginas |

## Reglas visuales

- **Colores con significado** (variables en `static/css/app.css`):
  - vencido = rojo, por vencer = ámbar, vigente = verde, venta pausada = azul claro;
  - bajo el punto de reorden = naranja;
  - el color **nunca va solo**: cada estado lleva ícono y texto (por ejemplo "Vence en 3 días").
- **Fechas** en tres columnas (Día, Mes, Año) bajo un encabezado común, con el fondo del color de su
  estado. Hay una leyenda de colores encima de cada tabla. "Por vencer" significa 7 días o menos
  (`ExpirationStatus.EXPIRING_SOON_DAYS`).
- **Tablas compactas**: números alineados a la derecha con cifras de igual ancho, descripciones
  recortadas a dos líneas (el texto completo aparece al pasar el cursor) y filas desplegables para
  los lotes.
- **Indicadores (KPI)**: etiqueta, valor grande y una nota. Los de estado llevan un borde superior del
  color del estado.
- **Botones de fila**: con ícono y texto. En pantallas de menos de 1400 px queda solo el ícono, pero
  el nombre sigue disponible para lectores de pantalla (`aria-label`) y al pasar el cursor.
- **Accesibilidad**:
  - enlace "Saltar al contenido";
  - foco visible en todos los controles;
  - formularios con etiquetas y errores junto a cada campo;
  - los mensajes de error se anuncian (`role="alert"`).

## Inicio

Resume el día:
- **Indicadores**: ventas de hoy, unidades vendibles, lotes por vencer y lotes vencidos.
- **Necesitan atención**: los lotes más urgentes, con su fecha en columnas.
- **Accesos rápidos** a las tareas frecuentes.
- **Venta pausada**: los productos que están en esa situación.
