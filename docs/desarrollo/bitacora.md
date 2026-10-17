# Bitácora de desarrollo

Registro de lo que se hizo, en orden. Todo el trabajo de este registro se hizo con Claude Code
(declarado en el README); los commits los hace el equipo.

## 16 de octubre de 2026

### CLAUDE.md y plan

- Se leyeron los tres documentos de `docs/` (lineamientos del curso, Entrega 1 y reparto de la Entrega 2).
- Se creó `CLAUDE.md` con la descripción, el stack, la estructura de carpetas, los comandos, las
  convenciones y la regla de que Claude nunca hace `git commit` ni `git push`.
- Se propuso un plan por fases (0 a 7) alineado con el calendario del reparto.

### Fase 1: esqueleto ejecutable

Tareas A1, A3, A4 y D1. Detalle en [fase-1-esqueleto.md](fase-1-esqueleto.md).

- Proyecto Maven con Spring Boot 4.1.1 (Java 21) y wrapper con Maven 3.9.16.
- H2 en archivo, Flyway y la migración `V20261017_1200__esquema_inicial.sql` con 11 tablas.
- JaCoCo con umbral de 80 % sobre `domain`, prueba de arquitectura con ArchUnit y CI en GitHub Actions.
- README v1, plantilla base Thymeleaf y un `package-info.java` por paquete con su responsable.
- Verificado: build en verde y la app responde en `http://localhost:8080`. También se comprobó
  que ArchUnit y JaCoCo fallan cuando deben.
- `.gitignore` completo para Java, Maven, Spring Boot, H2 e IDEs (incluye `.claude/settings.local.json`).

### Parte B: POS e inventario

Tareas B1 a B9 y la base de B10. Detalle en [parte-b-pos-inventario.md](parte-b-pos-inventario.md).

- Como A2, A5 y A7 aún no existen, se creó la parte mínima de A que B necesita: modelo
  `Product` y `Batch`, puertos, entidades JPA, `TransactionRunner` y usuarios iniciales. Queda
  marcado para revisión en [decisiones-pendientes.md](decisiones-pendientes.md).
- Regla pura `FifoAllocator` con 13 pruebas.
- `SalesService` (iniciar, escanear, cerrar) con reintento ante conflictos de concurrencia.
- `InventoryQueryService` y la vista de inventario por lotes.
- Persistencia de Product, Batch (con `@Version`), Sale y SaleItem.
- Pantalla de ventas para trabajar solo con teclado (foco automático, Enter, F2, Esc).
- Datos de demostración provisionales (9 productos) en el perfil `demo`.
- Pruebas: 73 en total, cobertura del dominio de 99,1 %. Incluye la aceptación completa de US-03
  por HTTP y la prueba de dos hilos sobre el mismo lote, repetida 10 veces.
- Verificado a mano con la app corriendo: una venta de yogur ×2 y arroz ×3 dio $ 20.800 y
  descontó del lote que vence primero.
- Incidente: al detener la app de prueba se usó un filtro de procesos demasiado amplio, que
  pudo cerrar el servidor de Java de VS Code. Se corrigió el filtro (ahora busca solo el JAR de
  InventIQ). Si VS Code muestra un error del servidor de Java, basta con recargar la ventana.

### Administración → Productos (parte de D4)

Detalle en [admin-productos.md](admin-productos.md).

- Nueva subpestaña **Administración → Productos**, a la que ahora lleva el menú
  "Administración". Para registrar un producto: se escanea o se escribe el código; si es nuevo se
  abre el formulario, si ya existe se muestran sus datos y si está mal escrito se explica por qué.
- Reglas en el dominio: `GtinValidator` (código estándar con dígito de verificación) y
  `NewProductValidator` (pesos enteros, precio ≥ costo, stock inicial coherente con
  perecedero/vencimiento). Todos los errores se muestran a la vez, cada uno en su campo.
- Stock inicial opcional: crea el primer lote en la misma transacción, así el producto se puede
  vender de inmediato.
- Cambios en puertos (requieren revisión del equipo): `create` en `ProductRepository` y
  `BatchRepository`, y un `SupplierRepository` nuevo de solo lectura.
- Los códigos de los datos demo provisionales no tenían dígito de verificación válido; se
  corrigieron (por ejemplo, el yogur pasó de `7702001000011` a `7702001000012`). Por eso hay que
  borrar `data/` una vez.
- Pruebas: 68 nuevas, 141 en total. Cobertura del dominio: 99,5 %.
- Verificado con la app corriendo: se registró "Avena en hojuelas 500 g" con 20 unidades, se
  vendieron 3 en el POS ($ 20.700) y el inventario mostró 17. Registrarlo otra vez fue rechazado.

### Interfaz nueva, catálogo ampliado e historial

Detalle en [interfaz.md](interfaz.md), [admin-productos.md](admin-productos.md) e
[historial-analisis.md](historial-analisis.md).

- **Interfaz**:
  - barra lateral retráctil (íconos solos o con texto, se recuerda el estado; en celular se abre
    encima);
  - logo en su lugar y fecha en la cabecera;
  - hoja de estilos rehecha como sistema (variables, tarjetas, indicadores, tablas, etiquetas de
    estado);
  - íconos SVG propios;
  - página de inicio con indicadores y alertas;
  - página de error con el mismo diseño.
- **Catálogo**:
  - tipo, marca, contenido (g, kg, ml, L, und) y descripción en el producto, el formulario, el
    inventario y la tabla de productos;
  - nuevas reglas de validación (tipo obligatorio, contenido coherente);
  - el tamaño pasó del nombre de los productos demo a su columna.
- **Tablas**: buscador y filtros; próximo vencimiento en Día/Mes/Año con colores e ícono; lotes
  desplegables con su orden de venta.
- **Botones por producto**: Editar, Pausar o Habilitar venta, Lotes e Historial. La venta pausada
  deja el producto en el inventario, pero el POS lo rechaza (al escanear y al cerrar).
- **Historial y análisis**:
  - indicadores con variación contra el período anterior;
  - 7 gráficas de 5 tipos (Chart.js 4.5.1 servido desde el JAR), cada una con "Ver tabla";
  - historial de ventas;
  - precios de compra y venta con su evolución;
  - el margen se calcula con el costo vigente el día de cada venta.
- **Datos demo**: 12 productos y 2 proveedores más, y una migración Java que genera 90 días de
  ventas y seis meses de precios, con semilla fija.
- **Diseño de gráficas**: se siguió una guía de visualización. Sin doble eje, una serie por color,
  paleta validada para daltonismo (ΔE 24,7 entre venta y costo) y vista de tabla en todas.
- **Pruebas**: 224 en total (83 nuevas); cobertura del dominio 99,4 % y de la aplicación 99,7 %.
  `DemoDataTest` comprueba que los datos demo cuadren.
- **Verificado con la app**: todas las páginas responden 200, el log no tiene errores, los scripts
  tienen sintaxis válida y el JSON de las gráficas trae 30 días, 11 tipos y 21 productos con su
  historia de precios. No se tomaron capturas de pantalla.
- **Integración con el repositorio de GitHub** (`luduarte23/InventIQ`):
  - `main` solo tenía `README.md` y `.gitignore`. Ambos se integraron como base: el `.gitignore`
    conserva la plantilla de Java arriba y suma nuestras reglas (sin quitar ninguna línea); el README
    conserva todas sus secciones en inglés y llena las que estaban "In development".
  - Se verificó con un clon del repositorio que, partiendo de `main`, los dos archivos quedan como
    modificaciones y no como conflicto.
  - La rama `luduarte23-patch-1` solo agrega `index.html` (presentación de la Entrega 1) y no choca
    con nada.
- **Errores encontrados y corregidos durante el trabajo**:
  - `stripTrailingZeros()` mostraba "5.2E+3" en el formulario de edición;
  - una prueba usaba "hoy" en la zona del sistema y no en la de Bogotá (habría fallado en el CI de
    madrugada).

## 17 de octubre de 2026

### A2: modelo de dominio y puertos completos

- **Modelo nuevo** (`domain.model`, Java puro):
  - compras: `PurchaseOrder`, `OrderLine` y `OrderStatus` con la tabla de transiciones válidas
    (`DRAFT → APPROVED → SENT → PARTIALLY_RECEIVED → RECEIVED`), aprobación, envío y recepción
    total o parcial con pendientes visibles (US-05);
  - `Settings` (presupuesto semanal, ventana de ventas, WhatsApp del admin);
  - auditoría (`AuditEntry`, `AuditAction`) y alertas (`Alert`, `AlertType`);
  - usuarios (`User`, `Role`);
  - pedido mínimo del proveedor (`MinimumOrder`, `MinimumOrderType`); `Supplier` lo incluye.
- **Excepciones nuevas**: `InvalidOrderTransitionException`, `OrderNotEditableException`,
  `OrderWithoutSupplierException`, `InvalidReceiptException`, `InvalidSettingsException`.
- **Puertos nuevos**: `PurchaseOrderRepository`, `SettingsRepository`, `AuditLogRepository`,
  `AlertRepository`, `UserRepository` y `MessageNotifier`.
- `SupplierMapper` lee el pedido mínimo de la tabla; el fake de proveedores se ajustó al nuevo campo.
- Pruebas: 22 nuevas (`OrderStatusTest`, `PurchaseOrderTest`, `AdminModelTest`), 246 en total.
  Cobertura del dominio: 99,4 % de líneas y 96,6 % de ramas.

### A3: calidad y CI

Lo base (JaCoCo con umbral de 80 % en `domain`, ArchUnit y el workflow) venía de la Fase 1. Se completó así:

- `ci.yml`:
  - tabla de cobertura por capa en el resumen de cada corrida;
  - reportes de Surefire como artefacto cuando falla una prueba;
  - `concurrency` para cancelar corridas viejas del mismo PR;
  - `permissions: contents: read`, `timeout-minutes` y `workflow_dispatch`.
- Plantilla de PR (`.github/pull_request_template.md`) con la lista de chequeo del equipo.
- Pasos para proteger `main` documentados en [fase-1-esqueleto.md](fase-1-esqueleto.md#ci). Se
  configura en GitHub; falta aplicarla.
- README: el flujo de trabajo menciona la protección de `main` y la plantilla.
- Verificado:
  - el script del resumen corrió en local con el reporte real (dominio 99,4 %) y el YAML es válido;
  - `jacoco:check` falla con el umbral subido a 100 % y pasa con el 80 % configurado.

### A4: README v1

El README ya tenía la descripción, el alcance, la arquitectura y los comandos. Se completó para que
alguien ajeno al equipo arranque el proyecto solo con él:

- **Arranque por terminal**: tabla con Linux/macOS/Git Bash, PowerShell y cmd. Se comprobó que en
  PowerShell `mvnw.cmd` sin `.\` no se encuentra, y que `-Dspring-boot.run.profiles=demo` sin
  comillas se parte en el primer punto y arranca **sin** datos demo.
- **Solución de problemas**: Java ausente, permiso de `mvnw`, puerto ocupado, perfil demo vacío,
  error de Flyway al cambiar de rama y base en uso.
- **Pruebas**: la variante de PowerShell, correr `verify` antes del PR y dónde ver la cobertura en
  el CI.
- **Flujo de trabajo**: reglas (rama por tarea, revisor distinto cada vez, acuerdo de los 4 para el
  dominio) y los comandos paso a paso de una tarea.
- **Declaración de uso de IA**: cómo se usa Claude Code (modo Plan o Manual, nunca hace commits,
  revisión en PR) y filas nuevas para A2, A3 y A4.
- **Correcciones**: los puertos ya no son solo `ProductRepository` y `BatchRepository`; Twilio está
  marcado para la Entrega 3, porque en la Entrega 2 los mensajes solo van al log.
- **Verificado**: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"` arrancó en
  PowerShell con el perfil `demo` activo, y `/inventory` respondió 200.

### A5: persistencia de Product, Batch y User

La persistencia de Product y Batch venía de la Parte B: entidades separadas del dominio, mappers,
adaptadores, `@Version` en las dos tablas, y `findAvailableByProduct` en orden FIFO (vence primero,
luego recibido primero) con sus pruebas en `SalesPersistenceTest` y `CatalogPersistenceTest`. Se
completó User:

- `UserEntity` (tabla `app_user`, solo lectura), `UserJpaRepository`, `UserMapper` y
  `UserRepositoryAdapter`, que implementa el puerto `UserRepository` de A2.
- `SaleRepositoryAdapter` busca al cajero con `UserJpaRepository`. Se eliminaron las dos consultas
  nativas sobre `app_user` de `SaleJpaRepository` (decisión 7).
- `UserPersistenceTest` (`@DataJpaTest` con el esquema real de Flyway) prueba:
  - admin y cajero con sus roles;
  - contraseñas guardadas como BCrypt, nunca en claro;
  - usuario inexistente;
  - usuario inactivo devuelto como tal.
- Verificado: `./mvnw verify` en verde con 250 pruebas; `ddl-auto=validate` acepta `UserEntity`;
  las clases nuevas tienen 100 % de líneas cubiertas y las pruebas de ventas siguen pasando.
