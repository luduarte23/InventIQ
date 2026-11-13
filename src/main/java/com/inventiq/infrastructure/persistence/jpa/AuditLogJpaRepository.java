package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.AuditLogEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la consulta a partir del nombre del método. */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    /** Registros del más reciente al más antiguo; Pageable limita cuántos se traen. */
    List<AuditLogEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
