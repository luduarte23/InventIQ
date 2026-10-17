package co.edu.unal.inventiq.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import co.edu.unal.inventiq.domain.port.MessageNotifier;

/** A6: el notificador de la Entrega 2 escribe en el log y nunca lanza excepciones. Sin Spring. */
@ExtendWith(OutputCaptureExtension.class)
class LogMessageNotifierTest {

    private final MessageNotifier notifier = new LogMessageNotifier();

    @Test
    void writesTheMessageToTheLogAndReportsItAsSent(CapturedOutput output) {
        boolean sent = notifier.send("+573001234567", "Pedido #12 aprobado: 2 productos, total $ 150.000");

        assertThat(sent).isTrue();
        assertThat(output.getOut())
                .contains("[WhatsApp simulado] Mensaje para +********4567")
                .contains("Pedido #12 aprobado: 2 productos, total $ 150.000");
    }

    @Test
    void neverWritesTheFullPhoneNumber(CapturedOutput output) {
        notifier.send("+573001234567", "Hola");

        assertThat(output.getOut()).doesNotContain("3001234567");
    }

    @Test
    void withoutANumberTheMessageIsNotSentButTheFlowContinues(CapturedOutput output) {
        assertThat(notifier.send(null, "Alerta: pedido por revisar")).isFalse();
        assertThat(notifier.send("   ", "Alerta: pedido por revisar")).isFalse();

        assertThat(output.getOut())
                .contains("no hay un número de WhatsApp configurado")
                .contains("Alerta: pedido por revisar");
    }

    @Test
    void aNumberWithoutInternationalFormatIsRejected(CapturedOutput output) {
        assertThat(notifier.send("3001234567", "Hola")).isFalse();
        assertThat(notifier.send("+57 300 123", "Hola")).isFalse();

        assertThat(output.getOut()).contains("no tiene formato internacional");
    }

    @Test
    void anEmptyMessageIsNotSent() {
        assertThat(notifier.send("+573001234567", null)).isFalse();
        assertThat(notifier.send("+573001234567", " ")).isFalse();
    }

    @Test
    void surroundingSpacesInTheNumberAreIgnored() {
        assertThat(notifier.send("  +573001234567 ", "Hola")).isTrue();
    }

    @Test
    void masksAllButTheLastFourDigits() {
        assertThat(LogMessageNotifier.mask("+573001234567")).isEqualTo("+********4567");
        assertThat(LogMessageNotifier.mask("123")).isEqualTo("123");
    }
}
