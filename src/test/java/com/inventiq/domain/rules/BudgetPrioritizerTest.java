package com.inventiq.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas de la priorización por presupuesto, sin Spring ni base de datos. */
class BudgetPrioritizerTest {

    private final BudgetPrioritizer prioritizer = new BudgetPrioritizer();

    @Test
    void ifEverythingFitsAllOrdersAreFunded() {
        BudgetAllocation result = prioritizer.prioritize(List.of(
                candidate(1L, "200000", "10000"),
                candidate(2L, "250000", "20000")), money("500000"));

        assertEquals(List.of(2L, 1L), result.fundedOrderIds());
        assertEquals(List.of(), result.deferredOrderIds());
        assertEquals(money("450000"), result.totalFunded());
    }

    @Test
    void theOrderWithMoreDailyProfitGoesFirst() {
        // Solo cabe uno de los dos: gana el que deja 20.000 por día.
        BudgetAllocation result = prioritizer.prioritize(List.of(
                candidate(1L, "300000", "10000"),
                candidate(2L, "300000", "20000")), money("500000"));

        assertEquals(List.of(2L), result.fundedOrderIds());
        assertEquals(List.of(1L), result.deferredOrderIds());
        assertEquals(money("300000"), result.totalFunded());
    }

    @Test
    void aSmallerOrderCanStillFitAfterABigOneIsDeferred() {
        // Quedan 200.000 tras el primero: el de 300.000 no cabe, el de 150.000 sí.
        BudgetAllocation result = prioritizer.prioritize(List.of(
                candidate(1L, "300000", "10000"),
                candidate(2L, "300000", "20000"),
                candidate(3L, "150000", "5000")), money("500000"));

        assertEquals(List.of(2L, 3L), result.fundedOrderIds());
        assertEquals(List.of(1L), result.deferredOrderIds());
        assertEquals(money("450000"), result.totalFunded());
    }

    @Test
    void withABudgetOfZeroEverythingIsDeferred() {
        BudgetAllocation result = prioritizer.prioritize(List.of(
                candidate(1L, "100000", "10000")), money("0"));

        assertEquals(List.of(), result.fundedOrderIds());
        assertEquals(List.of(1L), result.deferredOrderIds());
        assertEquals(money("0"), result.totalFunded());
    }

    @Test
    void withTheSameDailyProfitTheLowerIdGoesFirst() {
        BudgetAllocation result = prioritizer.prioritize(List.of(
                candidate(2L, "300000", "10000"),
                candidate(1L, "300000", "10000")), money("300000"));

        assertEquals(List.of(1L), result.fundedOrderIds());
    }

    @Test
    void theDailyProfitIsTheMarginTimesTheUnitsSoldPerDay() {
        // Yogur: se vende a 3.200, cuesta 2.100 y se venden 2 por día: 2.200 por día.
        assertEquals(money("2200.00"),
                BudgetPrioritizer.dailyProfit(money("3200"), money("2100"), new BigDecimal("2.0000")));
        // Si se vende por debajo del costo, no suma ganancia.
        assertEquals(money("0.00"),
                BudgetPrioritizer.dailyProfit(money("1000"), money("1500"), new BigDecimal("2.0000")));
    }

    private static BudgetCandidate candidate(Long orderId, String cost, String dailyProfit) {
        return new BudgetCandidate(orderId, money(cost), money(dailyProfit));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
