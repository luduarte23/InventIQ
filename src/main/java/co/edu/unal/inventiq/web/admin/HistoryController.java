package co.edu.unal.inventiq.web.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.unal.inventiq.application.reports.ProductPrices;
import co.edu.unal.inventiq.application.reports.SalesReport;
import co.edu.unal.inventiq.application.reports.SalesReportService;
import co.edu.unal.inventiq.web.ViewFormat;

/**
 * Administración → Historial: ventas del período con sus gráficas, y precios de compra y venta
 * de cada producto. Los datos de las gráficas van en la página como JSON (no hay API REST).
 *
 * @see HistoryChartData
 */
@Controller
@RequestMapping("/admin/history")
public class HistoryController {

    static final int DEFAULT_DAYS = 30;

    private final SalesReportService reports;
    private final ViewFormat fmt;

    public HistoryController(SalesReportService reports, ViewFormat fmt) {
        this.reports = reports;
        this.fmt = fmt;
    }

    /**
     * @param days    7, 30 o 90; cualquier otro valor usa 30
     * @param product producto preseleccionado en la gráfica de precios (botón "Historial" de Productos)
     */
    @GetMapping
    public String history(@RequestParam(defaultValue = "30") int days, @RequestParam(required = false) Long product,
                          Model model) {
        int period = SalesReportService.PERIODS.contains(days) ? days : DEFAULT_DAYS;
        SalesReport report = reports.report(period);
        List<ProductPrices> prices = reports.prices(period);
        Long selected = selectedProduct(product, report, prices);

        model.addAttribute("period", period);
        model.addAttribute("periods", SalesReportService.PERIODS);
        model.addAttribute("report", report);
        model.addAttribute("prices", prices);
        model.addAttribute("selectedProduct", selected);
        model.addAttribute("chartData", HistoryChartData.build(report, prices, selected, fmt));
        return "admin/history";
    }

    /** El producto pedido si existe; si no, el más vendido del período; si no, el primero. */
    private static Long selectedProduct(Long requested, SalesReport report, List<ProductPrices> prices) {
        if (requested != null && prices.stream().anyMatch(p -> p.product().id().equals(requested))) {
            return requested;
        }
        if (!report.byProduct().isEmpty()) {
            return report.byProduct().get(0).product().id();
        }
        return prices.isEmpty() ? null : prices.get(0).product().id();
    }
}
