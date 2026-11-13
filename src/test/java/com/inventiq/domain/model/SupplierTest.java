package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas de Supplier: validaciones, pedido mínimo (MOQ) y cambios. */
class SupplierTest {

    private static final String WHATSAPP = "+573001234567";

    @Test
    void aValidSupplierKeepsItsData() {
        Supplier supplier = new Supplier(9L, "Lácteos del Valle", WHATSAPP, 3,
                new BigDecimal("150000"), 24, true);

        assertEquals(9L, supplier.getId());
        assertEquals("Lácteos del Valle", supplier.getName());
        assertEquals(WHATSAPP, supplier.getWhatsappNumber());
        assertEquals(3, supplier.getLeadTimeDays());
        assertEquals(new BigDecimal("150000"), supplier.getMinOrderAmount());
        assertEquals(24, supplier.getMinOrderUnits());
        assertTrue(supplier.isActive());
    }

    @Test
    void nameAndWhatsappCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Supplier(null, null, WHATSAPP, 3, null, null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Supplier(null, "Lácteos", " ", 3, null, null, true));
    }

    @Test
    void negativeValuesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Supplier(null, "Lácteos", WHATSAPP, -1, null, null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Supplier(null, "Lácteos", WHATSAPP, 3, new BigDecimal("-1"), null, true));
        assertThrows(IllegalArgumentException.class,
                () -> new Supplier(null, "Lácteos", WHATSAPP, 3, null, -1, true));
    }

    @Test
    void aSupplierCanHaveAMinimumOrderByAmountByUnitsOrNone() {
        Supplier byAmount = new Supplier(null, "A", WHATSAPP, 3, new BigDecimal("150000"), null, true);
        Supplier byUnits = new Supplier(null, "B", WHATSAPP, 3, null, 24, true);
        Supplier none = new Supplier(null, "C", WHATSAPP, 3, null, null, true);

        assertTrue(byAmount.hasMinimumOrder());
        assertTrue(byUnits.hasMinimumOrder());
        assertFalse(none.hasMinimumOrder());
        assertNull(none.getMinOrderAmount());
        assertNull(none.getMinOrderUnits());
    }

    @Test
    void leadTimeAndWhatsappCanBeChanged() {
        Supplier supplier = supplier();

        supplier.changeLeadTime(5);
        supplier.changeWhatsappNumber("+573109876543");

        assertEquals(5, supplier.getLeadTimeDays());
        assertEquals("+573109876543", supplier.getWhatsappNumber());
    }

    @Test
    void invalidChangesAreRejected() {
        Supplier supplier = supplier();

        assertThrows(IllegalArgumentException.class, () -> supplier.changeLeadTime(-1));
        assertThrows(IllegalArgumentException.class, () -> supplier.changeWhatsappNumber(""));
    }

    @Test
    void aSupplierCanBeDeactivatedAndActivated() {
        Supplier supplier = supplier();

        supplier.deactivate();
        assertFalse(supplier.isActive());

        supplier.activate();
        assertTrue(supplier.isActive());
    }

    @Test
    void suppliersWithTheSameIdAreEqual() {
        Supplier a = new Supplier(9L, "A", WHATSAPP, 3, null, null, true);
        Supplier b = new Supplier(9L, "B", WHATSAPP, 7, null, null, false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void suppliersWithoutIdAreOnlyEqualToThemselves() {
        Supplier a = supplier();
        Supplier b = supplier();

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    private static Supplier supplier() {
        return new Supplier(null, "Lácteos del Valle", WHATSAPP, 3, null, null, true);
    }
}
