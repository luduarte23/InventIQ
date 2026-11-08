/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// AlertRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.Alert;
import java.util.List;

public interface AlertRepository {
    Alert save(Alert alert);
    List<Alert> findUnresolved();
}
