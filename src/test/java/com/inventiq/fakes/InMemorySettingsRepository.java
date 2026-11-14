package com.inventiq.fakes;

import com.inventiq.domain.model.Settings;
import com.inventiq.domain.port.SettingsRepository;

/** Repositorio falso de la configuración: guarda la única fila en memoria. Solo para pruebas. */
public class InMemorySettingsRepository implements SettingsRepository {

    private Settings settings;

    public InMemorySettingsRepository(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Settings get() {
        return settings;
    }

    @Override
    public Settings save(Settings newSettings) {
        this.settings = newSettings;
        return newSettings;
    }
}
