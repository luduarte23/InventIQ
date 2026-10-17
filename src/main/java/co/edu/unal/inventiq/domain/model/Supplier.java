package co.edu.unal.inventiq.domain.model;

/**
 * Proveedor. Versión mínima de solo lectura para elegirlo al registrar un producto; D4
 * agrega el pedido mínimo (MOQ) y la gestión de proveedores.
 *
 * @param leadTimeDays plazo de entrega en días, que usa el cálculo del ROP (C)
 */
public record Supplier(Long id, String name, String whatsapp, int leadTimeDays, boolean active) {
}
