package co.edu.unal.inventiq.domain.model;

import static co.edu.unal.inventiq.fixtures.NewProductBuilder.aValidProduct;
import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void onlyAnActiveProductWithItsSaleEnabledCanBeSold() {
        assertThat(aProduct().build().isSellable()).isTrue();
        assertThat(aProduct().saleEnabled(false).build().isSellable()).isFalse();
        assertThat(aProduct().active(false).build().isSellable()).isFalse();
    }

    @Test
    void pausingTheSaleChangesNothingElse() {
        Product product = aProduct().reorderPoint(12).supplierId(3L).build();

        Product paused = product.withSaleEnabled(false);

        assertThat(paused.saleEnabled()).isFalse();
        assertThat(paused.withSaleEnabled(true)).isEqualTo(product);
    }

    @Test
    void editingKeepsTheIdentityTheReorderPointAndTheStatus() {
        Product product = aProduct().id(7L).barcode("7702001000029").reorderPoint(12).saleEnabled(false).build();

        Product edited = product.withData(aValidProduct().name("  Leche sin lactosa ").brand("  ")
                .description(" Nueva receta. ").category(ProductCategory.LACTEOS).buildData());

        assertThat(edited.id()).isEqualTo(7L);
        assertThat(edited.barcode()).isEqualTo("7702001000029");
        assertThat(edited.reorderPoint()).isEqualTo(12);
        assertThat(edited.saleEnabled()).isFalse();
        assertThat(edited.active()).isTrue();
        assertThat(edited.name()).isEqualTo("Leche sin lactosa");
        assertThat(edited.brand()).isNull();
        assertThat(edited.description()).isEqualTo("Nueva receta.");
    }

    @Test
    void aRegisteredProductStartsActiveWithoutReorderPoint() {
        Product product = Product.register(" 7702001000029 ", aValidProduct().safetyStock(null).buildData(), false);

        assertThat(product.id()).isNull();
        assertThat(product.barcode()).isEqualTo("7702001000029");
        assertThat(product.active()).isTrue();
        assertThat(product.saleEnabled()).isFalse();
        assertThat(product.reorderPoint()).isNull();
        assertThat(product.safetyStock()).isZero();
        assertThat(product.netContent().display()).isEqualTo("1 L");
    }

    @Test
    void detectsWhenThePriceOrTheCostChanged() {
        Product product = aProduct().salePrice(4500).cost(3300).build();

        assertThat(product.pricesDifferFrom(aProduct().salePrice(4500).cost(3300).name("Otro").build())).isFalse();
        assertThat(product.pricesDifferFrom(aProduct().salePrice(4600).cost(3300).build())).isTrue();
        assertThat(product.pricesDifferFrom(aProduct().salePrice(4500).cost(3400).build())).isTrue();
    }

    @Test
    void theCategoryAndPricesAreRequired() {
        assertThatThrownBy(() -> aProduct().category(null).build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void contentIsShownWithoutExtraZerosAndWithADecimalComma() {
        assertThat(new NetContent(new java.math.BigDecimal("500.00"), ContentUnit.G).display()).isEqualTo("500 g");
        assertThat(new NetContent(new java.math.BigDecimal("1.50"), ContentUnit.L).display()).isEqualTo("1,5 L");
        assertThat(new NetContent(new java.math.BigDecimal("1000"), ContentUnit.ML).display()).isEqualTo("1000 ml");
        assertThat(new NetContent(new java.math.BigDecimal("6"), ContentUnit.UND).display()).isEqualTo("6 und");
        assertThat(ContentUnit.UND.wholeUnitsOnly()).isTrue();
        assertThat(ContentUnit.KG.label()).isEqualTo("kilogramos");
        assertThat(ProductCategory.GRANOS.label()).isEqualTo("Granos y cereales");
    }
}
