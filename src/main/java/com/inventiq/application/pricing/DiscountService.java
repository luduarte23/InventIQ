/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// DiscountService.java
package com.inventiq.application.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DiscountService {

    /** Lo invoca el scheduler a diario (§7). */
    List<DiscountApplied> runDailyExpiryReview(LocalDate today);

    /** Respuesta del administrador a la alerta de US-04 esc. 2. */
    void setManualDiscount(Long batchId, BigDecimal percent, Long adminUserId);
}
