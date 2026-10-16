package co.edu.unal.inventiq.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.unal.inventiq.application.reports.SalesReportService;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SaleRepository;

/** Cableado del historial y análisis (Administración → Historial). */
@Configuration
public class ReportsConfig {

    @Bean
    public SalesReportService salesReportService(SaleRepository sales, ProductRepository products,
                                                 PriceHistoryRepository priceHistory, Clock clock) {
        return new SalesReportService(sales, products, priceHistory, clock);
    }
}
