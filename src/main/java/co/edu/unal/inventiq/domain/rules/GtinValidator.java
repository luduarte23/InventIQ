package co.edu.unal.inventiq.domain.rules;

import java.util.Optional;
import java.util.Set;

/**
 * Valida códigos de barras GS1 (GTIN): EAN-8, UPC-A (12 dígitos), EAN-13 y GTIN-14, que son
 * los "códigos estándar" que asume la Entrega 1. El último dígito es de verificación y se
 * calcula con los anteriores, así que un código mal digitado a mano casi siempre se detecta.
 */
public class GtinValidator {

    private static final Set<Integer> VALID_LENGTHS = Set.of(8, 12, 13, 14);

    /** @return el problema del código para mostrárselo al usuario, o vacío si es válido */
    public Optional<String> problemWith(String barcode) {
        String code = barcode == null ? "" : barcode.strip();
        if (code.isEmpty()) {
            return Optional.of("Escanea o escribe el código de barras.");
        }
        if (!code.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return Optional.of("El código solo puede tener números.");
        }
        if (!VALID_LENGTHS.contains(code.length())) {
            return Optional.of("El código debe tener 8, 12, 13 o 14 dígitos (EAN-8, UPC-A, EAN-13 o GTIN-14)"
                    + " y tiene " + code.length() + ".");
        }
        int expected = checkDigit(code.substring(0, code.length() - 1));
        int actual = code.charAt(code.length() - 1) - '0';
        if (expected != actual) {
            return Optional.of("El código " + code + " no es válido: el último dígito debería ser " + expected
                    + ". Revisa si quedó bien escrito.");
        }
        return Optional.empty();
    }

    /**
     * Dígito de verificación GS1. De derecha a izquierda, los dígitos se multiplican
     * alternadamente por 3 y por 1; el dígito es lo que le falta a la suma para llegar a
     * la siguiente decena. Sirve también para generar códigos válidos en los datos demo.
     */
    public static int checkDigit(String digitsWithoutCheck) {
        int sum = 0;
        for (int i = 0; i < digitsWithoutCheck.length(); i++) {
            int digit = digitsWithoutCheck.charAt(digitsWithoutCheck.length() - 1 - i) - '0';
            sum += i % 2 == 0 ? digit * 3 : digit;
        }
        return (10 - sum % 10) % 10;
    }
}
