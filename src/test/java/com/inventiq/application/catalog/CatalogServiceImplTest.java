package com.inventiq.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Supplier;
import com.inventiq.fakes.InMemoryProductRepository;
import com.inventiq.fakes.InMemorySupplierRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas del catálogo con repositorios falsos: sin Spring ni base de datos. */
class CatalogServiceImplTest {

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    private final CatalogServiceImpl service = new CatalogServiceImpl(products, suppliers);

    private final Supplier alpina = suppliers.add(
            new Supplier(1L, "Alpina", "+573005550001", 3, null, null, true));
    private final Supplier retired = suppliers.add(
            new Supplier(2L, "Retirado", "+573005550002", 3, null, null, false));

    {
        products.save(new Product(10L, "1001", "Leche", new BigDecimal("4000"),
                new BigDecimal("3000"), true, 1L, 2, 0, true, null));
    }

    private static ProductData data(String barcode, Long supplierId) {
        return new ProductData(barcode, "Yogur", new BigDecimal("3200"), new BigDecimal("2100"),
                true, supplierId, 5);
    }

    // ---------- Productos ----------

    @Test
    void aNewProductStartsActiveWithReorderPointZero() {
        // El fake de productos no asigna ids: se crea con el id que trae el formulario (null).
        Product created = service.createProduct(data("2002", 1L));

        assertEquals("2002", created.getBarcode());
        assertEquals(1L, created.getSupplierId());
        assertTrue(created.isActive());
        assertEquals(0, created.getReorderPoint());
    }

    @Test
    void aRepeatedBarcodeIsRejectedEvenFromAnotherProduct() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createProduct(data("1001", null)));
        assertTrue(e.getMessage().contains("Leche"));
    }

    @Test
    void aProductCannotBeAssignedToAMissingOrInactiveSupplier() {
        assertThrows(IllegalArgumentException.class, () -> service.createProduct(data("2002", 99L)));
        assertThrows(IllegalArgumentException.class, () -> service.createProduct(data("2002", 2L)));
    }

    @Test
    void missingPricesAreRejectedWithAClearMessage() {
        ProductData noPrice = new ProductData("2002", "Yogur", null, new BigDecimal("1"), false, null, 0);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createProduct(noPrice));
        assertEquals("El precio de venta y el costo son obligatorios", e.getMessage());
    }

    @Test
    void updatingAProductChangesEveryEditableFieldIncludingBarcodeAndPerishable() {
        ProductData edit = new ProductData("1009", "Leche entera", new BigDecimal("4200"),
                new BigDecimal("3100"), false, null, 4);

        Product updated = service.updateProduct(10L, edit);

        assertEquals("1009", updated.getBarcode());
        assertEquals("Leche entera", updated.getName());
        assertEquals(new BigDecimal("4200"), updated.getSalePrice());
        assertFalse(updated.isPerishable());
        assertNull(updated.getSupplierId());
        assertEquals(4, updated.getSafetyStock());
    }

    @Test
    void aProductKeepsItsOwnBarcodeWhenEdited() {
        Product updated = service.updateProduct(10L, data("1001", 1L));

        assertEquals("1001", updated.getBarcode());
    }

    @Test
    void aProductIsDeactivatedAndReactivatedInsteadOfDeleted() {
        service.setProductActive(10L, false);
        assertFalse(products.findById(10L).get().isActive());

        service.setProductActive(10L, true);
        assertTrue(products.findById(10L).get().isActive());
    }

    @Test
    void anUnknownProductFails() {
        assertThrows(IllegalArgumentException.class, () -> service.getProduct(99L));
    }

    // ---------- Proveedores ----------

    @Test
    void aNewSupplierGetsAnIdAndStartsActive() {
        Supplier created = service.createSupplier(new SupplierData("Postobón", "+573005550003", 2,
                null, 24));

        assertNotNull(created.getId());
        assertTrue(created.isActive());
        assertEquals(24, created.getMinOrderUnits());
    }

    @Test
    void updatingASupplierChangesItsData() {
        Supplier updated = service.updateSupplier(1L, new SupplierData("Alpina S.A.", "+573009999999",
                4, new BigDecimal("150000"), null));

        assertEquals("Alpina S.A.", updated.getName());
        assertEquals("+573009999999", updated.getWhatsappNumber());
        assertEquals(4, updated.getLeadTimeDays());
        assertEquals(new BigDecimal("150000"), updated.getMinOrderAmount());
    }

    @Test
    void onlyActiveSuppliersCanBeOfferedToProducts() {
        assertEquals(2, service.allSuppliers().size());
        assertEquals(1, service.activeSuppliers().size());

        service.setSupplierActive(2L, true);
        assertEquals(2, service.activeSuppliers().size());
        service.setSupplierActive(1L, false);
        assertFalse(alpina.isActive());
    }

    @Test
    void anUnknownSupplierFails() {
        assertThrows(IllegalArgumentException.class, () -> service.getSupplier(99L));
        assertNotNull(retired);
    }
}