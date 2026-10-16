package co.edu.unal.inventiq.infrastructure.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.unal.inventiq.infrastructure.persistence.entity.SaleEntity;

public interface SaleJpaRepository extends JpaRepository<SaleEntity, Long> {

    /** Filas: id de la venta, fecha, producto, cantidad y precio unitario. */
    @Query("""
            select s.id, s.createdAt, i.productId, i.quantity, i.unitPrice
            from SaleEntity s join s.items i
            where s.createdAt >= :from and s.createdAt < :to
            order by s.createdAt, s.id, i.id
            """)
    List<Object[]> findItemsSoldBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Consultas nativas sobre app_user mientras no exista la entidad de usuario (A7).
    @Query(value = "select id from app_user where username = :username", nativeQuery = true)
    Optional<Long> findUserIdByUsername(@Param("username") String username);

    @Query(value = "select username from app_user where id = :id", nativeQuery = true)
    Optional<String> findUsernameByUserId(@Param("id") Long id);
}
