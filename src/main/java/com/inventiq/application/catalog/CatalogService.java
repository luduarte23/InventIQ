package com.inventiq.application.catalog;

import java.util.List;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Supplier;

/**
 * Catálogo de productos y proveedores (D4). Nada se borra: ventas, lotes y pedidos apuntan a
 * estos registros, así que se inactivan. Los errores de datos se lanzan como IllegalArgumentException.
 */
public interface CatalogService {

    /** Activos e inactivos, por nombre. */
    List<Product> allProducts();
    Product getProduct(Long id);
    Product createProduct(ProductData data);
    Product updateProduct(Long id, ProductData data);
    void setProductActive(Long id, boolean active);

    /** Activos e inactivos, por nombre. */
    List<Supplier> allSuppliers();
    /** Los que se pueden asignar a un producto. */
    List<Supplier> activeSuppliers();
    Supplier getSupplier(Long id);
    Supplier createSupplier(SupplierData data);
    Supplier updateSupplier(Long id, SupplierData data);
    void setSupplierActive(Long id, boolean active);
}