package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas de Product: validaciones, cambios y punto de reorden. */
class ProductTest {

    private static final BigDecimal PRICE = new BigDecimal("3200");
    private static final BigDecimal COST = new BigDecimal("2100");

    // --- Constructor y validaciones ---

    @Test
    void aValidProductKeepsItsData() {
        Product product = new Product(1L, "7702001000012", "Yogur de fresa", PRICE, COST,
                true, 9L, 4, 12, true, 3L);

        assertEquals(1L, product.getId());
        assertEquals("7702001000012", product.getBarcode());
        assertEquals("Yogur de fresa", product.getName());
        assertEquals(PRICE, product.getSalePrice());
        assertEquals(COST, product.getUnitCost());
        assertTrue(product.isPerishable());
        assertEquals(9L, product.getSupplierId());
        assertEquals(4, product.getSafetyStock());
        assertEquals(12, product.getReorderPoint());
        assertTrue(product.isActive());
        assertEquals(3L, product.getVersion());
    }

    @Test
    void barcodeAndNameCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, null, "Yogur",
                PRICE, COST, true, null, 0, 0, true, null));
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "  ", "Yogur",
                PRICE, COST, true, null, 0, 0, true, null));
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "770", "",
                PRICE, COST, true, null, 0, 0, true, null));
    }

    @Test
    void pricesAreRequiredAndCannotBeNegative() {
        assertThrows(NullPointerException.class, () -> new Product(null, "770", "Yogur",
                null, COST, true, null, 0, 0, true, null));
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "770", "Yogur",
                PRICE, new BigDecimal("-1"), true, null, 0, 0, true, null));
    }

    @Test
    void safetyStockAndReorderPointCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "770", "Yogur",
                PRICE, COST, true, null, -1, 0, true, null));
        assertThrows(IllegalArgumentException.class, () -> new Product(null, "770", "Yogur",
                PRICE, COST, true, null, 0, -1, true, null));
    }

    // --- Cambios ---

    @Test
    void pricesAndNameCanBeChanged() {
        Product product = product();

        product.changePrices(new BigDecimal("3500"), new BigDecimal("2300"));
        product.rename("Yogur de mora");

        assertEquals(new BigDecimal("3500"), product.getSalePrice());
        assertEquals(new BigDecimal("2300"), product.getUnitCost());
        assertEquals("Yogur de mora", product.getName());
    }

    @Test
    void invalidChangesAreRejected() {
        Product product = product();

        assertThrows(IllegalArgumentException.class,
                () -> product.changePrices(new BigDecimal("-1"), COST));
        assertThrows(IllegalArgumentException.class, () -> product.rename(" "));
        assertThrows(IllegalArgumentException.class, () -> product.changeSafetyStock(-1));
        assertThrows(IllegalArgumentException.class, () -> product.updateReorderPoint(-1));
    }

    @Test
    void aSupplierCanBeAssignedAndRemoved() {
        Product product = product();
        assertFalse(product.hasSupplier());

        product.assignSupplier(9L);
        assertTrue(product.hasSupplier());
        assertEquals(9L, product.getSupplierId());

        product.assignSupplier(null);
        assertFalse(product.hasSupplier());
        assertNull(product.getSupplierId());
    }

    @Test
    void safetyStockCanBeChanged() {
        Product product = product();

        product.changeSafetyStock(6);

        assertEquals(6, product.getSafetyStock());
    }

    @Test
    void aProductCanBeDeactivatedAndActivated() {
        Product product = product();

        product.deactivate();
        assertFalse(product.isActive());

        product.activate();
        assertTrue(product.isActive());
    }

    // --- Punto de reorden (§5.4: se pide cuando availableStock <= ROP) ---

    @Test
    void itNeedsReorderWhenStockIsAtOrBelowTheReorderPoint() {
        Product product = product();
        product.updateReorderPoint(10);

        assertEquals(10, product.getReorderPoint());
        assertTrue(product.needsReorder(9));
        assertTrue(product.needsReorder(10));
        assertFalse(product.needsReorder(11));
    }

    // --- Igualdad por id ---

    @Test
    void productsWithTheSameIdAreEqual() {
        Product a = new Product(1L, "770", "Yogur", PRICE, COST, true, null, 0, 0, true, 0L);
        Product b = new Product(1L, "771", "Leche", PRICE, COST, false, null, 0, 0, true, 0L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void productsWithoutIdAreOnlyEqualToThemselves() {
        Product a = product();
        Product b = product();

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    /** Producto nuevo (sin id ni proveedor), activo, con ROP 0. */
    private static Product product() {
        return new Product(null, "7702001000012", "Yogur de fresa", PRICE, COST,
                true, null, 4, 0, true, null);
    }
}
