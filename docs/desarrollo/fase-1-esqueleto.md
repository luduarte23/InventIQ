# Fase 1: esqueleto del proyecto

Tareas del reparto: A1 (proyecto y base), A3 (calidad y CI), A4 (README) y D1 (plantilla).

## Stack

| Pieza | Versión | Nota |
|---|---|---|
| Java | 21 (`java.version` en el pom) | Funciona también con JDK más nuevos |
| Spring Boot | 4.1.1 | Spring Framework 7, Hibernate 7, Jakarta EE 11 |
| Maven | 3.9.16 vía wrapper | `mvnw` / `mvnw.cmd`; no hay que instalar Maven |
| Base de datos | H2 en archivo (`./data/inventiq`) | Se crea sola al arrancar |
| Migraciones | Flyway | `src/main/resources/db/migration` |
| Vistas | Thymeleaf | Sin SPA ni `@RestController` |
| Pruebas | JUnit 5, AssertJ, ArchUnit 1.5.1, JaCoCo 0.8.15 | |

Boot 4 es modular: cada tecnología tiene su starter (`spring-boot-starter-webmvc`,
`spring-boot-starter-flyway`...) y las pruebas por capa necesitan su starter de test
(`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`). Las anotaciones de prueba
cambiaron de paquete respecto a Boot 3, por ejemplo
`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.

## Configuración

- `application.properties`: H2 en `./data/inventiq`, Flyway sobre `db/migration`,
  `ddl-auto=validate` (Hibernate solo valida, nunca cambia el esquema) y `open-in-view=false`.
- `application-demo.properties`: usa otra base (`./data/inventiq-demo`) y agrega `db/demo` a
  Flyway. Así las migraciones de datos de demostración no se mezclan con la base normal.

## Migraciones

Se nombran con fecha y hora para que dos personas no choquen con el mismo número:
`V20261022_1430__descripcion.sql`. Una migración fusionada no se edita nunca; los cambios van en
una nueva.

| Migración | Contenido |
|---|---|
| `V20261017_1200__esquema_inicial.sql` | Las 11 tablas: `app_user`, `supplier`, `product`, `batch`, `sale`, `sale_item`, `purchase_order`, `order_line`, `settings`, `audit_log`, `alert` |
| `V20261023_1000__usuarios_iniciales.sql` | Usuarios `admin` y `cajero` (Parte B) |
| `db/demo/V20261023_1100__datos_demo_pos.sql` | Datos provisionales de demostración (Parte B) |
| `V20261023_1200__catalogo_ampliado.sql` | Tipo, marca, contenido, descripción y venta pausada en `product`; tabla `price_history` |
| `db/demo/V20261023_1300__catalogo_demo.sql` | Datos de los productos demo, 12 productos y 2 proveedores más, café con la venta pausada |
| `db/demo/V20261023_1400__HistorialVentasDemo.java` | Migración en Java: 90 días de ventas y seis meses de precios (semilla fija) |

Las migraciones en Java van en el paquete `db.demo` (`src/main/java/db/demo/`) porque Flyway las busca
en la misma ubicación que las SQL del perfil demo.

## Reglas que el build hace cumplir

- **JaCoCo**: el build falla si la cobertura de líneas del paquete `domain` baja de 80 %.
- **ArchUnit** (`LayerDependencyTest`):
  - `domain` no puede importar Spring, JPA ni Hibernate.
  - Las capas solo dependen en el sentido `web → application → domain ← infrastructure`. `web`
    puede leer el modelo de dominio para mostrarlo y `infrastructure.config` crea los servicios.

Las dos reglas se probaron a propósito con una clase que las violaba, y el build falló como debía.

## CI

`.github/workflows/ci.yml` corre `./mvnw -B verify` con Temurin 21 en cada PR y cada push a
`main` (y a mano desde la pestaña Actions). Además (A3):

- escribe en el resumen de la corrida una tabla de cobertura por capa (`domain`, `application`,
  `infrastructure`, `web`), que se ve desde el PR sin descargar nada;
- publica el reporte de JaCoCo como artefacto y, si una prueba falla, también los reportes de
  Surefire;
- cancela la corrida anterior si llega otro push al mismo PR y tiene permisos de solo lectura.

Los PR se abren con la plantilla `.github/pull_request_template.md`, que trae la lista de chequeo del
equipo.

### Protección de `main` (se configura en GitHub, no en el código)

La hace alguien con permiso de administrador del repositorio `luduarte23/InventIQ`:

1. *Settings → Branches → Add branch ruleset* (o *Add classic branch protection rule*), con la rama
   `main` como objetivo.
2. Activar **Require a pull request before merging**, con **1** aprobación requerida y
   *Dismiss stale pull request approvals when new commits are pushed*.
3. Activar **Require status checks to pass** y agregar el check **`build`** (aparece como
   `CI / build` después de la primera corrida del workflow). Marcar *Require branches to be up to
   date before merging*.
4. Activar **Block force pushes** y dejar la eliminación de `main` bloqueada.
5. Guardar y comprobarlo: un `git push` directo a `main` debe ser rechazado.

## Plantilla base

`templates/layout.html` define un fragmento con parámetros. Cada página lo usa así:

```html
<html th:replace="~{layout :: layout(~{::title}, ~{::main})}">
```

Los mensajes se envían desde el controlador con
`redirectAttributes.addFlashAttribute("success" | "error", texto)` y los muestra
`fragments/messages.html`.
