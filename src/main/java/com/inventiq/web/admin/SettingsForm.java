package com.inventiq.web.admin;

import java.math.BigDecimal;

import com.inventiq.domain.model.Settings;

/** Lo que llena el formulario de parámetros (FR-08). Spring copia cada campo con los setters. */
public class SettingsForm {

    private BigDecimal weeklyBudget;
    private int salesWindowDays;
    private int reviewPeriodDays;
    private int expiryReviewHorizonDays;
    private String adminWhatsappNumber;

    public static SettingsForm from(Settings settings) {
        SettingsForm form = new SettingsForm();
        form.weeklyBudget = settings.getWeeklyBudget();
        form.salesWindowDays = settings.getSalesWindowDays();
        form.reviewPeriodDays = settings.getReviewPeriodDays();
        form.expiryReviewHorizonDays = settings.getExpiryReviewHorizonDays();
        form.adminWhatsappNumber = settings.getAdminWhatsappNumber();
        return form;
    }

    /** El constructor de Settings valida los valores: si algo está mal, lanza IllegalArgumentException. */
    public Settings toSettings() {
        return new Settings(weeklyBudget, salesWindowDays, reviewPeriodDays, expiryReviewHorizonDays,
                adminWhatsappNumber == null ? null : adminWhatsappNumber.trim());
    }

    public BigDecimal getWeeklyBudget() { return weeklyBudget; }
    public void setWeeklyBudget(BigDecimal weeklyBudget) { this.weeklyBudget = weeklyBudget; }
    public int getSalesWindowDays() { return salesWindowDays; }
    public void setSalesWindowDays(int salesWindowDays) { this.salesWindowDays = salesWindowDays; }
    public int getReviewPeriodDays() { return reviewPeriodDays; }
    public void setReviewPeriodDays(int reviewPeriodDays) { this.reviewPeriodDays = reviewPeriodDays; }
    public int getExpiryReviewHorizonDays() { return expiryReviewHorizonDays; }
    public void setExpiryReviewHorizonDays(int days) { this.expiryReviewHorizonDays = days; }
    public String getAdminWhatsappNumber() { return adminWhatsappNumber; }
    public void setAdminWhatsappNumber(String number) { this.adminWhatsappNumber = number; }
}