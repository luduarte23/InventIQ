package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas de Settings: valores por defecto de ACUERDOS §3 y validaciones. */
class SettingsTest {

    private static final BigDecimal BUDGET = new BigDecimal("500000");
    private static final String WHATSAPP = "+573001234567";

    @Test
    void withDefaultsUsesTheValuesOfSection3() {
        Settings settings = Settings.withDefaults(BUDGET, WHATSAPP);

        assertEquals(BUDGET, settings.getWeeklyBudget());
        assertEquals(30, settings.getSalesWindowDays());
        assertEquals(7, settings.getReviewPeriodDays());
        assertEquals(30, settings.getExpiryReviewHorizonDays());
        assertEquals(WHATSAPP, settings.getAdminWhatsappNumber());
        assertEquals(1L, Settings.SINGLETON_ID);
    }

    @Test
    void budgetIsRequiredAndCannotBeNegative() {
        assertThrows(NullPointerException.class, () -> Settings.withDefaults(null, WHATSAPP));
        assertThrows(IllegalArgumentException.class,
                () -> Settings.withDefaults(new BigDecimal("-1"), WHATSAPP));
    }

    @Test
    void daysMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class, () -> new Settings(BUDGET, 0, 7, 30, WHATSAPP));
        assertThrows(IllegalArgumentException.class, () -> new Settings(BUDGET, 30, 0, 30, WHATSAPP));
        assertThrows(IllegalArgumentException.class, () -> new Settings(BUDGET, 30, 7, 0, WHATSAPP));
    }

    @Test
    void adminWhatsappCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Settings.withDefaults(BUDGET, " "));
    }

    @Test
    void everyValueCanBeChanged() {
        Settings settings = Settings.withDefaults(BUDGET, WHATSAPP);

        settings.changeWeeklyBudget(new BigDecimal("800000"));
        settings.changeSalesWindowDays(60);
        settings.changeReviewPeriodDays(14);
        settings.changeExpiryReviewHorizonDays(15);
        settings.changeAdminWhatsappNumber("+573109876543");

        assertEquals(new BigDecimal("800000"), settings.getWeeklyBudget());
        assertEquals(60, settings.getSalesWindowDays());
        assertEquals(14, settings.getReviewPeriodDays());
        assertEquals(15, settings.getExpiryReviewHorizonDays());
        assertEquals("+573109876543", settings.getAdminWhatsappNumber());
    }

    @Test
    void invalidChangesAreRejected() {
        Settings settings = Settings.withDefaults(BUDGET, WHATSAPP);

        assertThrows(IllegalArgumentException.class,
                () -> settings.changeWeeklyBudget(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> settings.changeSalesWindowDays(0));
        assertThrows(IllegalArgumentException.class, () -> settings.changeReviewPeriodDays(-1));
        assertThrows(IllegalArgumentException.class,
                () -> settings.changeExpiryReviewHorizonDays(0));
        assertThrows(IllegalArgumentException.class, () -> settings.changeAdminWhatsappNumber(null));
    }
}