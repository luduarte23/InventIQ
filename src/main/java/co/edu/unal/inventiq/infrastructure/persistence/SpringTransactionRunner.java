package co.edu.unal.inventiq.infrastructure.persistence;

import java.util.function.Supplier;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.unal.inventiq.application.TransactionRunner;
import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;

/**
 * Transacción de Spring para los servicios de application. Un conflicto de concurrencia
 * que aparezca al hacer commit (bloqueo optimista o espera de bloqueo en H2) se reporta
 * como ConcurrentStockUpdateException para que el servicio pueda reintentar.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate template;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        try {
            return template.execute(status -> work.get());
        } catch (ConcurrencyFailureException e) {
            throw new ConcurrentStockUpdateException(e);
        }
    }
}
