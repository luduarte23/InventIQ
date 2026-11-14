package com.inventiq.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Ayudante para pruebas con MockMvc: hace que una petición llegue "con sesión iniciada".
 *
 * Uso:  mockMvc.perform(get("/pos").with(TestUsers.cashier()))
 *
 * Se usa esto en vez de @WithMockUser porque nuestros controladores esperan que el usuario
 * de la sesión sea un AuthenticatedUser (igual que en la aplicación real). Con @WithMockUser
 * llegaría un objeto distinto y el controlador fallaría con ClassCastException.
 */
public final class TestUsers {

    /** Usuarios de prueba. Sus ids no existen en la base de datos: sirven para probar accesos. */
    public static final AuthenticatedUser CASHIER = new AuthenticatedUser(101L, "cajero-prueba", Role.CASHIER);
    public static final AuthenticatedUser ADMIN = new AuthenticatedUser(102L, "admin-prueba", Role.ADMIN);

    private TestUsers() {
    }

    /** La petición la hace un cajero. */
    public static RequestPostProcessor cashier() {
        return as(CASHIER);
    }

    /** La petición la hace un administrador. */
    public static RequestPostProcessor admin() {
        return as(ADMIN);
    }

    /** La petición la hace el usuario indicado, armado igual que InventiqAuthenticationProvider. */
    public static RequestPostProcessor as(AuthenticatedUser user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
    }
}