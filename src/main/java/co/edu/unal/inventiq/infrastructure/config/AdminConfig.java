package co.edu.unal.inventiq.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.unal.inventiq.application.TransactionRunner;
import co.edu.unal.inventiq.application.admin.CatalogService;
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SupplierRepository;

/** Cableado de los servicios de administración (D). Ver SalesConfig para el porqué del new. */
@Configuration
public class AdminConfig {

    @Bean
    public CatalogService catalogService(ProductRepository products, BatchRepository batches,
                                         SupplierRepository suppliers, PriceHistoryRepository priceHistory,
                                         TransactionRunner transactions, Clock clock) {
        return new CatalogService(products, batches, suppliers, priceHistory, transactions, clock);
    }
}
