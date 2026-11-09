/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// OrderLineSuggestion.java
package com.inventiq.application.replenishment;

import java.math.BigDecimal;

public record OrderLineSuggestion(Long productId, int suggestedQuantity,
                                  BigDecimal lineCost, String reason) {}
