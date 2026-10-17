package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;

import co.edu.unal.inventiq.domain.exception.InvalidSettingsException;

/**
 * Parámetros operativos de la tienda (FR-08). Hay uno solo por tienda.
 *
 * @param weeklyBudget    presupuesto semanal de compras; null si no hay límite (lo usa FR-06)
 * @param salesWindowDays días de historia con los que se calcula la velocidad de ventas (ROP)
 * @param adminWhatsapp   WhatsApp del administrador; null si no se configuró
 */
public record Settings(BigDecimal weeklyBudget, int salesWindowDays, String adminWhatsapp) {

    public static final int DEFAULT_SALES_WINDOW_DAYS = 30;

    public Settings {
        if (weeklyBudget != null && weeklyBudget.signum() < 0) {
            throw new InvalidSettingsException("El presupuesto semanal no puede ser negativo.");
        }
        if (salesWindowDays <= 0) {
            throw new InvalidSettingsException("La ventana de ventas debe ser de al menos un día.");
        }
    }

    /** Los parámetros con los que arranca una tienda nueva. */
    public static Settings defaults() {
        return new Settings(null, DEFAULT_SALES_WINDOW_DAYS, null);
    }

    public boolean hasBudget() {
        return weeklyBudget != null;
    }
}
