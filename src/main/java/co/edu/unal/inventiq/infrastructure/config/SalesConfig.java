package co.edu.unal.inventiq.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.unal.inventiq.application.TransactionRunner;
import co.edu.unal.inventiq.application.sales.InventoryQueryService;
import co.edu.unal.inventiq.application.sales.SalesService;
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SaleRepository;

/**
 * Cableado de los servicios de la Parte B. Los servicios no llevan anotaciones de Spring;
 * se crean aquí con new para que las pruebas puedan armarlos igual, con fakes.
 * Cada letra tiene su propia clase *Config para no editar el mismo archivo.
 */
@Configuration
public class SalesConfig {

    @Bean
    public SalesService salesService(ProductRepository products, BatchRepository batches, SaleRepository sales,
                                     TransactionRunner transactions, Clock clock) {
        return new SalesService(products, batches, sales, transactions, clock);
    }

    @Bean
    public InventoryQueryService inventoryQueryService(ProductRepository products, BatchRepository batches,
                                                       Clock clock) {
        return new InventoryQueryService(products, batches, clock);
    }
}
