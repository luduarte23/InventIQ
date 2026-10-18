# Decisiones pendientes

Lo que el equipo debe revisar antes de fusionar. Según las reglas comunes, cambiar el dominio o los
puertos requiere el acuerdo de los cuatro integrantes.

## Hechas en la Fase 1

1. **Esquema V1 frente a `ACUERDOS.md` §3.** El esquema se dedujo de los documentos porque
   `ACUERDOS.md` no estaba disponible. Hay que compararlo y ajustarlo antes de fusionar la primera
   migración. *Responsable: A.*
2. **Protección de `main` en GitHub.** Exigir un PR con una aprobación y el check `CI / build`.
   *Responsable: A.* Los pasos están en
   [fase-1-esqueleto.md](fase-1-esqueleto.md#protección-de-main-se-configura-en-github-no-en-el-código);
   falta que el dueño del repositorio los aplique en GitHub.

## Hechas para la Parte B (trabajo que era de A)

B depende de A2, A5 y A7, que aún no existían. Para no bloquearla se hizo lo mínimo, marcado
para revisión:

3. **Modelo y puertos (A2).** Se crearon `Product`, `Batch`, `Sale`, `SaleItem`, `Cart` y los
   puertos `ProductRepository`, `BatchRepository` y `SaleRepository`, solo con lo que usa B. Si el
   modelo acordado el 17 de octubre difiere, hay que alinearlo.
   - `Product` es un `record` de solo lectura; D4 agregará la escritura.
   - `Batch` lleva la versión de bloqueo optimista como un `long`. Sigue siendo Java puro, pero así
     el adaptador puede detectar conflictos.
4. **`TransactionRunner`** (en `application`). Es la forma de tener transacciones sin `@Transactional`
   en los servicios. Si se acepta, C y D lo usan igual.
5. **Persistencia de Product y Batch (A5).** `ProductEntity`, `BatchEntity` (con `@Version`), mappers y
   adaptadores, solo con las consultas de B. `findAvailableByProduct` ya viene en orden FIFO.
6. **Usuarios iniciales** (`V20261023_1000__usuarios_iniciales.sql`): `admin/admin123` y
   `cajero/cajero123`, en BCrypt **sin prefijo** `{bcrypt}`. A7 debe usar `BCryptPasswordEncoder`, o
   agregar el prefijo si prefiere el codificador delegante. Están en la migración base (no solo en
   demo) para que cualquiera pueda entrar siguiendo el README. Hay que cambiarlas si la app se
   despliega.
7. **Cajero sin login.** Mientras no exista A7, las ventas quedan a nombre de `cajero`
   (`PosController.DEFAULT_CASHIER`). Al agregar Spring Security se usará el `Principal` sin cambiar
   el controlador. *Actualizado en A5:* ya existe `UserEntity`, y `SaleRepositoryAdapter` busca al
   cajero con `UserJpaRepository`. Las consultas nativas sobre `app_user` se eliminaron.
   *Resuelto en A7:* se eliminó `DEFAULT_CASHIER`; la venta queda a nombre de quien inició sesión.
8. **Datos demo provisionales** (`db/demo/V20261023_1100__datos_demo_pos.sql`): 9 productos y sus
   lotes. C4 decide si los conserva y agrega los 80–100 productos y el historial en migraciones
   posteriores.

## Hechas para Administración → Productos (trabajo que era de D4)

9. **Cambios en puertos.** `ProductRepository.create` y `BatchRepository.create` (insertar), y un
    `SupplierRepository` nuevo de solo lectura (`findAll`, `findById`). El modelo `Supplier` es
    mínimo y aún no tiene el pedido mínimo (MOQ, FR-05).
10. **`CatalogService`** quedó en `application/admin`, donde el reparto ubica el trabajo de D, con su
    `AdminConfig`. D4 agrega ahí la edición y la inactivación.
11. **Reglas de validación**, todas en el dominio. Conviene confirmarlas con el usuario entrevistado:
    - Solo códigos GTIN (EAN-8, UPC-A, EAN-13, GTIN-14) con dígito de verificación válido. Es
      coherente con el alcance de la Entrega 1 ("productos con código estándar"). Los productos
      ya registrados con otros códigos se siguen encontrando.
    - Precios en **pesos enteros**.
    - **Precio de venta ≥ costo**. Si la tienda quiere registrar productos "gancho" por debajo del
      costo, esta regla se puede relajar a una advertencia.
12. **Stock inicial al registrar.** Crea un lote sin pedido (`order_line_id` vacío). Hoy no queda
    en la auditoría porque D3 aún no existe; cuando exista, registrarlo como `STOCK_ADJUSTED` (D5).
13. **Códigos demo corregidos** a EAN-13 válidos (migración demo aún no fusionada). C4 debe generar
    sus códigos con `GtinValidator.checkDigit`, como explica `db/demo/README.md`.

## Hechas con la interfaz nueva, el catálogo ampliado y el historial

14. **Más cambios en el dominio y en los puertos**:
    - `Product` ahora tiene tipo, marca, contenido, descripción y `saleEnabled`;
    - `ProductRepository` suma `findAll` y `update`;
    - `SaleRepository` suma `findItemsSoldBetween`, que C puede usar para la velocidad de ventas;
    - nuevo `PriceHistoryRepository`.
15. **Tipos de producto como lista fija** (`ProductCategory`, 13 tipos más "Otros"). Si la tienda
    quiere crear los suyos, hay que pasarlos a una tabla administrable.
16. **Venta pausada**: el producto queda en el inventario, pero no se vende. Hay que confirmar con
    el usuario entrevistado si además se necesita "ocultar" productos del inventario. Eso sería la
    inactivación de D4.
17. **"Por vencer" = 7 días o menos**, fijo en `ExpirationStatus.EXPIRING_SOON_DAYS`. Podría ser un
    parámetro de la tienda (D5).
18. **Generador de historial demo en Java** (`db/demo/V20261023_1400__HistorialVentasDemo.java`).
    Adelanta parte de C4 (historial de ventas para el ROP). C puede ajustar sus parámetros (días,
    ritmo de ventas) en lugar de escribir SQL a mano.
19. **Chart.js como WebJar** (librería de JavaScript servida desde el JAR, no una API externa). No
    cuenta como la "única API externa" que permite el curso, pero conviene mencionarlo en el
    documento de arquitectura.
20. **Ediciones simultáneas de un producto**: hoy gana la última. Si se quiere detectar el conflicto,
    el formulario debe llevar la versión (`@Version` ya existe en la tabla).

## Hechas en A2 (modelo y puertos completos)

23. **Transiciones del pedido en el dominio.** `OrderStatus.nextStatuses()` define las válidas y
    `PurchaseOrder` las aplica (`approve`, `markSent`, `receive`). D6 y D7 las usan desde sus
    servicios; si necesitan otra regla, se agrega aquí con su prueba.
    - Un pedido "por revisar" (sin proveedor) **no se puede aprobar** hasta asignarle uno.
    - Solo se recibe un pedido `SENT` o `PARTIALLY_RECEIVED`; no se puede recibir más de lo pendiente.
    - **"Cerrar" un pedido parcial** (US-05 esc. 2) no tiene estado propio. Si D6 lo necesita,
      hay que acordar si es un estado nuevo (p. ej. `CLOSED`) o si se marca `RECEIVED` con pendientes.
24. **`Supplier` cambió de forma**: suma `minimumOrder` (null si no exige mínimo). Quien cree
    proveedores (D4) debe pasar ese argumento.
25. **`MessageNotifier.send` devuelve `boolean` y nunca lanza.** Así una falla de Twilio no corta
    el flujo; quien lo llama crea una alerta `NOTIFICATION_FAILED`. *Actualizado en A6:* ya existe
    `LogMessageNotifier`.
26. **Interfaces de servicios.** El reparto pide "interfaces de puertos y servicios". Los servicios
    de `application` (como `SalesService`) son clases concretas que reciben los puertos por
    constructor; no se crearon interfaces para ellos porque solo tendrían una implementación.
27. **Sin adaptadores todavía** para los puertos nuevos: `UserRepository` es de A5 y
    `PurchaseOrderRepository`, `SettingsRepository`, `AuditLogRepository` y `AlertRepository` son
    de D2. Tampoco hay fakes en memoria para ellos; cada letra los agrega con sus pruebas.

## Hechas en A4 (README v1)

28. **`CLAUDE.md` tiene la instrucción de Windows incompleta.** Dice "usa `mvnw.cmd`", pero en
    PowerShell hace falta `.\mvnw.cmd`, y los argumentos `-D...` con puntos deben ir entre comillas.
    Sin comillas, el perfil demo no se aplica y no sale ningún error. El README ya lo explica.
    Corregir `CLAUDE.md` requiere el acuerdo de los cuatro, así que no se tocó.

## Hechas en A5 (persistencia de Product, Batch y User)

29. **`UserEntity` es de solo lectura** y guarda el rol como texto (`ADMIN` | `CASHIER`), igual que
    `SupplierEntity` con el tipo de pedido mínimo; `UserMapper` lo convierte a `Role`. Crear o
    cambiar usuarios desde la aplicación no está en el alcance de la Entrega 2. Si se necesita, se
    agregan la escritura y un método `save` al puerto.
30. **`findByUsername` devuelve también usuarios inactivos.** Así el login (A7) puede distinguir
    entre "no existe" e "inactivo"; Spring Security lo rechaza con `User.active()` (`disabled`).

## Hechas en A6 (notificador en el log)

31. **El notificador de log imita al proveedor real.** Devuelve `false` (y escribe una advertencia)
    si no hay número, si el número no está en formato internacional (`+` y de 8 a 15 dígitos) o si
    el mensaje está vacío. Así, en la demo se puede ver el camino de la alerta `NOTIFICATION_FAILED`
    sin Twilio.
32. **El número se enmascara en el log** (`+********4567`), porque es un dato personal; el mensaje
    se escribe completo, porque es lo que se muestra en la demo.
33. **Cambio a Twilio en la Entrega 3.** `LogMessageNotifier` es un `@Component` sin condiciones.
    Cuando exista `TwilioMessageNotifier`, uno de los dos debe ser condicional (por ejemplo
    `@ConditionalOnProperty` según haya credenciales), para que Spring no encuentre dos beans del
    mismo puerto.

## Hechas en A7 (login y roles)

34. **Todo es del administrador salvo lo que se abra a propósito.** `SecurityConfig` deja públicos
    `/login`, `/error` y los estáticos; `/pos/**` para los dos roles; `/` para cualquier usuario con
    sesión (al cajero lo redirige a Ventas) y **cualquier otra ruta solo para ADMIN**. Las pantallas
    nuevas de C (`/replenishment`) y D (`/purchasing`, `/admin/...`) quedan protegidas sin tocar la
    configuración. Si alguna debe verla el cajero, hay que abrirla en `SecurityConfig`.
35. **Redirección por rol en el inicio, no en el login.** Después de entrar, todos van a `/` (o a la
    página que pidieron). `HomeController` manda al cajero a `/pos`. Así, el enlace del logo tampoco
    le da un 403 al cajero.
36. **`BCryptPasswordEncoder` directo**, porque los hashes de la migración no tienen prefijo
    `{bcrypt}` (punto 6). Si se quiere el codificador delegante, hay que hacer una migración que
    agregue el prefijo.
37. **CSRF activo** en todos los formularios. Los que usan `th:action` reciben el token solos. Un
    formulario POST sin `th:action` responde 403, así que C y D deben usar siempre `th:action`.
38. **Un solo mensaje para cualquier fallo de login** ("Usuario o contraseña incorrectos"), también
    para usuarios inactivos. Así no se revela qué usuarios existen.
39. **Pruebas web con usuario simulado.** Las pruebas de aceptación usan
    `@WithMockUser` y `.with(csrf())` en cada POST. Dos pruebas de inventario de
    `SalesAcceptanceTest` corren como admin, porque el inventario ya no es del cajero.
40. **Sesión con los valores de Spring**: 30 minutos de inactividad, nueva sesión al entrar (contra
    fijación de sesión) y sin "recordarme". Si la tienda quiere sesiones más largas para el cajero,
    se cambia `server.servlet.session.timeout`.

## Hechas en A8 (pruebas de seguridad)

41. **El barrido de seguridad lee las rutas de Spring, no de una lista.** `SecurityRoutesTest` revisa
    cada ruta registrada con cuatro casos: sin sesión, cajero, administrador y POST sin CSRF. Una
    pantalla nueva de C o D queda probada sola. Si una pantalla nueva debe verla el cajero, hay que
    cambiar **dos** cosas: `SecurityConfig` y el método `isCashierRoute` de la prueba. Así, abrirle algo
    al cajero siempre es una decisión explícita y visible en el PR.
42. **El costo mínimo de BCrypt queda en 10** (el valor por defecto). La prueba falla si algún usuario
    se guarda con un costo menor.

## Hechas en A9 (JAR en máquina limpia)

43. **La "máquina limpia" es la del CI.** No hay Docker en los equipos, así que la prueba se hace
    de dos formas:
    - en local, con `scripts/smoke-test-jar.sh`, que corre el JAR en una carpeta vacía fuera del
      repositorio;
    - en GitHub Actions, en cada PR, en una máquina recién creada donde solo se instala Java 21.

    Falta una prueba manual en el computador de otro integrante antes de la entrega (calendario: día
    de integración).
44. **El JAR conserva el nombre con la versión** (`inventiq-0.1.0-SNAPSHOT.jar`). La Entrega 1 lo
    llama `inventiq.jar` en el diagrama de despliegue. Se podría fijar con `<finalName>`, pero el
    comando de `CLAUDE.md` (`target/inventiq-*.jar`) dejaría de coincidir, y cambiar `CLAUDE.md`
    requiere el acuerdo de los cuatro. Para la etiqueta v0.2 (A10) conviene decidir si la versión
    del `pom.xml` pasa a `0.2.0`.
45. **El JAR de `main` se guarda 30 días** como artefacto del CI (`inventiq-jar`), para descargarlo
    en la demo sin compilar.

## Hechas en A11 (documento de arquitectura)

46. **El documento vive en `docs/arquitectura.md`, con diagramas en Mermaid.** Mermaid es texto, así que
    los diagramas se revisan en el PR igual que el código y GitHub los muestra como imagen. Si la entrega
    se sube como PDF, hay que exportarlo desde GitHub o desde una vista previa que dibuje Mermaid.
47. **El documento describe el código actual**, no el plan. Lo que falta (servicios de C y D,
    adaptadores de D2) aparece como "en construcción" o "pendiente". Después del día de integración
    (29 oct) hay que actualizar las tablas de servicios, puertos y estado de las reglas.
48. **Las secciones de B, C y D** (10.2 a 10.4) quedan con lo mínimo que debe tener cada una. Según el
    reparto, A11 se cierra cuando estén completas y otra letra haya revisado el documento.

## Abiertas

21. ~~Restringir `/inventory` y `/admin/**` al rol ADMIN~~. *Resuelto en A7* (punto 34).
22. **Historial de ventas para C.** Ya existe `SaleRepository.findItemsSoldBetween(desde, hasta)`,
    que devuelve las líneas vendidas con su fecha. `SalesVelocityCalculator` puede partir de ahí
    para la ventana de 30 días; falta que C confirme si le sirve tal cual.
