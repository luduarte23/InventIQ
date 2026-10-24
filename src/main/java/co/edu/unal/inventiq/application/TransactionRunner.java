package co.edu.unal.inventiq.application;

import java.util.function.Supplier;

/**
 * Ejecuta un bloque como una sola transacción: si lanza una excepción, nada de lo que
 * hizo queda guardado. Los servicios no usan @Transactional para poder armarse sin
 * Spring en las pruebas; la infraestructura implementa esta interfaz con Spring.
 */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);
}
