/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// AuditLogRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.AuditLog;
import java.util.List;

public interface AuditLogRepository {
    AuditLog save(AuditLog entry);
    List<AuditLog> findRecent(int limit);
}
