package co.edu.unal.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Alert;

/** Puerto de alertas para el administrador. */
public interface AlertRepository {

    /** Crea la alerta si no tiene id o guarda sus cambios; la devuelve con su id. */
    Alert save(Alert alert);

    Optional<Alert> findById(long id);

    /** Alertas sin resolver, de la más reciente a la más antigua. */
    List<Alert> findUnresolved();
}
