package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la consulta a partir del nombre del método. */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
