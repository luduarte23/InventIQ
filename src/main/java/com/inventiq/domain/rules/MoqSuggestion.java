package com.inventiq.domain.rules;

import java.math.BigDecimal;

/** Una línea que se sugiere agregar al pedido: producto, unidades y su costo. */
public record MoqSuggestion(Long productId, int quantity, BigDecimal lineCost) {
}
