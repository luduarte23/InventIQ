package co.edu.unal.inventiq.application.reports;

import java.time.LocalTime;

/** Franja del día en que se hizo una venta. */
public enum TimeBand {

    MORNING("Mañana", "antes de las 12 m.", 0),
    AFTERNOON("Tarde", "de 12 m. a 5 p. m.", 12),
    EVENING("Noche", "desde las 5 p. m.", 17);

    private final String label;
    private final String range;
    private final int startHour;

    TimeBand(String label, String range, int startHour) {
        this.label = label;
        this.range = range;
        this.startHour = startHour;
    }

    public String label() {
        return label;
    }

    public String range() {
        return range;
    }

    public static TimeBand of(LocalTime time) {
        if (time.getHour() >= EVENING.startHour) {
            return EVENING;
        }
        return time.getHour() >= AFTERNOON.startHour ? AFTERNOON : MORNING;
    }
}
