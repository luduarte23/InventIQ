package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.SaleEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data genera la implementación; save, findById y deleteById vienen de JpaRepository. */
public interface SaleJpaRepository extends JpaRepository<SaleEntity, Long> {

    /**
     * Suma las unidades vendidas de un producto desde una fecha, contando solo ventas
     * cerradas (una venta abierta o cancelada todavía no es una venta real).
     * Esta suma no se puede expresar con el nombre del método, por eso usa @Query.
     */
    @Query("select coalesce(sum(i.quantity), 0) from SaleItemEntity i "
            + "where i.productId = :productId and i.sale.closedAt is not null "
            + "and i.sale.createdAt >= :since")
    Long sumUnitsSoldSince(@Param("productId") Long productId, @Param("since") Instant since);
}
