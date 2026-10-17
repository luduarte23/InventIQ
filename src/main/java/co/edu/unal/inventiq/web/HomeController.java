package co.edu.unal.inventiq.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import co.edu.unal.inventiq.application.reports.SalesReportService;
import co.edu.unal.inventiq.application.sales.BatchView;
import co.edu.unal.inventiq.application.sales.InventoryOverview;
import co.edu.unal.inventiq.application.sales.InventoryQueryService;
import co.edu.unal.inventiq.application.sales.ProductStock;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/**
 * Inicio: las ventas de hoy, los indicadores del inventario y los lotes que necesitan atención.
 * Con el login (A7) el cajero irá directo a Ventas.
 */
@Controller
public class HomeController {

    static final int MAX_ALERTS = 8;

    private final InventoryQueryService inventory;
    private final SalesReportService reports;

    public HomeController(InventoryQueryService inventory, SalesReportService reports) {
        this.inventory = inventory;
        this.reports = reports;
    }

    @GetMapping("/")
    public String home(Model model) {
        InventoryOverview overview = inventory.overview();
        model.addAttribute("inventory", overview.summary());
        model.addAttribute("todaySales", reports.report(1).totals());
        model.addAttribute("alerts", alerts(overview));
        model.addAttribute("paused", overview.rows().stream().filter(row -> !row.product().saleEnabled()).toList());
        return "home";
    }

    /** Lotes vencidos con unidades y lotes por vencer, del más urgente al menos urgente. */
    private static List<Alert> alerts(InventoryOverview overview) {
        return overview.rows().stream()
                .flatMap(row -> row.batches().stream()
                        .filter(view -> view.status() == ExpirationStatus.EXPIRED
                                || view.status() == ExpirationStatus.EXPIRING_SOON)
                        .map(view -> new Alert(row, view)))
                .sorted(Comparator.comparing(alert -> alert.batch().daysUntilExpiration()))
                .limit(MAX_ALERTS)
                .toList();
    }

    public record Alert(ProductStock stock, BatchView batch) {
    }
}
