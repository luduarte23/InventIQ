package com.inventiq.application.security;

import com.inventiq.domain.model.Role;
import java.io.Serializable;

/**
 * El usuario con sesión iniciada (§6.1). Es Serializable porque Spring Security lo guarda
 * dentro de la sesión HTTP, y DevTools guarda las sesiones en disco al reiniciar la app.
 */
public record AuthenticatedUser(Long userId, String username, Role role) implements Serializable {}
