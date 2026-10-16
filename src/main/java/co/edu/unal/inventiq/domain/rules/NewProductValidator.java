package co.edu.unal.inventiq.domain.rules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NewProduct;
import co.edu.unal.inventiq.domain.model.ProductData;

/**
 * Reglas para registrar o editar un producto. Devuelve todos los errores a la vez, por campo,
 * para que el formulario los muestre juntos. Lo que necesita consultar datos guardados (código
 * repetido, proveedor existente) lo revisa CatalogService.
 */
public class NewProductValidator {

    static final int MAX_NAME_LENGTH = 150;
    static final int MAX_BRAND_LENGTH = 80;
    static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final BigDecimal MAX_PESOS = new BigDecimal("9999999999");
    private static final BigDecimal MAX_CONTENT = new BigDecimal("99999");

    private final GtinValidator gtin = new GtinValidator();

    /** Producto nuevo: código, datos y stock inicial. Clave = nombre del campo del formulario. */
    public Map<String, String> validate(NewProduct product, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        gtin.problemWith(product.barcode()).ifPresent(problem -> errors.put("barcode", problem));
        errors.putAll(validate(product.data()));
        validateInitialStock(product, today, errors);
        return errors;
    }

    /** Solo los datos editables; es lo que se revisa al editar un producto existente. */
    public Map<String, String> validate(ProductData data) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateName(data.name(), errors);
        if (data.category() == null) {
            errors.put("category", "Elige el tipo de producto.");
        }
        validateOptionalText("brand", "La marca", data.brand(), MAX_BRAND_LENGTH, errors);
        validateNetContent(data.netContentAmount(), data.contentUnit(), errors);
        validateOptionalText("description", "La descripción", data.description(), MAX_DESCRIPTION_LENGTH, errors);
        validatePesos("salePrice", "el precio de venta", data.salePrice(), errors);
        validatePesos("cost", "el costo", data.cost(), errors);
        if (!errors.containsKey("salePrice") && !errors.containsKey("cost")
                && data.salePrice().compareTo(data.cost()) < 0) {
            errors.put("salePrice", "El precio de venta (" + data.salePrice().toPlainString()
                    + ") es menor que el costo (" + data.cost().toPlainString()
                    + "). Revisa si quedaron invertidos.");
        }
        if (data.safetyStock() != null && data.safetyStock() < 0) {
            errors.put("safetyStock", "El stock de seguridad no puede ser negativo.");
        }
        return errors;
    }

    private static void validateName(String name, Map<String, String> errors) {
        if (name == null || name.isBlank()) {
            errors.put("name", "Escribe el nombre del producto.");
        } else if (name.strip().length() > MAX_NAME_LENGTH) {
            errors.put("name", "El nombre admite máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
    }

    private static void validateOptionalText(String field, String label, String text, int maxLength,
                                             Map<String, String> errors) {
        if (text != null && text.strip().length() > maxLength) {
            errors.put(field, label + " admite máximo " + maxLength + " caracteres.");
        }
    }

    /** El contenido es opcional, pero la cantidad y la unidad van juntas. */
    private static void validateNetContent(BigDecimal amount, ContentUnit unit, Map<String, String> errors) {
        if (amount == null && unit == null) {
            return;
        }
        if (amount == null) {
            errors.put("netContentAmount", "Escribe cuánto contiene (por ejemplo 500) o quita la unidad.");
        } else if (unit == null) {
            errors.put("contentUnit", "Elige la unidad del contenido (g, kg, ml, L o und).");
        } else if (amount.signum() <= 0) {
            errors.put("netContentAmount", "El contenido debe ser mayor que cero.");
        } else if (amount.stripTrailingZeros().scale() > 2) {
            errors.put("netContentAmount", "Usa máximo dos decimales en el contenido.");
        } else if (unit.wholeUnitsOnly() && amount.stripTrailingZeros().scale() > 0) {
            errors.put("netContentAmount", "Las unidades por empaque se cuentan enteras (por ejemplo 6 und).");
        } else if (amount.compareTo(MAX_CONTENT) > 0) {
            errors.put("netContentAmount", "El contenido es demasiado alto. Revisa la unidad (¿kg en vez de g?).");
        }
    }

    /**
     * Los pesos colombianos no usan centavos. Exigir pesos enteros además atrapa el error
     * más común al escribir precios: "4.500" (con punto de miles) llega como 4,5.
     */
    private static void validatePesos(String field, String label, BigDecimal amount, Map<String, String> errors) {
        if (amount == null) {
            errors.put(field, "Escribe " + label + ".");
        } else if (amount.signum() <= 0) {
            errors.put(field, capitalize(label) + " debe ser mayor que cero.");
        } else if (amount.stripTrailingZeros().scale() > 0) {
            errors.put(field, "Escribe " + label + " en pesos enteros, sin decimales ni puntos de miles"
                    + " (por ejemplo 4500).");
        } else if (amount.compareTo(MAX_PESOS) > 0) {
            errors.put(field, capitalize(label) + " es demasiado alto.");
        }
    }

    private static void validateInitialStock(NewProduct product, LocalDate today, Map<String, String> errors) {
        Integer quantity = product.initialQuantity();
        LocalDate expiration = product.initialExpiration();
        if (quantity != null && quantity < 0) {
            errors.put("initialQuantity", "La cantidad inicial no puede ser negativa.");
            return;
        }
        if (!product.hasInitialStock()) {
            if (expiration != null) {
                errors.put("initialExpiration", "Escribe la cantidad inicial o deja la fecha vacía.");
            }
            return;
        }
        if (product.data().perishable()) {
            if (expiration == null) {
                errors.put("initialExpiration",
                        "Un producto perecedero necesita la fecha de vencimiento de su stock inicial.");
            } else if (expiration.isBefore(today)) {
                errors.put("initialExpiration",
                        "Esa fecha ya pasó: el stock nacería vencido y no se podría vender.");
            }
        } else if (expiration != null) {
            errors.put("initialExpiration", "Un producto no perecedero no lleva fecha de vencimiento."
                    + " Deja la fecha vacía o marca el producto como perecedero.");
        }
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
