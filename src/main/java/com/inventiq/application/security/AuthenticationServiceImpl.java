package com.inventiq.application.security;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.domain.model.exception.InvalidCredentialsException;
import com.inventiq.domain.port.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Login y verificación de roles (ACUERDOS §6.1, FR-01).
 * Las contraseñas se comparan con BCrypt: en la base solo está el hash, nunca el texto.
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    /**
     * Hash BCrypt de una contraseña cualquiera. Se compara cuando el usuario no existe, para que
     * la respuesta tarde lo mismo que con un usuario real y el tiempo no delate qué usuarios existen.
     */
    private static final String DUMMY_HASH = "$2a$10$eoOYon5r7zCU6foBsblMSO/vRZGfoS8GJBFdL0uN10AcJctAOb0gO";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Devuelve el usuario si el nombre y la contraseña son correctos y está activo.
     * En cualquier otro caso lanza la misma excepción, con el mismo mensaje: así no se revela
     * si falló el usuario, la contraseña o si la cuenta está desactivada.
     */
    @Override
    public AuthenticatedUser login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            throw new InvalidCredentialsException();
        }
        Optional<User> found = users.findByUsername(username.trim());
        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword, DUMMY_HASH);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash()) || !user.isActive()) {
            throw new InvalidCredentialsException();
        }
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
    }

    /** true solo si el usuario existe, está activo y tiene ese rol. */
    @Override
    public boolean hasRole(Long userId, Role role) {
        if (userId == null || role == null) {
            return false;
        }
        return users.findById(userId)
                .map(user -> user.isActive() && user.getRole() == role)
                .orElse(false);
    }
}