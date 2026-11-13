package com.inventiq.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventiq.domain.model.Settings;
import com.inventiq.domain.port.AlertRepository;
import com.inventiq.domain.port.AuditLogRepository;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.domain.port.SaleRepository;
import com.inventiq.domain.port.SettingsRepository;
import com.inventiq.domain.port.SupplierRepository;
import com.inventiq.domain.port.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Levanta la aplicación completa (con H2 en memoria, perfil test) y comprueba que cada
 * puerto de repositorio de ACUERDOS §6.7 tiene exactamente una implementación.
 * Si alguien agrega un puerto y olvida el adaptador, o crea dos, esta prueba lo dice por nombre.
 */
@SpringBootTest
class RepositoryWiringTest {

    private static final List<Class<?>> REPOSITORY_PORTS = List.of(
            UserRepository.class,
            SupplierRepository.class,
            ProductRepository.class,
            BatchRepository.class,
            SaleRepository.class,
            PurchaseOrderRepository.class,
            SettingsRepository.class,
            AuditLogRepository.class,
            AlertRepository.class);

    @Autowired private ApplicationContext context;

    @Test
    void everyRepositoryPortHasExactlyOneImplementation() {
        for (Class<?> port : REPOSITORY_PORTS) {
            assertEquals(1, context.getBeanNamesForType(port).length,
                    () -> "El puerto " + port.getSimpleName() + " debe tener exactamente una implementación");
        }
    }

    @Test
    void theSettingsRowExistsWhenTheApplicationStarts() {
        Settings settings = context.getBean(SettingsRepository.class).get();

        assertEquals(30, settings.getSalesWindowDays());
    }
}
