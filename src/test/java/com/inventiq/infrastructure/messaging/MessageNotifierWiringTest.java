package com.inventiq.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.port.MessageNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Con la aplicación completa, quien pida un MessageNotifier recibe el notificador falso
 * (es el de por defecto mientras no exista el adaptador de Twilio).
 */
@SpringBootTest
class MessageNotifierWiringTest {

    @Autowired private MessageNotifier notifier;

    @Test
    void theApplicationUsesTheLoggingNotifierByDefault() {
        assertInstanceOf(LoggingMessageNotifier.class, notifier);
        assertTrue(notifier.send("+573001234567", "Prueba de cableado").success());
    }
}
