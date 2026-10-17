# Administración → Productos

Subpestaña para que el administrador registre productos escaneando o escribiendo su código, los
edite y decida cuáles están a la venta. Cubre la parte de productos de la tarea **D4** (crear,
editar), más la **venta pausada**. La inactivación y la gestión de proveedores siguen pendientes
para D.

## Cómo se usa

1. Entra a **Productos** en la barra lateral (`/admin/products`).
2. En **Agregar producto**, escanea el producto o escribe su código y presiona Enter:
   - **Código nuevo y válido**: se abre el formulario con el código ya puesto.
   - **Código que ya existe**: se muestran sus datos, con botones para editarlo y ver sus lotes.
   - **Código mal escrito**: se explica el problema (por ejemplo, "el último dígito debería ser 1").
3. Llena el formulario, que tiene cinco secciones:
   - **Identificación**: nombre, tipo y marca.
   - **Presentación**: contenido (cantidad y unidad: g, kg, ml, L o und) y descripción.
   - **Precios**: precio de venta y costo, con el margen calculado mientras se escribe.
   - **Abastecimiento**: proveedor, stock de seguridad y si es perecedero.
   - **Venta y stock inicial**: si se pone a la venta de inmediato, y el primer lote (opcional).

El **catálogo** tiene:
- **Indicadores**: productos, a la venta, con venta pausada, sin proveedor y margen promedio.
- **Buscador y filtros** por tipo y por estado.
- **Columnas**: código, producto, tipo, marca, contenido, descripción, precio y costo, margen,
  proveedor, stock, próximo vencimiento (Día, Mes y Año, con color) y estado de venta.

### Botones de cada fila

| Botón | Qué hace |
|---|---|
| **Editar** | Abre el formulario con los datos actuales. El código no se puede cambiar. Si cambia el precio o el costo, queda en el historial de precios |
| **Pausar venta / Habilitar venta** | Con la venta pausada, el producto **sigue en el inventario con su stock**, pero el POS no lo vende (por ejemplo, stock comprado antes de un alza que aún no se quiere vender). El inventario lo marca como "Venta pausada" y se puede filtrar |
| **Lotes** | Abre el inventario filtrado en ese producto, con sus lotes ya desplegados |
| **Historial** | Abre Historial y análisis con la evolución de los precios de ese producto |

## Reglas de negocio

Todas viven en el dominio y se prueban sin Spring ni base de datos.

| Regla | Dónde |
|---|---|
| Código GTIN válido (EAN-8, UPC-A, EAN-13 o GTIN-14) con dígito de verificación | `GtinValidator` |
| El código no se repite, ni siquiera con un producto inactivo | `CatalogService` + restricción `UNIQUE` |
| Nombre obligatorio (máximo 150), **tipo obligatorio**, marca hasta 80 caracteres, descripción hasta 500 | `NewProductValidator` |
| **Contenido**: cantidad y unidad van juntas; la cantidad es mayor que cero, con dos decimales como máximo; las unidades por empaque ("und") son enteras | `NewProductValidator` |
| Precio y costo en pesos enteros y mayores que cero; **validación cruzada**: precio ≥ costo | `NewProductValidator` |
| Stock inicial coherente: un perecedero exige fecha no pasada; uno no perecedero no lleva fecha | `NewProductValidator` |
| El proveedor, si se elige, existe y está activo | `CatalogService` |
| **Venta pausada**: el POS la rechaza al escanear y también al cerrar la venta, si se pausó mientras el producto estaba en el carrito | `SalesService` + `Product.isSellable()` |
| Al registrar un producto, y al editar su precio o costo, se guarda en el historial de precios | `CatalogService` |

## Diseño

- **`ProductData`** reúne los datos editables y **`NewProduct`** = código + `ProductData` +
  venta habilitada + stock inicial. El mismo validador revisa el alta y la edición; la edición no
  revisa el código ni el stock inicial.
- **Tipo y unidad** son listas fijas en el dominio (`ProductCategory`, `ContentUnit`) y se guardan
  como texto. Si la tienda necesita sus propios tipos, se pueden pasar a una tabla.
- **Errores en dos niveles**: los campos ilegibles se marcan con `messages.properties` y las reglas
  de negocio con el mapa de errores del dominio. Si hay un campo ilegible no se guarda nada.

## Reporte de pruebas

| Clase | Tipo | Pruebas |
|---|---|---|
| `GtinValidatorTest` | Regla pura | 15 |
| `NewProductValidatorTest` | Regla pura | 26 |
| `ProductTest`, `NewProductTest`, `ExpirationStatusTest` | Modelo | 15 |
| `CatalogServiceTest` | Servicio con fakes | 24 |
| `CatalogPersistenceTest` | `@DataJpaTest` contra H2 | 7 |
| `ProductAdminAcceptanceTest` | Aceptación por HTTP | 18 |

## Pendiente

- Restringir `/admin/**` al rol ADMIN cuando exista el login (A7).
- Inactivar productos y gestionar proveedores (D4).
- Registrar el stock inicial y la pausa de la venta en la auditoría cuando exista (D3/D5).
- Detectar ediciones simultáneas del mismo producto. Hoy gana la última; la versión `@Version` ya
  está en la tabla.
