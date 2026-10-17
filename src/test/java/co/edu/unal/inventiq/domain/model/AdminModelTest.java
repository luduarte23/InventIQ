package co.edu.unal.inventiq.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.InvalidSettingsException;

/** Modelos pequeños de administración: parámetros, pedido mínimo, auditoría, alertas y usuarios. */
class AdminModelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);

    @Test
    void settingsStartWithA30DayWindowAndNoBudget() {
        Settings settings = Settings.defaults();

        assertThat(settings.salesWindowDays()).isEqualTo(30);
        assertThat(settings.hasBudget()).isFalse();
        assertThat(new Settings(new BigDecimal("400000"), 30, "+573001234567").hasBudget()).isTrue();
    }

    @Test
    void settingsRejectNegativeBudgetAndEmptyWindow() {
        assertThatThrownBy(() -> new Settings(new BigDecimal("-1"), 30, null))
                .isInstanceOf(InvalidSettingsException.class);
        assertThatThrownBy(() -> new Settings(null, 0, null))
                .isInstanceOf(InvalidSettingsException.class)
                .hasMessage("La ventana de ventas debe ser de al menos un día.");
    }

    /** US-02: el proveedor pide 500.000 COP y el borrador va en 400.000. */
    @Test
    void minimumOrderByAmount() {
        MinimumOrder moq = new MinimumOrder(MinimumOrderType.AMOUNT, new BigDecimal("500000"));

        assertThat(moq.isMetBy(new BigDecimal("400000"), 1000)).isFalse();
        assertThat(moq.isMetBy(new BigDecimal("500000"), 1)).isTrue();
    }

    @Test
    void minimumOrderByUnits() {
        MinimumOrder moq = new MinimumOrder(MinimumOrderType.UNITS, new BigDecimal("24"));

        assertThat(moq.isMetBy(new BigDecimal("999999"), 23)).isFalse();
        assertThat(moq.isMetBy(BigDecimal.ZERO, 24)).isTrue();
        assertThatThrownBy(() -> new MinimumOrder(MinimumOrderType.UNITS, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void auditEntriesKnowWhetherTheSystemDecided() {
        AuditEntry automatic = AuditEntry.automatic(AuditAction.DRAFT_ORDERS_GENERATED, "2 borradores", NOW);
        AuditEntry manual = AuditEntry.byUser("admin", AuditAction.SETTINGS_CHANGED, null, NOW);

        assertThat(automatic.isAutomatic()).isTrue();
        assertThat(manual.isAutomatic()).isFalse();
        assertThat(manual.action().label()).isEqualTo("Parámetros cambiados");
    }

    @Test
    void alertsStartUnresolved() {
        Alert alert = Alert.raise(AlertType.ORDER_NEEDS_REVIEW, "3 productos sin proveedor", NOW);

        assertThat(alert.resolved()).isFalse();
        assertThat(alert.resolve().resolved()).isTrue();
        assertThat(alert.resolve().message()).isEqualTo(alert.message());
        assertThat(alert.type().label()).isEqualTo("Pedido por revisar");
    }

    @Test
    void onlyAdminsAreAdmins() {
        assertThat(new User(1L, "admin", "$2a$hash", Role.ADMIN, true).isAdmin()).isTrue();
        assertThat(new User(2L, "cajero", "$2a$hash", Role.CASHIER, true).isAdmin()).isFalse();
        assertThat(Role.CASHIER.label()).isEqualTo("Cajero");
    }
}
