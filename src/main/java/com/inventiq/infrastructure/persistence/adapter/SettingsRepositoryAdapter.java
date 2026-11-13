package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Settings;
import com.inventiq.domain.port.SettingsRepository;
import com.inventiq.infrastructure.persistence.entity.SettingsEntity;
import com.inventiq.infrastructure.persistence.jpa.SettingsJpaRepository;
import org.springframework.stereotype.Repository;

/** Implementa el puerto SettingsRepository del dominio usando JPA (A5). */
@Repository
public class SettingsRepositoryAdapter implements SettingsRepository {

    private final SettingsJpaRepository jpa;

    public SettingsRepositoryAdapter(SettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** La fila la crea la migración V2. Si falta, la base no está bien migrada. */
    @Override
    public Settings get() {
        return jpa.findById(Settings.SINGLETON_ID)
                .map(SettingsEntity::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la fila de settings: revisa que Flyway aplicó la migración V2"));
    }

    @Override
    public Settings save(Settings settings) {
        return jpa.save(SettingsEntity.fromDomain(settings)).toDomain();
    }
}
