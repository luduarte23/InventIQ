package co.edu.unal.inventiq.domain.model;

/**
 * Proveedor. D4 agrega su gestión (crear, editar, inactivar).
 *
 * @param leadTimeDays plazo de entrega en días, que usa el cálculo del ROP (C)
 * @param minimumOrder null si el proveedor no exige pedido mínimo
 */
public record Supplier(Long id, String name, String whatsapp, int leadTimeDays, MinimumOrder minimumOrder,
                       boolean active) {
}
