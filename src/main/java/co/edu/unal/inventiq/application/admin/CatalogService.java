package co.edu.unal.inventiq.application.admin;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import co.edu.unal.inventiq.application.TransactionRunner;
import co.edu.unal.inventiq.domain.exception.InvalidProductException;
import co.edu.unal.inventiq.domain.exception.ProductNotFoundException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.NewProduct;
import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductData;
import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SupplierRepository;
import co.edu.unal.inventiq.domain.rules.GtinValidator;
import co.edu.unal.inventiq.domain.rules.NewProductValidator;

/**
 * Catálogo de productos (D4): registrar por escáner o a mano, editar, y pausar o habilitar la
 * venta. Cada cambio de precio o costo queda en el historial de precios.
 */
public class CatalogService {

    private final ProductRepository products;
    private final BatchRepository batches;
    private final SupplierRepository suppliers;
    private final PriceHistoryRepository priceHistory;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final GtinValidator gtin = new GtinValidator();
    private final NewProductValidator validator = new NewProductValidator();

    public CatalogService(ProductRepository products, BatchRepository batches, SupplierRepository suppliers,
                          PriceHistoryRepository priceHistory, TransactionRunner transactions, Clock clock) {
        this.products = products;
        this.batches = batches;
        this.suppliers = suppliers;
        this.priceHistory = priceHistory;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * Primero busca el código tal cual (así se encuentran también productos antiguos con
     * códigos no estándar) y solo si no existe valida que sea un GTIN.
     */
    public BarcodeLookup lookup(String barcode) {
        String code = barcode == null ? "" : barcode.strip();
        Optional<Product> existing = products.findByBarcode(code);
        if (existing.isPresent()) {
            return new BarcodeLookup.AlreadyRegistered(entryFor(existing.get(), supplierNames()));
        }
        return gtin.problemWith(code)
                .<BarcodeLookup>map(reason -> new BarcodeLookup.Invalid(code, reason))
                .orElseGet(() -> new BarcodeLookup.Available(code));
    }

    /** Todos los errores del producto nuevo, sin guardar nada. Vacío si se puede registrar. */
    public Map<String, String> validate(NewProduct request) {
        Map<String, String> errors = new LinkedHashMap<>(validator.validate(request, LocalDate.now(clock)));
        if (!errors.containsKey("barcode")) {
            products.findByBarcode(request.barcode().strip()).ifPresent(existing -> errors.put("barcode",
                    "Ya existe un producto con el código " + existing.barcode() + ": " + existing.name() + "."));
        }
        checkSupplier(request.data(), errors);
        return errors;
    }

    /** Todos los errores de una edición, sin guardar nada. */
    public Map<String, String> validateChanges(ProductData data) {
        Map<String, String> errors = new LinkedHashMap<>(validator.validate(data));
        checkSupplier(data, errors);
        return errors;
    }

    /**
     * Registra el producto y, si trae stock inicial, su primer lote, en una sola transacción.
     * Su precio inicial abre el historial de precios.
     *
     * @throws InvalidProductException con todos los errores por campo si algo no cumple las reglas
     */
    public Product register(NewProduct request) {
        Map<String, String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new InvalidProductException(errors);
        }
        return transactions.inTransaction(() -> {
            LocalDateTime now = LocalDateTime.now(clock);
            Product saved = products.create(request.toProduct());
            priceHistory.record(PriceChange.of(saved, now));
            if (request.hasInitialStock()) {
                int quantity = request.initialQuantity();
                batches.create(new Batch(null, saved.id(), quantity, quantity, request.initialExpiration(), now, 0));
            }
            return saved;
        });
    }

    /** @throws ProductNotFoundException si no existe */
    public Product get(long productId) {
        return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }

    /**
     * Guarda la edición. Si cambió el precio de venta o el costo, lo registra en el historial.
     *
     * @throws InvalidProductException  con los errores por campo
     * @throws ProductNotFoundException si el producto no existe
     */
    public Product update(long productId, ProductData data) {
        Map<String, String> errors = validateChanges(data);
        if (!errors.isEmpty()) {
            throw new InvalidProductException(errors);
        }
        return transactions.inTransaction(() -> {
            Product current = get(productId);
            Product saved = products.update(current.withData(data));
            if (saved.pricesDifferFrom(current)) {
                priceHistory.record(PriceChange.of(saved, LocalDateTime.now(clock)));
            }
            return saved;
        });
    }

    /**
     * Pausa o habilita la venta. Con la venta pausada el producto sigue en el inventario con su
     * stock, pero el POS no lo vende (p. ej. stock comprado antes de un alza de precio).
     */
    public Product setSaleEnabled(long productId, boolean enabled) {
        return transactions.inTransaction(() -> {
            Product current = get(productId);
            if (current.saleEnabled() == enabled) {
                return current;
            }
            return products.update(current.withSaleEnabled(enabled));
        });
    }

    /** Proveedores que se pueden asignar a un producto, por nombre. */
    public List<Supplier> activeSuppliers() {
        return suppliers.findAll().stream().filter(Supplier::active).toList();
    }

    /** Productos activos, por nombre, con el nombre de su proveedor. */
    public List<CatalogEntry> catalog() {
        Map<Long, String> names = supplierNames();
        return products.findAllActive().stream().map(product -> entryFor(product, names)).toList();
    }

    /** El catálogo con sus indicadores. */
    public CatalogOverview overview() {
        return CatalogOverview.of(catalog());
    }

    private void checkSupplier(ProductData data, Map<String, String> errors) {
        if (data.supplierId() != null
                && suppliers.findById(data.supplierId()).filter(Supplier::active).isEmpty()) {
            errors.put("supplierId", "El proveedor elegido no existe o está inactivo.");
        }
    }

    private Map<Long, String> supplierNames() {
        return suppliers.findAll().stream().collect(Collectors.toMap(Supplier::id, Supplier::name));
    }

    private static CatalogEntry entryFor(Product product, Map<Long, String> supplierNames) {
        return new CatalogEntry(product, product.supplierId() == null ? null : supplierNames.get(product.supplierId()));
    }
}
