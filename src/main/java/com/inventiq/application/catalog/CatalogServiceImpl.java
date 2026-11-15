package com.inventiq.application.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.SupplierRepository;

/**
 * Crea, edita e inactiva productos y proveedores. Revisa lo que depende de otros registros
 * (código de barras único, proveedor existente y activo); el resto lo validan Product y Supplier.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

    private final ProductRepository products;
    private final SupplierRepository suppliers;

    public CatalogServiceImpl(ProductRepository products, SupplierRepository suppliers) {
        this.products = products;
        this.suppliers = suppliers;
    }

    // ---------- Productos ----------

    @Override
    @Transactional(readOnly = true)
    public List<Product> allProducts() {
        return products.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El producto " + id + " no existe"));
    }

    @Override
    @Transactional
    public Product createProduct(ProductData data) {
        requirePrices(data);
        requireUniqueBarcode(data.barcode(), null);
        requireActiveSupplier(data.supplierId());
        Product product = new Product(null, data.barcode(), data.name(), data.salePrice(),
                data.unitCost(), data.perishable(), data.supplierId(), data.safetyStock(),
                0, true, null);
        return products.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductData data) {
        Product product = getProduct(id);
        requirePrices(data);
        requireUniqueBarcode(data.barcode(), id);
        if (data.supplierId() != null && !data.supplierId().equals(product.getSupplierId())) {
            requireActiveSupplier(data.supplierId());
        }
        product.changeBarcode(data.barcode());
        product.rename(data.name());
        product.changePrices(data.salePrice(), data.unitCost());
        product.changePerishable(data.perishable());
        product.assignSupplier(data.supplierId());
        product.changeSafetyStock(data.safetyStock());
        return products.save(product);
    }

    @Override
    @Transactional
    public void setProductActive(Long id, boolean active) {
        Product product = getProduct(id);
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        products.save(product);
    }

    // ---------- Proveedores ----------

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> allSuppliers() {
        return suppliers.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> activeSuppliers() {
        return suppliers.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier getSupplier(Long id) {
        return suppliers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El proveedor " + id + " no existe"));
    }

    @Override
    @Transactional
    public Supplier createSupplier(SupplierData data) {
        Supplier supplier = new Supplier(null, data.name(), data.whatsappNumber(),
                data.leadTimeDays(), data.minOrderAmount(), data.minOrderUnits(), true);
        return suppliers.save(supplier);
    }

    @Override
    @Transactional
    public Supplier updateSupplier(Long id, SupplierData data) {
        Supplier supplier = getSupplier(id);
        supplier.rename(data.name());
        supplier.changeWhatsappNumber(data.whatsappNumber());
        supplier.changeLeadTime(data.leadTimeDays());
        supplier.changeMinimumOrder(data.minOrderAmount(), data.minOrderUnits());
        return suppliers.save(supplier);
    }

    @Override
    @Transactional
    public void setSupplierActive(Long id, boolean active) {
        Supplier supplier = getSupplier(id);
        if (active) {
            supplier.activate();
        } else {
            supplier.deactivate();
        }
        suppliers.save(supplier);
    }

    // ---------- Reglas que dependen de otros registros ----------

    /** El código de barras es único en la tabla, incluidos los inactivos. */
    private void requireUniqueBarcode(String barcode, Long ownId) {
        products.findByBarcode(barcode)
                .filter(other -> !other.getId().equals(ownId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Ya existe un producto con el código "
                            + barcode + ": " + other.getName());
                });
    }

    /** Un producto solo se asigna a un proveedor que exista y esté activo (o a ninguno). */
    private void requireActiveSupplier(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        Supplier supplier = getSupplier(supplierId);
        if (!supplier.isActive()) {
            throw new IllegalArgumentException("El proveedor " + supplier.getName() + " está inactivo");
        }
    }

    private static void requirePrices(ProductData data) {
        if (data.salePrice() == null || data.unitCost() == null) {
            throw new IllegalArgumentException("El precio de venta y el costo son obligatorios");
        }
        if (data.salePrice().compareTo(BigDecimal.ZERO) < 0 || data.unitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los precios no pueden ser negativos");
        }
    }
}