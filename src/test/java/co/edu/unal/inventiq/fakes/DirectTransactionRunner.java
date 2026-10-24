package co.edu.unal.inventiq.fakes;

import java.util.function.Supplier;

import co.edu.unal.inventiq.application.TransactionRunner;

/**
 * Ejecuta el bloque directamente. No necesita deshacer nada porque los repositorios
 * falsos solo cambian datos en saveAll/save, al final del bloque.
 */
public class DirectTransactionRunner implements TransactionRunner {

    private int executions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        executions++;
        return work.get();
    }

    public int executions() {
        return executions;
    }
}
