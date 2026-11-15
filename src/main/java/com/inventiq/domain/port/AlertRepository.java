/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// AlertRepository.java
package com.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import com.inventiq.domain.model.Alert;

public interface AlertRepository {
    Alert save(Alert alert);
    List<Alert> findUnresolved();
    Optional<Alert> findById(Long id);
}
