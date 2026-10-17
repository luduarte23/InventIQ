package co.edu.unal.inventiq.domain.rules;

import static co.edu.unal.inventiq.fixtures.NewProductBuilder.aValidProduct;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NewProduct;
import co.edu.unal.inventiq.domain.model.ProductData;

class NewProductValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

    private final NewProductValidator validator = new NewProductValidator();

    @Test
    void aValidProductWithoutInitialStockHasNoErrors() {
        assertThat(errorsOf(aValidProduct().build())).isEmpty();
    }

    @Test
    void aPerishableProductWithStockAndAFutureExpirationIsValid() {
        assertThat(errorsOf(aValidProduct().initialStock(12, TODAY.plusDays(20)).build())).isEmpty();
    }

    @Test
    void stockThatExpiresTodayIsStillValidBecauseItCanBeSoldToday() {
        assertThat(errorsOf(aValidProduct().initialStock(3, TODAY).build())).isEmpty();
    }

    @Test
    void aNonPerishableProductWithStockNeedsNoExpiration() {
        assertThat(errorsOf(aValidProduct().perishable(false).initialStock(30, null).build())).isEmpty();
    }

    @Test
    void barcodeMustBeAValidGtin() {
        Map<String, String> errors = errorsOf(aValidProduct().barcode("7702001000028").build());

        assertThat(errors).containsOnlyKeys("barcode");
        assertThat(errors.get("barcode")).contains("debería ser 9");
    }

    @Test
    void allMissingFieldsAreReportedAtOnce() {
        NewProduct empty = aValidProduct().barcode(null).name("  ").salePrice(null).cost(null).build();

        assertThat(errorsOf(empty)).containsOnlyKeys("barcode", "name", "salePrice", "cost")
                .containsEntry("name", "Escribe el nombre del producto.")
                .containsEntry("salePrice", "Escribe el precio de venta.")
                .containsEntry("cost", "Escribe el costo.");
    }

    @Test
    void nameHasAMaximumLength() {
        String longName = "x".repeat(NewProductValidator.MAX_NAME_LENGTH + 1);

        assertThat(errorsOf(aValidProduct().name(longName).build())).containsOnlyKeys("name");
        assertThat(errorsOf(aValidProduct().name("x".repeat(NewProductValidator.MAX_NAME_LENGTH)).build())).isEmpty();
    }

    @Test
    void pricesMustBePositive() {
        Map<String, String> errors = errorsOf(aValidProduct().salePrice("0").cost("-5").build());

        assertThat(errors.get("salePrice")).isEqualTo("El precio de venta debe ser mayor que cero.");
        assertThat(errors.get("cost")).isEqualTo("El costo debe ser mayor que cero.");
    }

    @Test
    void decimalPesosAreRejectedBecauseTheyUsuallyComeFromAThousandsSeparator() {
        // El usuario escribe "4.500" pensando en cuatro mil quinientos y llega 4.5.
        Map<String, String> errors = errorsOf(aValidProduct().salePrice("4.5").cost("3.3").build());

        assertThat(errors.get("salePrice")).contains("pesos enteros").contains("4500");
        assertThat(errors).containsKey("cost");
    }

    @Test
    void trailingZeroDecimalsAreWholePesos() {
        assertThat(errorsOf(aValidProduct().salePrice("5200.00").cost("3900.0").build())).isEmpty();
    }

    @Test
    void absurdlyHighPricesAreRejected() {
        assertThat(errorsOf(aValidProduct().salePrice("99999999999").build()).get("salePrice"))
                .contains("demasiado alto");
    }

    @Test
    void salePriceBelowCostIsCaughtByCrossChecking() {
        Map<String, String> errors = errorsOf(aValidProduct().salePrice("3900").cost("5200").build());

        assertThat(errors).containsOnlyKeys("salePrice");
        assertThat(errors.get("salePrice")).contains("menor que el costo").contains("invertidos");
    }

    @Test
    void sellingAtCostIsAllowed() {
        assertThat(errorsOf(aValidProduct().salePrice("3900").cost("3900").build())).isEmpty();
    }

    @Test
    void crossCheckIsSkippedWhenAPriceIsAlreadyWrong() {
        Map<String, String> errors = errorsOf(aValidProduct().salePrice(null).cost("5200").build());

        assertThat(errors).containsExactly(Map.entry("salePrice", "Escribe el precio de venta."));
    }

    @Test
    void safetyStockCannotBeNegative() {
        assertThat(errorsOf(aValidProduct().safetyStock(-1).build())).containsOnlyKeys("safetyStock");
        assertThat(errorsOf(aValidProduct().safetyStock(null).build())).isEmpty();
    }

    @Test
    void perishableStockNeedsAnExpirationDate() {
        assertThat(errorsOf(aValidProduct().initialStock(5, null).build()).get("initialExpiration"))
                .contains("necesita la fecha de vencimiento");
    }

    @Test
    void initialStockCannotBeBornExpired() {
        assertThat(errorsOf(aValidProduct().initialStock(5, TODAY.minusDays(1)).build()).get("initialExpiration"))
                .contains("ya pasó");
    }

    @Test
    void nonPerishableStockMustNotHaveAnExpirationDate() {
        assertThat(errorsOf(aValidProduct().perishable(false).initialStock(5, TODAY.plusDays(30)).build())
                .get("initialExpiration")).contains("no perecedero");
    }

    @Test
    void anExpirationDateWithoutQuantityIsAMistake() {
        assertThat(errorsOf(aValidProduct().initialStock(null, TODAY.plusDays(5)).build()))
                .containsOnlyKeys("initialExpiration");
        assertThat(errorsOf(aValidProduct().initialStock(0, TODAY.plusDays(5)).build()))
                .containsOnlyKeys("initialExpiration");
    }

    @Test
    void initialQuantityCannotBeNegative() {
        assertThat(errorsOf(aValidProduct().initialStock(-3, TODAY.plusDays(5)).build()))
                .containsOnlyKeys("initialQuantity");
    }

    @Test
    void theTypeIsRequired() {
        assertThat(errorsOf(aValidProduct().category(null).build()))
                .containsExactly(Map.entry("category", "Elige el tipo de producto."));
    }

    @Test
    void brandAndDescriptionAreOptionalButHaveAMaximumLength() {
        assertThat(errorsOf(aValidProduct().brand(null).description(null).build())).isEmpty();
        assertThat(errorsOf(aValidProduct().brand("x".repeat(NewProductValidator.MAX_BRAND_LENGTH + 1)).build()))
                .containsOnlyKeys("brand");
        assertThat(errorsOf(aValidProduct()
                .description("x".repeat(NewProductValidator.MAX_DESCRIPTION_LENGTH + 1)).build()))
                .containsOnlyKeys("description");
    }

    @Test
    void contentIsOptionalButAmountAndUnitGoTogether() {
        assertThat(errorsOf(aValidProduct().netContent(null, null).build())).isEmpty();
        assertThat(errorsOf(aValidProduct().netContent("500", null).build())).containsOnlyKeys("contentUnit");
        assertThat(errorsOf(aValidProduct().netContent(null, ContentUnit.G).build()))
                .containsOnlyKeys("netContentAmount");
    }

    @Test
    void contentMustBePositiveWithAtMostTwoDecimals() {
        assertThat(errorsOf(aValidProduct().netContent("0", ContentUnit.G).build()).get("netContentAmount"))
                .contains("mayor que cero");
        assertThat(errorsOf(aValidProduct().netContent("1.255", ContentUnit.KG).build()).get("netContentAmount"))
                .contains("dos decimales");
        assertThat(errorsOf(aValidProduct().netContent("1.25", ContentUnit.KG).build())).isEmpty();
        assertThat(errorsOf(aValidProduct().netContent("500000", ContentUnit.G).build()).get("netContentAmount"))
                .contains("demasiado alto");
    }

    @Test
    void unitsPerPackageAreWhole() {
        assertThat(errorsOf(aValidProduct().netContent("6.5", ContentUnit.UND).build()).get("netContentAmount"))
                .contains("enteras");
        assertThat(errorsOf(aValidProduct().netContent("6", ContentUnit.UND).build())).isEmpty();
        assertThat(errorsOf(aValidProduct().netContent("6.00", ContentUnit.UND).build())).isEmpty();
    }

    @Test
    void anEditIsValidatedWithoutBarcodeOrInitialStock() {
        ProductData data = aValidProduct().barcode("no importa").initialStock(-5, TODAY.minusDays(3))
                .salePrice("100").cost("200").buildData();

        assertThat(validator.validate(data)).containsOnlyKeys("salePrice");
        assertThat(validator.validate(aValidProduct().buildData())).isEmpty();
    }

    private Map<String, String> errorsOf(NewProduct product) {
        return validator.validate(product, TODAY);
    }
}
