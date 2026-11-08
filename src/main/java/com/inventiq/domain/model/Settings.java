/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Settings {

    /** La tabla settings solo admite la fila con id = 1. */
    public static final long SINGLETON_ID = 1L;

    private BigDecimal weeklyBudget;
    private int salesWindowDays;
    private int reviewPeriodDays;
    private int expiryReviewHorizonDays;
    private String adminWhatsappNumber;

    public Settings(BigDecimal weeklyBudget, int salesWindowDays, int reviewPeriodDays,
                    int expiryReviewHorizonDays, String adminWhatsappNumber) {
        this.weeklyBudget = requireNonNegative(weeklyBudget);
        this.salesWindowDays = requirePositive(salesWindowDays, "salesWindowDays");
        this.reviewPeriodDays = requirePositive(reviewPeriodDays, "reviewPeriodDays");
        this.expiryReviewHorizonDays =
                requirePositive(expiryReviewHorizonDays, "expiryReviewHorizonDays");
        this.adminWhatsappNumber = requireText(adminWhatsappNumber, "adminWhatsappNumber");
    }

    /** Configuración inicial con los valores por defecto del V1 (30, 7 y 30 días). */
    public static Settings withDefaults(BigDecimal weeklyBudget, String adminWhatsappNumber) {
        return new Settings(weeklyBudget, 30, 7, 30, adminWhatsappNumber);
    }

    public void changeWeeklyBudget(BigDecimal newBudget) {
        this.weeklyBudget = requireNonNegative(newBudget);
    }

    public void changeSalesWindowDays(int days) {
        this.salesWindowDays = requirePositive(days, "salesWindowDays");
    }

    public void changeReviewPeriodDays(int days) {
        this.reviewPeriodDays = requirePositive(days, "reviewPeriodDays");
    }

    public void changeExpiryReviewHorizonDays(int days) {
        this.expiryReviewHorizonDays = requirePositive(days, "expiryReviewHorizonDays");
    }

    public void changeAdminWhatsappNumber(String number) {
        this.adminWhatsappNumber = requireText(number, "adminWhatsappNumber");
    }

    private static BigDecimal requireNonNegative(BigDecimal value) {
        Objects.requireNonNull(value, "weeklyBudget es obligatorio");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("weeklyBudget no puede ser negativo");
        }
        return value;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " debe ser mayor que 0");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public int getSalesWindowDays() { return salesWindowDays; }
    public int getReviewPeriodDays() { return reviewPeriodDays; }
    public int getExpiryReviewHorizonDays() { return expiryReviewHorizonDays; }
    public String getAdminWhatsappNumber() { return adminWhatsappNumber; }
}
