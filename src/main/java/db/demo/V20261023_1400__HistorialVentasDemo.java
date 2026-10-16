package db.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Historial de la demo (solo perfil {@code demo}): la evolución de los precios de compra y venta
 * de cada producto y 90 días de ventas simuladas, para que el historial y sus gráficas tengan
 * datos. Usa una semilla fija, así la demo es igual en todas las máquinas.
 *
 * <p>Las ventas siguen un ritmo realista: más tickets el fin de semana, picos a media mañana, al
 * almuerzo y al final de la tarde, productos de diario (lácteos, pan, huevos) más frecuentes y
 * una leve tendencia al alza. Cada venta descuenta de lotes históricos ya agotados, así que el
 * inventario actual de la demo no cambia. El precio cobrado es el que estaba vigente ese día.
 */
public class V20261023_1400__HistorialVentasDemo extends BaseJavaMigration {

    static final long SEED = 20261016L;
    static final int DAYS = 90;
    private static final int PAUSED_SINCE_DAYS = 12;
    private static final ZoneId STORE_ZONE = ZoneId.of("America/Bogota");

    /** Días atrás de cada cambio de precio y el factor sobre el precio actual (el último es el actual). */
    private static final int[] CHANGE_DAYS_AGO = {175, 118, 61, 19};
    private static final double[] PRICE_FACTORS = {0.88, 0.92, 0.96, 1.0};
    private static final double[] COST_FACTORS = {0.86, 0.91, 0.955, 1.0};

    private static final Map<String, Double> CATEGORY_POPULARITY = Map.ofEntries(
            Map.entry("LACTEOS", 9.0), Map.entry("PANADERIA", 8.0), Map.entry("BEBIDAS", 7.0),
            Map.entry("CARNES", 6.0), Map.entry("SNACKS", 5.0), Map.entry("GRANOS", 4.5),
            Map.entry("ENDULZANTES", 4.0), Map.entry("FRUTAS_VERDURAS", 4.0), Map.entry("CAFE", 3.0),
            Map.entry("ACEITES", 2.5), Map.entry("ENLATADOS", 2.0), Map.entry("ASEO", 1.8), Map.entry("OTROS", 2.0));

    private static final Map<DayOfWeek, Double> WEEKDAY_FACTOR = new EnumMap<>(Map.of(
            DayOfWeek.MONDAY, 0.85, DayOfWeek.TUESDAY, 0.80, DayOfWeek.WEDNESDAY, 0.90,
            DayOfWeek.THURSDAY, 0.95, DayOfWeek.FRIDAY, 1.15, DayOfWeek.SATURDAY, 1.40, DayOfWeek.SUNDAY, 1.10));

    /** Peso de cada hora de 7:00 a 20:00. */
    private static final int FIRST_HOUR = 7;
    private static final double[] HOUR_WEIGHTS = {4, 7, 6, 5, 6, 8, 7, 4, 4, 5, 8, 9, 7, 4};

    @Override
    public void migrate(Context context) throws SQLException {
        new Generator(context.getConnection(), LocalDate.now(STORE_ZONE), new Random(SEED)).run();
    }

    private record ProductRow(long id, BigDecimal salePrice, BigDecimal cost, boolean perishable, String category,
                              boolean saleEnabled) {
    }

    private record PricePoint(LocalDateTime from, BigDecimal salePrice, BigDecimal cost) {
    }

    private record Item(int product, int quantity, BigDecimal unitPrice) {
    }

    private record Ticket(LocalDateTime at, int daysAgo, List<Item> items) {
    }

    private static final class Generator {

        private final Connection connection;
        private final LocalDate today;
        private final Random random;

        Generator(Connection connection, LocalDate today, Random random) {
            this.connection = connection;
            this.today = today;
            this.random = random;
        }

        void run() throws SQLException {
            List<ProductRow> products = loadProducts();
            if (products.isEmpty()) {
                return;
            }
            long cashierId = cashierId();
            List<List<PricePoint>> timelines = writePriceHistory(products);
            List<Ticket> tickets = simulateTickets(products, timelines);
            Map<String, Long> batchIds = writeHistoricalBatches(products, tickets);
            writeSales(products, tickets, batchIds, cashierId);
        }

        private List<ProductRow> loadProducts() throws SQLException {
            List<ProductRow> products = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT id, sale_price, cost, perishable, category, sale_enabled FROM product ORDER BY id")) {
                while (rows.next()) {
                    products.add(new ProductRow(rows.getLong(1), rows.getBigDecimal(2), rows.getBigDecimal(3),
                            rows.getBoolean(4), rows.getString(5), rows.getBoolean(6)));
                }
            }
            return products;
        }

        private long cashierId() throws SQLException {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT id FROM app_user WHERE username = 'cajero'")) {
                if (!rows.next()) {
                    throw new IllegalStateException("La demo necesita el usuario 'cajero'");
                }
                return rows.getLong(1);
            }
        }

        /** Reemplaza el historial de precios por una evolución de seis meses que termina en el precio actual. */
        private List<List<PricePoint>> writePriceHistory(List<ProductRow> products) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM price_history");
            }
            List<List<PricePoint>> timelines = new ArrayList<>();
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO price_history (product_id, sale_price, cost, changed_at) VALUES (?, ?, ?, ?)")) {
                for (ProductRow product : products) {
                    List<PricePoint> timeline = new ArrayList<>();
                    for (int step = 0; step < CHANGE_DAYS_AGO.length; step++) {
                        boolean current = step == CHANGE_DAYS_AGO.length - 1;
                        int daysAgo = CHANGE_DAYS_AGO[step] + random.nextInt(11) - 5;
                        LocalDateTime from = today.minusDays(daysAgo).atTime(8, 30);
                        BigDecimal cost = current ? product.cost()
                                : roundTo(scale(product.cost(), COST_FACTORS[step] + jitter()), 50);
                        BigDecimal price = current ? product.salePrice()
                                : roundTo(scale(product.salePrice(), PRICE_FACTORS[step] + jitter()), 100).max(cost);
                        timeline.add(new PricePoint(from, price, cost));
                        insert.setLong(1, product.id());
                        insert.setBigDecimal(2, price);
                        insert.setBigDecimal(3, cost);
                        insert.setTimestamp(4, Timestamp.valueOf(from));
                        insert.addBatch();
                    }
                    timelines.add(timeline);
                }
                insert.executeBatch();
            }
            return timelines;
        }

        private List<Ticket> simulateTickets(List<ProductRow> products, List<List<PricePoint>> timelines) {
            double[] popularity = new double[products.size()];
            for (int i = 0; i < products.size(); i++) {
                popularity[i] = CATEGORY_POPULARITY.getOrDefault(products.get(i).category(), 2.0)
                        * (0.6 + random.nextDouble() * 0.8);
            }
            List<Ticket> tickets = new ArrayList<>();
            for (int daysAgo = DAYS; daysAgo >= 1; daysAgo--) {
                LocalDate day = today.minusDays(daysAgo);
                double trend = 0.85 + 0.30 * (DAYS - daysAgo) / DAYS;
                double noise = 0.85 + random.nextDouble() * 0.30;
                int count = (int) Math.round(16 * WEEKDAY_FACTOR.get(day.getDayOfWeek()) * trend * noise);
                for (int t = 0; t < count; t++) {
                    LocalDateTime at = day.atTime(sampleHour(), random.nextInt(60), random.nextInt(60));
                    List<Item> items = new ArrayList<>();
                    Set<Integer> chosen = new HashSet<>();
                    int lines = sampleLines();
                    for (int attempts = 0; items.size() < lines && attempts < 40; attempts++) {
                        int product = sampleProduct(popularity);
                        boolean paused = !products.get(product).saleEnabled() && daysAgo <= PAUSED_SINCE_DAYS;
                        if (paused || !chosen.add(product)) {
                            continue;
                        }
                        items.add(new Item(product, sampleQuantity(), priceAt(timelines.get(product), at)));
                    }
                    if (!items.isEmpty()) {
                        tickets.add(new Ticket(at, daysAgo, items));
                    }
                }
            }
            tickets.sort(Comparator.comparing(Ticket::at));
            return tickets;
        }

        /**
         * Un lote histórico por producto y por bloque de 30 días, con exactamente las unidades que
         * se vendieron de él: queda agotado y no aparece en el inventario actual.
         */
        private Map<String, Long> writeHistoricalBatches(List<ProductRow> products, List<Ticket> tickets)
                throws SQLException {
            Map<String, Integer> units = new HashMap<>();
            for (Ticket ticket : tickets) {
                for (Item item : ticket.items()) {
                    units.merge(batchKey(item.product(), block(ticket.daysAgo())), item.quantity(), Integer::sum);
                }
            }
            Map<String, Long> batchIds = new HashMap<>();
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO batch (product_id, quantity_received, quantity_available, expiration_date, received_at)"
                            + " VALUES (?, ?, 0, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                for (Map.Entry<String, Integer> entry : units.entrySet()) {
                    String[] key = entry.getKey().split(":");
                    ProductRow product = products.get(Integer.parseInt(key[0]));
                    LocalDateTime receivedAt = today.minusDays(30L * (Integer.parseInt(key[1]) + 1) + 2).atTime(7, 0);
                    insert.setLong(1, product.id());
                    insert.setInt(2, entry.getValue());
                    insert.setObject(3, product.perishable() ? receivedAt.toLocalDate().plusDays(40) : null);
                    insert.setTimestamp(4, Timestamp.valueOf(receivedAt));
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        keys.next();
                        batchIds.put(entry.getKey(), keys.getLong(1));
                    }
                }
            }
            return batchIds;
        }

        private void writeSales(List<ProductRow> products, List<Ticket> tickets, Map<String, Long> batchIds,
                                long cashierId) throws SQLException {
            try (PreparedStatement sale = connection.prepareStatement(
                    "INSERT INTO sale (cashier_id, created_at, total) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement item = connection.prepareStatement(
                         "INSERT INTO sale_item (sale_id, product_id, batch_id, quantity, unit_price) VALUES (?, ?, ?, ?, ?)")) {
                for (Ticket ticket : tickets) {
                    BigDecimal total = ticket.items().stream()
                            .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    sale.setLong(1, cashierId);
                    sale.setTimestamp(2, Timestamp.valueOf(ticket.at()));
                    sale.setBigDecimal(3, total);
                    sale.executeUpdate();
                    long saleId;
                    try (ResultSet keys = sale.getGeneratedKeys()) {
                        keys.next();
                        saleId = keys.getLong(1);
                    }
                    for (Item line : ticket.items()) {
                        item.setLong(1, saleId);
                        item.setLong(2, products.get(line.product()).id());
                        item.setLong(3, batchIds.get(batchKey(line.product(), block(ticket.daysAgo()))));
                        item.setInt(4, line.quantity());
                        item.setBigDecimal(5, line.unitPrice());
                        item.addBatch();
                    }
                }
                item.executeBatch();
            }
        }

        private static BigDecimal priceAt(List<PricePoint> timeline, LocalDateTime at) {
            PricePoint current = timeline.get(0);
            for (PricePoint point : timeline) {
                if (!point.from().isAfter(at)) {
                    current = point;
                }
            }
            return current.salePrice();
        }

        private int sampleHour() {
            return FIRST_HOUR + sampleIndex(HOUR_WEIGHTS);
        }

        private int sampleProduct(double[] popularity) {
            return sampleIndex(popularity);
        }

        private int sampleIndex(double[] weights) {
            double total = 0;
            for (double weight : weights) {
                total += weight;
            }
            double target = random.nextDouble() * total;
            for (int i = 0; i < weights.length; i++) {
                target -= weights[i];
                if (target < 0) {
                    return i;
                }
            }
            return weights.length - 1;
        }

        private int sampleLines() {
            double r = random.nextDouble();
            return r < 0.40 ? 1 : r < 0.70 ? 2 : r < 0.90 ? 3 : 4;
        }

        private int sampleQuantity() {
            double r = random.nextDouble();
            return r < 0.68 ? 1 : r < 0.90 ? 2 : 3;
        }

        private double jitter() {
            return (random.nextDouble() - 0.5) * 0.02;
        }

        private static int block(int daysAgo) {
            return (daysAgo - 1) / 30;
        }

        private static String batchKey(int product, int block) {
            return product + ":" + block;
        }

        private static BigDecimal scale(BigDecimal value, double factor) {
            return value.multiply(BigDecimal.valueOf(factor));
        }

        private static BigDecimal roundTo(BigDecimal value, int step) {
            BigDecimal unit = BigDecimal.valueOf(step);
            return value.divide(unit, 0, RoundingMode.HALF_UP).multiply(unit);
        }
    }
}
