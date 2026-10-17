package co.edu.unal.inventiq.domain.model;

import static co.edu.unal.inventiq.fixtures.NewProductBuilder.aValidProduct;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NewProductTest {

    @Test
    void becomesAnActiveProductWithoutIdOrReorderPoint() {
        Product product = aValidProduct().barcode(" 7702001000029 ").name("  Leche deslactosada 1 L ")
                .supplierId(4L).build().toProduct();

        assertThat(product.id()).isNull();
        assertThat(product.barcode()).isEqualTo("7702001000029");
        assertThat(product.name()).isEqualTo("Leche deslactosada 1 L");
        assertThat(product.supplierId()).isEqualTo(4L);
        assertThat(product.reorderPoint()).isNull();
        assertThat(product.active()).isTrue();
    }

    @Test
    void missingSafetyStockMeansZero() {
        assertThat(aValidProduct().safetyStock(null).build().toProduct().safetyStock()).isZero();
    }

    @Test
    void onlyAPositiveQuantityCountsAsInitialStock() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        assertThat(aValidProduct().initialStock(null, null).build().hasInitialStock()).isFalse();
        assertThat(aValidProduct().initialStock(0, date).build().hasInitialStock()).isFalse();
        assertThat(aValidProduct().initialStock(1, date).build().hasInitialStock()).isTrue();
    }
}
