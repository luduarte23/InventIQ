/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// AuthenticatedUser.java
package com.inventiq.application.security;

import com.inventiq.domain.model.Role;

public record AuthenticatedUser(Long userId, String username, Role role) {}
