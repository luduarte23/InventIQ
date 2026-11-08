/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// SettingsRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.Settings;

public interface SettingsRepository {
    Settings get();
    Settings save(Settings settings);
}