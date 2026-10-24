package co.edu.unal.inventiq.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Sale;
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

    public List<Sale> all() {
        return List.copyOf(sales);
    }
}
