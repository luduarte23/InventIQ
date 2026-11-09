/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// AuthenticationService.java
package com.inventiq.application.security;

import com.inventiq.domain.model.Role;

public interface AuthenticationService {

    /** @throws com.inventiq.domain.model.exception.InvalidCredentialsException */
    AuthenticatedUser login(String username, String rawPassword);

    boolean hasRole(Long userId, Role role);
}
