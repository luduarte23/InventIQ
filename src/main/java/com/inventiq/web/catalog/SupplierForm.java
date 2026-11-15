package com.inventiq.web.catalog;

import java.math.BigDecimal;

import com.inventiq.application.catalog.SupplierData;
import com.inventiq.domain.model.Supplier;

/** Lo que llena el formulario de proveedor. Los mínimos vacíos llegan como null: "sin mínimo". */
public class SupplierForm {

    private String name;
    private String whatsappNumber;
    private int leadTimeDays;
    private BigDecimal minOrderAmount;
    private Integer minOrderUnits;

    public static SupplierForm from(Supplier supplier) {
        SupplierForm form = new SupplierForm();
        form.name = supplier.getName();
        form.whatsappNumber = supplier.getWhatsappNumber();
        form.leadTimeDays = supplier.getLeadTimeDays();
        form.minOrderAmount = supplier.getMinOrderAmount();
        form.minOrderUnits = supplier.getMinOrderUnits();
        return form;
    }

    public SupplierData toData() {
        return new SupplierData(trim(name), trim(whatsappNumber), leadTimeDays, minOrderAmount,
                minOrderUnits);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public Integer getMinOrderUnits() { return minOrderUnits; }
    public void setMinOrderUnits(Integer minOrderUnits) { this.minOrderUnits = minOrderUnits; }
}