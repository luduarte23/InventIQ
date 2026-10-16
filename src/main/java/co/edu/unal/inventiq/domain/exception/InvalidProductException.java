package co.edu.unal.inventiq.domain.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Los datos de un producto no cumplen las reglas. Trae los errores por campo para el formulario. */
public class InvalidProductException extends BusinessException {

    private final Map<String, String> fieldErrors;

    public InvalidProductException(Map<String, String> fieldErrors) {
        super("Revisa los datos del producto: " + String.join(" ", fieldErrors.values()));
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /** Clave: nombre del campo (componente de NewProduct). Valor: mensaje para el usuario. */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
