/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ReceiptLine.java
package com.inventiq.application.purchasing;

import java.time.LocalDate;

/** Cada línea recibida crea un Batch. expirationDate es null si el producto no es perecedero. */
public record ReceiptLine(Long productId, int quantityReceived, LocalDate expirationDate) {}
