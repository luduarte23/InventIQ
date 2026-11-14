package com.inventiq.fakes;

import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.SaleItem;
import com.inventiq.domain.port.SaleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso de ventas: guarda todo en una lista. Solo para pruebas. */
public class InMemorySaleRepository implements SaleRepository {

    private final List<Sale> sales = new ArrayList<>();
    private long nextId = 1;

    /** Si la venta es nueva le pone un id, como haría la base de datos. */
    @Override
    public Sale save(Sale sale) {
        if (sale.getId() == null) {
            sale = new Sale(nextId++, sale.getCashierId(), sale.getCreatedAt(), sale.getClosedAt(), sale.getItems());
        }
        if (!sales.contains(sale)) {
            sales.add(sale);
        }
        return sale;
    }

    @Override
    public Optional<Sale> findById(Long id) {
        for (Sale sale : sales) {
            if (sale.getId().equals(id)) {
                return Optional.of(sale);
            }
        }
        return Optional.empty();
    }

    @Override
    public void delete(Long id) {
        sales.removeIf(sale -> sale.getId().equals(id));
    }

    /** Unidades vendidas del producto desde 'since', contando solo ventas cerradas. */
    @Override
    public int unitsSoldSince(Long productId, Instant since) {
        int total = 0;
        for (Sale sale : sales) {
            if (!sale.isClosed() || sale.getCreatedAt().isBefore(since)) {
                continue;
            }
            for (SaleItem item : sale.getItems()) {
                if (item.getProductId().equals(productId)) {
                    total += item.getQuantity();
                }
            }
        }
        return total;
    }
}
