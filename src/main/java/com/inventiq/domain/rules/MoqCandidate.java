package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Producto del mismo proveedor que todavía no está en el pedido y que se puede agregar
 * para alcanzar el pedido mínimo. Solo entran productos con historial de ventas.
 */
public record MoqCandidate(Long productId, BigDecimal unitsPerDay, BigDecimal unitCost) {

    public MoqCandidate {
        Objects.requireNonNull(productId, "productId es obligatorio");
        Objects.requireNonNull(unitsPerDay, "unitsPerDay es obligatorio");
        Objects.requireNonNull(unitCost, "unitCost es obligatorio");
    }
}
