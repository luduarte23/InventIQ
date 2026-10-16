package co.edu.unal.inventiq.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class GtinValidatorTest {

    private final GtinValidator gtin = new GtinValidator();

    /** Ejemplos publicados por GS1 / Wikipedia para cada formato. */
    @ParameterizedTest
    @ValueSource(strings = {"96385074", "036000291452", "4006381333931", "10012345678902"})
    void acceptsValidCodesOfEveryStandardLength(String code) {
        assertThat(gtin.problemWith(code)).isEmpty();
    }

    @Test
    void ignoresSpacesAroundTheCodeBecauseScannersMaySendThem() {
        assertThat(gtin.problemWith("  4006381333931 ")).isEmpty();
    }

    @Test
    void aMistypedLastDigitSaysWhichOneWasExpected() {
        assertThat(gtin.problemWith("4006381333932"))
                .hasValueSatisfying(problem -> assertThat(problem).contains("debería ser 1"));
    }

    @Test
    void swappedDigitsAreDetected() {
        assertThat(gtin.problemWith("4006381339331")).isPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void asksForACodeWhenItIsMissing(String code) {
        assertThat(gtin.problemWith(code))
                .hasValueSatisfying(problem -> assertThat(problem).contains("Escanea o escribe"));
    }

    @Test
    void rejectsLetters() {
        assertThat(gtin.problemWith("40063813339A1"))
                .hasValueSatisfying(problem -> assertThat(problem).contains("solo puede tener números"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567890", "123456789012345"})
    void rejectsLengthsThatAreNotGtin(String code) {
        assertThat(gtin.problemWith(code))
                .hasValueSatisfying(problem -> assertThat(problem).contains("tiene " + code.length()));
    }

    @Test
    void computesTheGs1CheckDigit() {
        assertThat(GtinValidator.checkDigit("400638133393")).isEqualTo(1);
        assertThat(GtinValidator.checkDigit("770200100001")).isEqualTo(2);
        assertThat(GtinValidator.checkDigit("03600029145")).isEqualTo(2);
        assertThat(GtinValidator.checkDigit("000000000000")).isZero();
    }
}
