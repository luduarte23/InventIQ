package co.edu.unal.inventiq.domain.model;

import java.util.Objects;

/**
 * Usuario de la aplicación. La contraseña llega ya cifrada con BCrypt: el dominio nunca ve
 * la contraseña en claro.
 *
 * @param active false si el usuario ya no puede entrar
 */
public record User(Long id, String username, String passwordHash, Role role, boolean active) {

    public User {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
