package co.edu.unal.inventiq.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SaleTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 30);

    @Test
    void totalAndUnitsAddUpAllItems() {
        Sale sale = new Sale(null, "cajero", NOW, List.of(
                new SaleItem(1, 10, 3, new BigDecimal("3200")),
                new SaleItem(1, 11, 2, new BigDecimal("3200")),
                new SaleItem(2, 20, 1, new BigDecimal("4500"))));

        assertThat(sale.total()).isEqualByComparingTo("20500");
        assertThat(sale.unitCount()).isEqualTo(6);
    }

    @Test
    void aSaleNeedsAtLeastOneItem() {
        assertThatThrownBy(() -> new Sale(null, "cajero", NOW, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemsAreCopiedSoTheSaleCannotChangeAfterwards() {
        List<SaleItem> items = new ArrayList<>(List.of(new SaleItem(1, 10, 1, BigDecimal.TEN)));
        Sale sale = new Sale(null, "cajero", NOW, items);

        items.add(new SaleItem(2, 20, 1, BigDecimal.TEN));

        assertThat(sale.items()).hasSize(1);
    }
}
