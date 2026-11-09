/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.inventiq.domain.port;

public interface MessageNotifier {

    /**
     * Envía un mensaje de WhatsApp. NUNCA lanza excepción: si falla devuelve success = false,
     * y quien llamó registra una alerta NOTIFICATION_FAILED (ACUERDOS §5.6).
     */
    NotificationResult send(String whatsappNumber, String body);
}
