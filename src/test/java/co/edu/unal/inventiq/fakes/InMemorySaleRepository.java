package co.edu.unal.inventiq.fakes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SaleItem;
import co.edu.unal.inventiq.domain.model.SoldItem;
import co.edu.unal.inventiq.domain.port.SaleRepository;

public class InMemorySaleRepository implements SaleRepository {

    private final List<Sale> sales = new ArrayList<>();

    @Override
    public Sale save(Sale sale) {
        Sale saved = new Sale((long) sales.size() + 1, sale.cashierUsername(), sale.createdAt(), sale.items());
        sales.add(saved);
        return saved;
    }

    @Override
    public Optional<Sale> findById(long id) {
        return sales.stream().filter(s -> s.id() == id).findFirst();
    }

    @Override
    public List<SoldItem> findItemsSoldBetween(LocalDateTime from, LocalDateTime to) {
        List<SoldItem> items = new ArrayList<>();
        for (Sale sale : sales) {
            if (!sale.createdAt().isBefore(from) && sale.createdAt().isBefore(to)) {
                for (SaleItem item : sale.items()) {
                    items.add(new SoldItem(sale.id(), sale.createdAt(), item.productId(), item.quantity(),
                            item.unitPrice()));
                }
            }
        }
        items.sort(Comparator.comparing(SoldItem::soldAt));
        return items;
    }

    public List<Sale> all() {
        return List.copyOf(sales);
    }
}
