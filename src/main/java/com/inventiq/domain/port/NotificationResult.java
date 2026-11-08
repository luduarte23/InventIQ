/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.inventiq.domain.port;

/** Resultado de un envío. success = false nunca interrumpe el flujo (ACUERDOS §5.6). */
public record NotificationResult(boolean success, String providerMessageId, String errorMessage) {

    public static NotificationResult ok(String providerMessageId) {
        return new NotificationResult(true, providerMessageId, null);
    }

    public static NotificationResult failed(String errorMessage) {
        return new NotificationResult(false, null, errorMessage);
    }
}
