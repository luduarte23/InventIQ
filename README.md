# InventIQ

<p align="left">
  <img src="https://img.shields.io/badge/Java-21%20LTS-red?style=for-the-badge&logo=openjdk">
  <img src="https://img.shields.io/badge/Spring%20Boot-Monolito-success?style=for-the-badge&logo=springboot">
  <img src="https://img.shields.io/badge/Estado-En%20desarrollo%20(ENTREGA%202)-yellow?style=for-the-badge">
  <img src="https://img.shields.io/badge/UNAL-Proyecto%20Universitario-green?style=for-the-badge">
  <a href="https://github.com/luduarte23/InventIQ/actions/workflows/ci.yml"><img src="https://github.com/luduarte23/InventIQ/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"></a>
</p>

> ## Ingeniería de Software I (2016701) - Sede Bogotá
>
> Docente: Magda Lucía Mejía Torres
>
> Entrega 1 - Requisitos y Arquitectura | Fecha de entrega: 05/10/2026
>
> Entrega 2 - Incremento 1 | En curso
>
> Repositorio: https://github.com/luduarte23/InventIQ

---

## Descripción

**InventIQ** es un motor automatizado de decisiones operativas para pequeños negocios de venta al por menor.

Hoy los dueños y administradores de tienda dedican varias horas a la semana a revisar inventarios a mano, calcular cuánto reabastecer y estimar cuándo pedir a cada proveedor, apoyándose en la intuición, hojas de cálculo o listas escritas a mano. Esto genera pérdidas por productos vencidos, faltantes de los productos más vendidos y pedidos a proveedores descoordinados que no alcanzan descuentos por volumen ni umbrales de envío gratis.

InventIQ registra las ventas en tiempo real mediante lectura de código de barras con **descuento de inventario FIFO**, y analiza de forma continua el historial de ventas, los tiempos de entrega de los proveedores, el flujo de caja disponible y las fechas de vencimiento de los lotes. Con esa información calcula de forma autónoma **puntos de reorden (ROP) óptimos**, prioriza las compras dentro del presupuesto y calcula **escalas de descuento dinámicas** para la mercancía próxima a vencer.

---

## Alcance

### Dentro del alcance

- **Registro de ventas en tiempo real y lectura de código de barras** con lector USB (modo teclado) o ingreso manual, descontando automáticamente las unidades del lote más antiguo (FIFO).
- **Historial de transacciones** (ventas y compras) como fuente de datos para el análisis de velocidad de ventas y rotación.
- **Cálculo dinámico del punto de reorden (ROP)**, a partir de la velocidad histórica de ventas, el tiempo de entrega del proveedor y el stock de seguridad.
- **Consolidación de pedidos por proveedor y borradores**, agrupando los productos con stock bajo por proveedor en órdenes de compra estructuradas y editables.
- **Optimización del pedido mínimo (MOQ)**, sugiriendo productos adicionales de alta rotación para alcanzar los umbrales de envío gratis o descuento por volumen.
- **Priorización de compras bajo un presupuesto**, ordenando los pedidos por margen y rotación cuando el costo total supera el flujo de caja semanal disponible.
- **Descuentos dinámicos por vencimiento**, aplicando escalas de descuento progresivas a los lotes perecederos según su fecha de vencimiento y su ritmo de venta.
- **Integración con WhatsApp (Twilio)**: los pedidos y alertas se generan en pantalla, con envío opcional a la cuenta de WhatsApp del administrador. En el curso se usa el Sandbox de Twilio; pasar a producción solo requiere una suscripción paga y configurar la cuenta, sin cambios de código.

### Fuera del alcance

- Periféricos de POS complejos (impresoras térmicas, cajones monederos, datáfonos).
- Módulo de crédito a clientes / evaluación de riesgo del "fiado".
- Gestión de varias sucursales y bodegas (un solo inventario).
- Aplicaciones móviles nativas y operación sin conexión (solo aplicación web renderizada en el servidor).
- Mecanismos alternativos de identificación de productos (se requiere un código de barras estándar).

---

## Interesados y roles de usuario

| Rol                              | Descripción                                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Administrador                    | Dueño o administrador de la tienda. Accede al panel, configura proveedores, umbrales de inventario y precios.       |
| Cajero                           | Empleado de la tienda. Usa la pantalla de ventas para escanear productos o escribir el código a mano.               |
| Lector de código de barras       | Dispositivo USB opcional; envía el código como texto (modo teclado).                                                |
| Twilio (API de WhatsApp)         | Da formato y entrega las alertas del sistema a la cuenta de WhatsApp del administrador.                             |

---

## Arquitectura

**Estilo seleccionado:** arquitectura en capas (N-Tier) con el patrón **MVC**, organizada en cuatro capas desacopladas cuyas dependencias apuntan solo hacia abajo:

1. **Presentación (MVC)** - Controladores y vistas del POS, compras y administración; el MOD-01 intercepta cada petición para validar la sesión y el rol.
2. **Aplicación** - Orquesta los casos de uso mediante servicios por módulo (`VentasService`, `ReabastecimientoService`, `DescuentosService`, `ComprasService`).
3. **Dominio (puro)** - Reglas de negocio (FIFO, ROP, MOQ, presupuesto, descuentos), sin ninguna dependencia de infraestructura; solo define las interfaces que necesita (`RepositorioInventario`, `NotificadorMensajes`).
4. **Infraestructura y despliegue (MOD-06)** - Implementa esas interfaces: persistencia en base de datos embebida, migraciones versionadas del esquema y el adaptador de Twilio.

En el código las clases tienen nombres en inglés. Por ejemplo, `VentasService` es `SalesService`, `NotificadorMensajes` es `MessageNotifier`, y `RepositorioInventario` se divide en un puerto por entidad (`ProductRepository`, `BatchRepository`, `SaleRepository`, etc.). Ver [Estructura del proyecto](#estructura-del-proyecto).

La arquitectura tal como está construida (capas, módulos, modelo de dominio, persistencia, seguridad, decisiones y cómo se verifica) está en [`docs/arquitectura.md`](docs/arquitectura.md).

**Compromisos aceptados:**

- _Escalabilidad horizontal vs. simplicidad_ - un monolito renuncia a escalar cada componente por separado, algo aceptable porque el alcance apunta a negocios de una sola sucursal.
- _Acoplamiento tecnológico vs. velocidad de desarrollo_ - todas las capas comparten una misma plataforma, priorizando la velocidad del equipo sobre la flexibilidad de usar varios lenguajes.

### Módulos

| Módulo | Nombre                        | Responsabilidades                                                                                                    |
| ------ | ----------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| MOD-01 | Seguridad y acceso            | Autenticación, seguridad de la sesión, control de acceso por roles.                                                  |
| MOD-02 | POS e inventario              | Procesamiento de ventas en tiempo real con código de barras, descuento de lotes FIFO.                                |
| MOD-03 | Motor de reabastecimiento     | Cálculo del ROP, agrupación por proveedor, optimización del MOQ, priorización por presupuesto.                       |
| MOD-04 | Vencimientos y precios        | Seguimiento de la vida útil de los lotes, cálculo de descuentos progresivos por vencimiento.                         |
| MOD-05 | Compras y administración      | Ajustes manuales de parámetros, confirmación de recepción de pedidos, mensajes a proveedores, auditoría de decisiones. |
| MOD-06 | Infraestructura y despliegue  | Empaquetado, base de datos embebida, migraciones versionadas, ejecución con un solo comando.                         |

### Despliegue

La aplicación se empaqueta como un único JAR autocontenido (`inventiq.jar`) que solo necesita Java 21: incluye el servidor web y usa una base de datos embebida guardada en archivo. Ver [Ejecutar el JAR](#ejecutar-el-jar).

---

## Stack tecnológico

| Elemento       | Tecnología                                                                                                  |
| -------------- | ----------------------------------------------------------------------------------------------------------- |
| Lenguaje       | Java 21 (LTS)                                                                                               |
| Framework      | Spring Boot 4.1 (servidor embebido)                                                                         |
| Vistas         | Thymeleaf, renderizadas en el servidor                                                                      |
| Build          | Maven, con el wrapper (`./mvnw`) incluido                                                                   |
| Base de datos  | H2 embebida, guardada en archivo; esquema con migraciones versionadas de Flyway                             |
| Autenticación  | Spring Security: por sesión, restringida por rol (Administrador / Cajero), contraseñas con hash BCrypt      |
| API externa    | Sandbox de WhatsApp de Twilio, detrás de la interfaz `MessageNotifier` - _en desarrollo_ (por ahora, los mensajes se escriben en el log) |
| Pruebas        | JUnit, MockMvc con Spring Security Test, ArchUnit 1.5 y JaCoCo 0.8.16 (el build falla si la cobertura del dominio baja del 80 %) |
| CI             | GitHub Actions: `./mvnw verify` en cada pull request y en cada push a `main`; guarda el reporte de cobertura y el JAR ejecutable |

---

## Terminología

| Término                              | Definición                                                                                                                                                       |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ROP** (punto de reorden)           | Nivel crítico de inventario que dispara una sugerencia de compra automática; se calcula con la velocidad diaria de ventas, el tiempo de entrega y el stock de seguridad. |
| **FIFO**                             | Primero en entrar, primero en salir: los lotes más antiguos se descuentan y venden primero para evitar pérdidas por vencimiento.                                  |
| **MOQ** (pedido mínimo)              | Cantidad mínima de unidades o monto que exige un proveedor para procesar un pedido y dar beneficios como el envío gratis.                                        |
| **POS** (punto de venta)             | La interfaz de captura de ventas, donde cada lectura de código de barras descuenta inventario en tiempo real.                                                    |

---

## Declaración de uso de IA

Este proyecto usó asistencia de IA durante el desarrollo. El código asistido por IA pasa por pull requests como cualquier otro código, y cada integrante debe poder explicarlo.

| Herramienta        | Partes                                                                                                                                                                                                                                                                                  | Propósito                                                                                                                          |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Claude             | Entrega 1                                                                                                                                                                                                                                                                               | Validar la arquitectura frente a las restricciones obligatorias del stack                                                          |
| Claude (claude.ai) | Parte A: configuración de H2 y Flyway y migración `V1` (A1); entidades, enums, excepciones, puertos e interfaces de servicio del dominio (A2); perfil de pruebas, JaCoCo, pruebas unitarias del dominio, ArchUnit, workflow de GitHub Actions y protección de `main` (A3); este README (A4); persistencia JPA de usuarios, proveedores, pedidos, configuración, auditoría y alertas (A5); notificador simulado y perfil `demo` (A6); inicio de sesión con roles (A7); pruebas de seguridad (A8); JAR ejecutable (A9) | Guía paso a paso y generación de código y configuración. El integrante revisó, compiló y ejecutó cada cambio antes de hacer commit |
| Claude Code        | Parte B: regla FIFO, servicio de ventas, persistencia provisional de productos, lotes y ventas, pantalla de ventas y sus pruebas                                                                                                                                                        | Implementar las tareas B2 a B4, B6 y B10 del plan de trabajo. El autor revisó cada cambio antes de subirlo                         |
| _Por completar_    | Parte C: motor de reabastecimiento                                                                                                                                                                                                                                                      | _Por completar_                                                                                                                    |
| _Por completar_    | Parte D: compras y administración                                                                                                                                                                                                                                                       | _Por completar_                                                                                                                    |

Regla del equipo: la IA no hace commits ni push. Cada integrante revisa los cambios y los sube con su propia identidad.

---

## Galería

_¡En desarrollo!_

---

## Primeros pasos

### Requisitos

Solo **Java 21** o superior. No necesitas instalar Maven ni una base de datos: el proyecto incluye el wrapper de Maven y usa una base H2 embebida guardada en archivo.

Revisa tu versión con `java -version`.

### Ejecutar la aplicación

```bash
git clone https://github.com/luduarte23/InventIQ.git
cd InventIQ
./mvnw spring-boot:run          # Linux / macOS / Git Bash
.\mvnw.cmd spring-boot:run      # Windows (PowerShell)
```

Abre <http://localhost:8080> e inicia sesión (ver [Inicio de sesión y roles](#inicio-de-sesión-y-roles)). La primera ejecución descarga las dependencias, así que tarda unos minutos.

La base de datos se crea en `./data/` y Flyway aplica las migraciones al arrancar. Para empezar desde cero, detén la aplicación y borra la carpeta `data/`.

La consola de H2 está en <http://localhost:8080/h2-console> y solo abre con la sesión del administrador (JDBC URL `jdbc:h2:file:./data/inventiqdb`, usuario `sa`, sin contraseña).

**Con datos demo** (perfil `demo`, en una base aparte: `./data/inventiq-demo`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo          # Linux / macOS / Git Bash
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"    # Windows (PowerShell)
```

Los datos demo están en `src/main/resources/db/demo/R__datos_demo.sql` y solo se cargan con este perfil. Mientras se completan, en [`docs/parte-b-ventas.md`](docs/parte-b-ventas.md#probarlo-a-mano) hay un ejemplo de productos y lotes para cargar desde la consola de H2.

Los mensajes de WhatsApp todavía no se envían: el notificador por defecto (`inventiq.notifier=log`) los escribe en la consola con la etiqueta `[WhatsApp simulado]`.

### Ejecutar el JAR

El JAR ejecutable contiene la aplicación completa. Solo necesita **Java 21**; no requiere Maven ni el código fuente.

**Obtenerlo:**

- Compilándolo: `./mvnw clean verify` (en PowerShell, `.\mvnw.cmd clean verify`) genera `target/inventiq.jar`.
- Desde GitHub: en la pestaña _Actions_, abre la última ejecución en verde de `main` y descarga el artifact `inventiq-jar` (se conserva 14 días).

**Arrancarlo:**

```bash
java -jar inventiq.jar
```

Luego abre <http://localhost:8080> e inicia sesión.

La base de datos se crea en la carpeta `data`, **al lado de donde se ejecuta el JAR**. Ejecútalo siempre desde la misma carpeta; la copia de seguridad consiste en copiar la carpeta `data` con la aplicación detenida.

**Opciones** (se escriben después del nombre del JAR):

| Opción                              | Para qué                                |
| ----------------------------------- | --------------------------------------- |
| `--server.port=9090`                | Usar otro puerto                        |
| `--spring.profiles.active=demo`     | Datos de ejemplo, en una base separada  |
| `--spring.h2.console.enabled=false` | Desactivar la consola de H2             |
| `--spring.jpa.show-sql=false`       | No escribir el SQL en el log            |

**Instalación en la tienda:** antes del **primer** arranque, define las contraseñas iniciales con variables de entorno (PowerShell):

```powershell
$env:INVENTIQ_INITIALUSERS_ADMINPASSWORD = "una-clave-segura"
$env:INVENTIQ_INITIALUSERS_CASHIERPASSWORD = "otra-clave-segura"
java -jar inventiq.jar --spring.h2.console.enabled=false --spring.jpa.show-sql=false
```

Las contraseñas solo se toman al crear los usuarios en una base nueva; después quedan guardadas (con hash) en la base y las variables ya no son necesarias.

### Inicio de sesión y roles

La aplicación exige iniciar sesión. Al arrancar por primera vez se crean dos usuarios:

| Usuario  | Contraseña  | Rol           | Puede usar                                  |
|----------|-------------|---------------|---------------------------------------------|
| `admin`  | `admin123`  | Administrador | Todo, incluida la consola de H2             |
| `cajero` | `cajero123` | Cajero        | Solo la pantalla de ventas (`/pos`)         |

- Estas contraseñas son **solo para desarrollo y para la demo**. En producción se definen con las
  variables de entorno `INVENTIQ_INITIALUSERS_ADMINPASSWORD` e `INVENTIQ_INITIALUSERS_CASHIERPASSWORD`
  antes del primer arranque.
- Las contraseñas se guardan con hash BCrypt, nunca en texto.
- Toda pantalla nueva queda restringida al administrador por defecto. Las reglas de acceso están en
  `infrastructure/security/SecurityConfig.java`.
- Si después de actualizar el código el login no acepta estas contraseñas, borra la base local
  (`data/inventiqdb*`) y vuelve a arrancar.

### Pantallas

La interfaz de usuario está en español.

| Pantalla          | URL              | Quién entra            | Qué hace                                                                                                    |
| ----------------- | ---------------- | ---------------------- | ----------------------------------------------------------------------------------------------------------- |
| Inicio de sesión  | `/login`         | Todos                  | Pide usuario y contraseña. Después de entrar, lleva a la pantalla de ventas                                 |
| Ventas (POS)      | `/pos`           | Cajero y administrador | Vender con un lector USB o escribiendo el código; el stock se descuenta en orden FIFO. Permite terminar o cancelar la venta. La venta queda a nombre del usuario con sesión iniciada |
| Reabastecimiento  | `/replenishment` | Administrador          | Sugerencias de reabastecimiento y creación de borradores de pedido                                          |

Las demás pantallas (inventario, pedidos y productos) están en desarrollo.

### Ejecutar las pruebas

```bash
./mvnw test                                        # todas las pruebas
./mvnw test -Dtest=FifoAllocatorTest               # una clase
./mvnw test -Dtest=FifoAllocatorTest#methodName    # un método
./mvnw verify                                      # pruebas + cobertura del dominio + reglas de arquitectura
```

En PowerShell, usa `.\mvnw.cmd` y pon entre comillas el parámetro `-Dtest`: `.\mvnw.cmd test "-Dtest=FifoAllocatorTest"`.

El reporte de cobertura se genera en `target/site/jacoco/index.html`. El CI también lo guarda en cada ejecución como el artifact `reporte-jacoco`.

- Las reglas de negocio (paquete `domain`) se prueban sin Spring ni base de datos.
- Las pruebas usan una base H2 en memoria (perfil `test`), así que no tocan `./data` y se pueden ejecutar con la aplicación corriendo.
- `./mvnw verify` falla si la cobertura de líneas del paquete `domain` baja del 80 %.
- Una prueba de ArchUnit falla si el dominio importa Spring, JPA o Twilio, si llama a `LocalDate.now()` o `Instant.now()`, o si una capa depende de otra en la dirección equivocada.

### Pruebas de seguridad

Las reglas de acceso se verifican automáticamente en cada `verify`:

| Prueba | Qué comprueba |
|---|---|
| `AccessRulesTest` | Sin sesión todo lleva al login; el cajero solo usa `/pos`; el administrador entra a todo |
| `LoginFlowTest` | Login real con los usuarios iniciales, rechazos, cierre de sesión y token CSRF |
| `PosControllerCashierTest` | La venta queda a nombre del usuario con sesión iniciada |

Para probar una pantalla protegida con MockMvc se usa el ayudante `TestUsers`
(`src/test/java/com/inventiq/support`):

```java
@SpringBootTest
@AutoConfigureMockMvc   // org.springframework.boot.webmvc.test.autoconfigure
class MiPantallaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elAdminPuedeVerla() throws Exception {
        mockMvc.perform(get("/mi-pantalla").with(TestUsers.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void elAdminPuedeEnviarElFormulario() throws Exception {
        mockMvc.perform(post("/mi-pantalla").with(TestUsers.admin()).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
```

- `TestUsers.admin()` y `TestUsers.cashier()` hacen que la petición llegue con sesión iniciada.
- Todo `post` necesita además `.with(csrf())`; sin él responde 403.
- No usar `@WithMockUser`: los controladores esperan un `AuthenticatedUser`.
- Al agregar una pantalla nueva, añadir su ruta a `AccessRulesTest`.

### Estructura del proyecto

Un solo proyecto Maven organizado por capas (`com.inventiq`):

| Paquete          | Contenido                                                                                              |
| ---------------- | ------------------------------------------------------------------------------------------------------ |
| `domain.model`   | Entidades, enums y excepciones del negocio. Java puro.                                                 |
| `domain.rules`   | Reglas de negocio puras: FIFO, ROP, MOQ, presupuesto y descuentos.                                     |
| `domain.port`    | Interfaces que necesita el dominio: repositorios, notificador, auditoría y alertas.                    |
| `application`    | Servicios de cada módulo (`security`, `pos`, `pricing`, `replenishment`, `purchasing`).                |
| `infrastructure` | Persistencia JPA (entidades, repositorios y adaptadores), configuración y, más adelante, el adaptador de Twilio. |
| `web`, `presentation` | Controladores y vistas Thymeleaf: ventas y reabastecimiento en `web`; inicio de sesión en `presentation`. Las dos son la capa de presentación. |

Las dependencias van en una sola dirección: `presentación (web, presentation) → application → domain ← infrastructure`. La prueba de ArchUnit lo verifica en cada build.

La documentación está en [`docs/`](docs/): [`arquitectura.md`](docs/arquitectura.md) describe el sistema completo y [`parte-b-ventas.md`](docs/parte-b-ventas.md) el detalle de las ventas.

### Flujo de trabajo

- Una rama por tarea, con el código de la tarea en el nombre: `feature/A3/ci-coverage`.
- Cada integrante hace commits con su propia identidad.
- Todo entra a `main` por un pull request aprobado por otro integrante. Los cambios en `domain.model`, `domain.port` o en los acuerdos técnicos del equipo requieren la aprobación de los 4.
- `main` está protegida: no admite push directo ni force push, y un pull request solo se puede fusionar con el CI en verde y la rama al día con `main`.
- Antes de subir cambios, ejecuta `./mvnw verify`: es lo mismo que corre el CI.
- Las migraciones de Flyway se nombran `V<número>__descripcion.sql` (por ejemplo, `V1__esquema_inicial.sql`) y nunca se editan una vez fusionadas: un cambio de esquema es siempre una migración nueva.

---

## Equipo

| Nombre                            | Correo                  |
| --------------------------------- | ----------------------- |
| Luis Alejandro Duarte Daza        | luduarte@unal.edu.co    |
| Santiago Andrés Amézquita Becerra | samezquitab@unal.edu.co |
| Miguel Ángel Suárez Montiel       | migsuarezmo@unal.edu.co |
| Sebastián Camilo Parra Siabato    | separras@unal.edu.co    |
