package co.edu.unal.inventiq.domain.port;

import co.edu.unal.inventiq.domain.model.Settings;

/** Puerto de los parámetros de la tienda (una sola fila). */
public interface SettingsRepository {

    /** Los parámetros actuales; si nunca se guardaron, {@link Settings#defaults()}. */
    Settings get();

    void save(Settings settings);
}
