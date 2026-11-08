/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.inventiq.domain.port;

import com.inventiq.domain.model.AlertType;

public interface AlertService {

    void raise(AlertType type, String message, String relatedEntityType, Long relatedEntityId);
}
