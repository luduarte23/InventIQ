package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una línea vendida, con la fecha de su venta: es la materia prima del historial y del ROP. */
public record SoldItem(long saleId, LocalDateTime soldAt, long productId, int quantity, BigDecimal unitPrice) {

    public BigDecimal revenue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
