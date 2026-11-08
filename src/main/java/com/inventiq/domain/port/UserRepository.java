/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// UserRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);

    // Propuesta A2: la necesita AuthenticationService.hasRole(userId, role) (§6.1).
    Optional<User> findById(Long id);
}
