/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// SupplierRepository.java
package com.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import com.inventiq.domain.model.Supplier;

public interface SupplierRepository {
    Optional<Supplier> findById(Long id);
    List<Supplier> findAllActive();
    /** Activos e inactivos, ordenados por nombre. */
    List<Supplier> findAll();
    Supplier save(Supplier supplier);
}