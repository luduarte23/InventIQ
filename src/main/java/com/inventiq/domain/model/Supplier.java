/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;

public class Supplier {

    private Long id;
    private String name;
    private String whatsappNumber;
    private int leadTimeDays;
    private BigDecimal minOrderAmount;   // puede ser null: sin mínimo en dinero
    private Integer minOrderUnits;       // puede ser null: sin mínimo en unidades
    private boolean active;

    public Supplier(Long id, String name, String whatsappNumber, int leadTimeDays,
                    BigDecimal minOrderAmount, Integer minOrderUnits, boolean active) {
        this.id = id;
        this.name = requireText(name, "name");
        this.whatsappNumber = requireText(whatsappNumber, "whatsappNumber");
        this.leadTimeDays = requireNonNegative(leadTimeDays, "leadTimeDays");
        this.minOrderAmount = validateMinAmount(minOrderAmount);
        this.minOrderUnits = validateMinUnits(minOrderUnits);
        this.active = active;
    }

    public boolean hasMinimumOrder() {
        return minOrderAmount != null || minOrderUnits != null;
    }

    public void changeLeadTime(int newLeadTimeDays) {
        this.leadTimeDays = requireNonNegative(newLeadTimeDays, "leadTimeDays");
    }

    public void changeWhatsappNumber(String newNumber) {
        this.whatsappNumber = requireText(newNumber, "whatsappNumber");
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
        return value;
    }

    private static BigDecimal validateMinAmount(BigDecimal amount) {
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("minOrderAmount no puede ser negativo");
        }
        return amount;
    }

    private static Integer validateMinUnits(Integer units) {
        if (units != null && units < 0) {
            throw new IllegalArgumentException("minOrderUnits no puede ser negativo");
        }
        return units;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public Integer getMinOrderUnits() { return minOrderUnits; }
    public boolean isActive() { return active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Supplier other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
