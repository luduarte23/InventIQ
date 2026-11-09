/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// DiscountApplied.java
package com.inventiq.application.pricing;

import java.math.BigDecimal;

public record DiscountApplied(Long batchId, BigDecimal previousPercent, BigDecimal newPercent) {}