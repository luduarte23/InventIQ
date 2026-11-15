/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// ProductRepository.java
package com.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import com.inventiq.domain.model.Product;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    Optional<Product> findByBarcode(String barcode);
    List<Product> findAllActive();
    List<Product> findAll();
    Product save(Product product);
}
