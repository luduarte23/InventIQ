package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.SettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo necesita findById y save, que ya vienen de JpaRepository. */
public interface SettingsJpaRepository extends JpaRepository<SettingsEntity, Long> {
}
