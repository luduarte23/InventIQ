package co.edu.unal.inventiq.domain.exception;

/**
 * Base de las reglas de negocio violadas. El mensaje está pensado para mostrarse
 * tal cual al usuario, así que va en español y sin detalles técnicos.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
