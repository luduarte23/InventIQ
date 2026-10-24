# Decisiones pendientes

Lo que el equipo debe revisar. Cambiar el dominio o los puertos requiere el acuerdo de los cuatro
integrantes.

## Tomadas para que la Parte B funcionara (trabajo de A)

B depende de A2, A5 y A7. Para no bloquearla se hizo lo mínimo:

1. **Esquema V1.** Se dedujo de los documentos de la Entrega 1 y del reparto. Hay que compararlo
   con `ACUERDOS.md` §3. *Responsable: A.*
2. **Modelo y puertos (A2).** `Product`, `Batch`, `Sale`, `SaleItem` y `Cart`, y los puertos
   `ProductRepository`, `BatchRepository` y `SaleRepository`, solo con lo que usa B. `Product` es de
   solo lectura: D4 agrega el registro y la edición.
3. **`TransactionRunner`** (en `application`): transacciones sin `@Transactional` en los servicios,
   para poder probarlos con fakes. Si se acepta, C y D lo usan igual.
4. **Persistencia (A5).** Entidades, mappers y adaptadores con `@Version` en `Batch` (bloqueo
   optimista). `findAvailableByProduct` ya devuelve los lotes en orden FIFO.
5. **Usuarios iniciales** en la migración base: `admin/admin123` y `cajero/cajero123`, en BCrypt
   **sin prefijo** `{bcrypt}`. A7 debe usar `BCryptPasswordEncoder`.
6. **Cajero sin login.** Mientras no exista A7, las ventas quedan a nombre de `cajero`. Con Spring
   Security se usará el usuario autenticado sin cambiar el controlador.
7. **Datos demo provisionales** (9 productos). C4 decide si los conserva.

## Otras

8. **"Por vencer" = 7 días o menos**, fijo en `ExpirationStatus`. Podría ser un parámetro (D5).
9. **`CLAUDE.md` no se versiona** (está en `.gitignore`): cada integrante tiene su copia local de
   las reglas para Claude Code. Si el equipo quiere compartir las mismas reglas, hay que volver a
   subirlo.

## Abiertas

10. Restringir `/inventory` al rol ADMIN cuando exista el login (A7).
11. **Historial de ventas para C.** `SalesVelocityCalculator` necesita las ventas de los últimos 30
    días: se puede agregar una consulta a `SaleRepository`, acordándola entre B y C.
