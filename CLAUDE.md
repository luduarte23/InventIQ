# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Reglas para Claude (obligatorias)

- **Nunca hagas `git commit` ni `git push`; el usuario hace los commits.** Tampoco crees ramas, etiquetes ni abras PR salvo que te lo pidan explícitamente. Cada integrante debe hacer sus propios commits bajo su identidad (lo exige el curso).
- El equipo trabaja con Claude Code en modo **Plan o Manual**, nunca en edición automática: propón cambios pequeños y revisables, porque cada persona debe leer y entender el diff (en la defensa individual pueden preguntarle por cualquier parte del código).
- Cambiar este `CLAUDE.md`, el paquete `domain` o las interfaces de puertos requiere acuerdo de los 4 integrantes: si una tarea lo implica, avísalo antes de tocarlos.
- Fuentes de verdad: `docs/` (lineamientos del curso, Entrega 1 con requerimientos y arquitectura, reparto de la Entrega 2). `ACUERDOS.md` (cuando exista) define el esquema de tablas (§3) y otros acuerdos.
- **Mantén `docs/desarrollo/` al día** en cada bloque de trabajo: agrega una entrada en `bitacora.md`, actualiza `estado-actual.md` y, si tomaste una decisión que el equipo debe revisar, anótala en `decisiones-pendientes.md`. Los módulos tienen su propio archivo (p. ej. `parte-b-pos-inventario.md`).

## Descripción del proyecto

**InventIQ**: motor de decisiones de inventario para tiendas pequeñas (un solo punto de venta). Proyecto de Ingeniería de Software I (UNAL). La aplicación corre del lado del proveedor; el usuario solo usa un navegador.

Lo que el software decide en lugar de un humano (y lo que se califica como lógica de negocio):
- **Venta con deducción FIFO** (FR-02, US-03): el cajero escanea un código (lector USB tipo teclado o entrada manual) y se descuenta del lote que vence primero, luego del recibido primero; los lotes vencidos se excluyen. El código de barras no distingue lotes.
- **Punto de reorden (ROP)** (FR-03): a partir de la velocidad de ventas (ventana de 30 días), el plazo del proveedor y el stock de seguridad. Cantidad sugerida redondeada hacia arriba, mínimo 1; sin historial se usa el stock de seguridad.
- **Borradores de pedido por proveedor** (FR-04, US-01): un borrador por proveedor; los productos sin proveedor van a un pedido "por revisar" con alerta; no se duplican borradores abiertos.
- **Estados del pedido**: `DRAFT → APPROVED → SENT → PARTIALLY_RECEIVED → RECEIVED`, solo transiciones válidas. La recepción (FR-10, US-05) crea un lote por línea con su vencimiento; la recepción parcial deja pendientes visibles.
- Posteriores (Entrega 3): completar mínimo de pedido/MOQ (FR-05), priorizar bajo presupuesto semanal (FR-06), descuentos dinámicos por vencimiento (FR-07), mensaje al proveedor por WhatsApp/Twilio (FR-09).

Roles: **administrador** (todo) y **cajero** (solo ventas; recibe 403 en rutas de admin). Fuera de alcance: hardware POS, crédito/fiado, multisede, apps móviles/offline, productos sin código de barras.

Alcance de la Entrega 2 (jueves 5 nov 2026): el ciclo login → venta → ROP → borrador → aprobación → recepción debe correr en la demo sin tocar la BD a mano.

## Stack tecnológico (obligatorio por el curso)

- Java 21, Spring Boot con servidor embebido, vistas **Thymeleaf** renderizadas en el servidor.
- Maven con wrapper (`mvnw` / `mvnw.cmd`) incluido en el repo.
- H2 en archivo, esquema versionado con **Flyway**, `spring.jpa.hibernate.ddl-auto=validate`.
- Spring Security (sesión, BCrypt, 2 roles), Spring Data JPA.
- Pruebas: JUnit, JaCoCo (umbral 80% sobre el dominio), ArchUnit, `@DataJpaTest`, `@WebMvcTest`. CI en GitHub Actions en cada PR.
- Única API externa permitida: Twilio (WhatsApp), siempre detrás de un adaptador con implementación falsa. En la Entrega 2 solo existe `LogMessageNotifier`; Twilio se activa en la Entrega 3 y únicamente si hay credenciales.

Restricciones duras: monolito (un repo, un JAR, un proceso); **sin SPA, sin `@RestController` ni API REST interna**; la app arranca con un solo comando sin instalar servidores ni configurar variables de entorno.

## Estructura de carpetas propuesta

Un solo proyecto Maven (no multimódulo), paquete raíz `co.edu.unal.inventiq`, organizado por capas y, dentro de cada capa, por módulo funcional:

```
src/main/java/co/edu/unal/inventiq/
├── domain/              # Java puro: sin Spring, sin JPA
│   ├── model/           # Product, Batch, Sale, SaleItem, Supplier, PurchaseOrder, OrderLine, OrderStatus, Settings, Alert...
│   ├── rules/           # FifoAllocator, SalesVelocityCalculator, ReorderPointCalculator, DraftOrderGrouper
│   ├── port/            # ProductRepository, BatchRepository, SaleRepository, MessageNotifier...
│   └── exception/       # UnknownBarcodeException, InsufficientStockException
├── application/         # casos de uso que orquestan dominio + puertos
│   └── sales/ replenishment/ purchasing/ admin/
├── infrastructure/      # implementa los puertos
│   ├── persistence/     # entity/ (@Entity), jpa/ (Spring Data), mapper/, adapter/
│   ├── notification/    # LogMessageNotifier, TwilioMessageNotifier (E3)
│   ├── security/        # SecurityConfig, UserDetailsService
│   └── config/          # Clock y beans generales
└── web/                 # @Controller + formularios
    └── auth/ pos/ inventory/ replenishment/ purchasing/ admin/
src/main/resources/
├── application.properties        # H2 en archivo, ddl-auto=validate
├── application-demo.properties   # agrega db/demo a las ubicaciones de Flyway
├── db/migration/                 # migraciones Flyway
├── db/demo/                      # datos de demostración (perfil demo)
├── templates/                    # layout.html, fragments/, una carpeta por módulo web
└── static/css/ static/js/        # JS mínimo (foco del campo del escáner)
src/test/java/co/edu/unal/inventiq/
├── domain/rules/                 # pruebas unitarias puras (cuentan para el 80%)
├── application/                  # servicios probados con fakes, sin Spring
├── fakes/                        # InMemoryProductRepository, FakeMessageNotifier...
├── infrastructure/persistence/   # @DataJpaTest contra H2
├── web/                          # @WebMvcTest y pruebas de seguridad
└── architecture/                 # ArchUnit
docs/arquitectura.md              # capas, modelo de dominio, dependencias (entregable)
```

Dependencias en un solo sentido: `web → application → domain ← infrastructure`. El dominio no conoce a nadie; la infraestructura implementa sus puertos. Una prueba ArchUnit falla si `domain` importa `org.springframework` o `jakarta.persistence`.

## Comandos

En Windows usa `mvnw.cmd` en lugar de `./mvnw` (en Git Bash funciona `./mvnw`).

```bash
./mvnw spring-boot:run                                         # levantar la app (crea/migra la BD H2 en archivo)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo         # con datos de demostración
./mvnw test                                                    # todas las pruebas
./mvnw test -Dtest=FifoAllocatorTest                           # una clase de prueba
./mvnw test -Dtest=FifoAllocatorTest#nombreDelMetodo           # un solo método
./mvnw verify                                                  # pruebas + verificación de cobertura JaCoCo (falla si el dominio < 80%)
./mvnw package && java -jar target/inventiq-*.jar              # empaquetar y ejecutar el JAR
```

Si estos comandos cambian, actualiza esta sección y el README a la vez. La base local queda en `./data/` (ignorada por git); bórrala para empezar de cero.

### Versiones y particularidades de Spring Boot 4

- Spring Boot **4.1.1** (Spring Framework 7, Spring Security 7, Hibernate 7, Jakarta EE 11), Maven 3.9.16 vía wrapper, compilado con `java.version=21`.
- Boot 4 es modular: cada tecnología tiene su starter (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`...) y las pruebas por capa necesitan su propio starter de test. Al usar `@WebMvcTest` agrega `spring-boot-starter-webmvc-test`; para `@DataJpaTest`, `spring-boot-starter-data-jpa-test`; para pruebas de seguridad, `spring-boot-starter-security-test`. Sus anotaciones están en paquetes nuevos (p. ej. `org.springframework.boot.webmvc.test.autoconfigure`), no en los de Boot 3 que aparecen en tutoriales.
- La consola web de H2 no viene incluida (en Boot 4 es un módulo aparte); no la agregues sin acordarlo.
- `LayerDependencyTest` (ArchUnit) y la regla de JaCoCo del `pom.xml` hacen cumplir las capas y el 80%: si fallan, corrige el código, no la regla.

## Convenciones de código

- **Dos modelos separados**: `Product` (dominio) y `ProductEntity` (persistencia), con mappers en `infrastructure/persistence/mapper`. Nunca pongas anotaciones JPA ni de Spring en `domain`.
- **Reglas de negocio en el dominio**, no en controladores ni servicios: FIFO, ROP, agrupación de borradores y transiciones de estado del pedido viven en `domain` con sus propias pruebas.
- **Servicios de `application` sin anotaciones en el constructor**: reciben los puertos por constructor para que las pruebas los armen a mano con los fakes de `test/.../fakes/`. El cableado con Spring va en `infrastructure/config`.
- **Fecha inyectada**: todo lo que dependa de "hoy" (ROP, vencimientos) recibe un `java.time.Clock`; nunca `LocalDate.now()` sin reloj.
- **Transacciones**: los servicios de `application` no usan `@Transactional`; reciben `application.TransactionRunner` (en producción `SpringTransactionRunner`, en pruebas `fakes.DirectTransactionRunner`). Los conflictos de concurrencia llegan como `ConcurrentStockUpdateException`.
- **Errores de negocio**: las excepciones de `domain.exception` heredan de `BusinessException` y su mensaje, en español, se muestra tal cual al usuario. Los controladores las atrapan y redirigen con un flash `error`.
- **Cableado**: cada letra registra sus servicios en su propia clase `infrastructure.config.*Config` (p. ej. `SalesConfig`) para no editar el mismo archivo.
- **Formularios**: un `*Form` en `web` con campos tipados y los mismos nombres que el objeto del dominio. Los errores de conversión (letras en un número) tienen su mensaje en `messages.properties`. Las reglas de negocio las valida el dominio, que devuelve un mapa campo → mensaje (ver `NewProductValidator` e `InvalidProductException`), y el controlador las marca con `rejectValue`. Si hay errores de conversión, no se guarda nada. Ejemplo completo: `ProductAdminController`.
- **Códigos de barras**: GTIN con dígito de verificación (`GtinValidator`). Los datos demo y de prueba que pasen por la pantalla de productos deben usar códigos válidos (`GtinValidator.checkDigit`).
- **Menú**: los módulos nuevos se agregan a la barra lateral en `templates/layout.html` (C y D reemplazan los ítems "Pronto"); las subpestañas de Administración van en `templates/fragments/admin-nav.html`.
- **Interfaz** (detalle en `docs/desarrollo/interfaz.md`): reutiliza las piezas existentes en lugar de crear estilos nuevos.
  - Íconos: `fragments/icons.html`.
  - Fechas de vencimiento en Día/Mes/Año con color y etiqueta: `fragments/expiration.html`.
  - Formatos en español: `${@fmt...}` (`web/ViewFormat`).
  - Clases de `static/css/app.css`: `card`, `kpi-row`, `data-table`, `badge-*`, `btn`.
  - Un estado nunca se muestra solo con color: siempre con ícono y texto.
- **Gráficas** (detalle en `docs/desarrollo/historial-analisis.md`):
  - Chart.js desde el WebJar.
  - Los datos van en la página como JSON, armados con mapas y listas; nada de API REST.
  - Nunca dos ejes Y en una gráfica, una serie por color, y "Ver tabla" en cada gráfica.
  - Colores de series solo desde las variables `--series-*` y `--ordinal-*`, que están validadas.
- **Web**: solo `@Controller` con vistas Thymeleaf y formularios; nunca `@RestController`. Todas las pantallas extienden `templates/layout.html`. Los errores de la pantalla de ventas no cortan la venta, y la entrada manual de código siempre está visible.
- **Concurrencia**: `@Version` (bloqueo optimista) en `Product` y `Batch`; las ventas reintentan ante `OptimisticLockException`, y el stock nunca puede quedar negativo.
- **Migraciones Flyway** con versión de fecha y hora para evitar choques: `V20261022_1430__descripcion.sql`. Nunca edites una migración ya fusionada ni cambies `ddl-auto=validate`.
- **Pruebas**: las reglas se escriben primero con su prueba (los escenarios Gherkin de US-01, US-03 y US-05 en la Entrega 1 son los casos de aceptación). Las pruebas de `domain` y `application` corren sin Spring ni base de datos.
- **Nombres**: clases y paquetes en inglés (los de arriba); textos de la interfaz en español. Los nombres en español de la Entrega 1 (`NotificadorMensajes`, `RepositorioInventario`, `VentasService`...) corresponden a `MessageNotifier`, los repositorios de `domain/port` y `SalesService`.
- **Flujo de trabajo**: una rama por tarea con el ID del reparto (`feature/B2-fifo`) y PR aprobado por otro integrante; nada directo a `main`.
- **README**: debe permitir que alguien ajeno al equipo clone y levante la app sin ayuda, e incluir cómo correr las pruebas y la sección de declaración de uso de IA. Mantenlo al día cuando cambies comandos o configuración.
