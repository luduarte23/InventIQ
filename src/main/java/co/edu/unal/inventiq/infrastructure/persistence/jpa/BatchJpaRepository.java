package co.edu.unal.inventiq.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unal.inventiq.infrastructure.persistence.entity.BatchEntity;

public interface BatchJpaRepository extends JpaRepository<BatchEntity, Long> {

    /** Ya viene en orden FIFO; el dominio vuelve a ordenar, así que la regla no depende de esta consulta. */
    @Query("""
            select b from BatchEntity b
            where b.productId = :productId and b.quantityAvailable > 0
            order by b.expirationDate asc nulls last, b.receivedAt asc, b.id asc
            """)
    List<BatchEntity> findAvailableByProduct(@Param("productId") Long productId);

    List<BatchEntity> findByQuantityAvailableGreaterThan(int quantity);
}
