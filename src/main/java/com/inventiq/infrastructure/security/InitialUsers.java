package com.inventiq.infrastructure.security;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Al arrancar la aplicación, crea los usuarios iniciales "admin" y "cajero" si no existen (FR-01).
 * Nunca modifica un usuario que ya existe, así que no pisa contraseñas cambiadas después.
 *
 * Las contraseñas vienen de application.properties (inventiq.initial-users.*) y no de una
 * migración: así no quedan escritas en el esquema y en producción se reemplazan con variables
 * de entorno, por ejemplo INVENTIQ_INITIALUSERS_ADMINPASSWORD.
 */
@Component
public class InitialUsers implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InitialUsers.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String cashierPassword;

    public InitialUsers(UserRepository users, PasswordEncoder passwordEncoder,
                        @Value("${inventiq.initial-users.admin-password}") String adminPassword,
                        @Value("${inventiq.initial-users.cashier-password}") String cashierPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.cashierPassword = cashierPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("admin", adminPassword, Role.ADMIN);
        createIfMissing("cajero", cashierPassword, Role.CASHIER);
    }

    private void createIfMissing(String username, String rawPassword, Role role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        users.save(new User(null, username, passwordEncoder.encode(rawPassword), role, true));
        LOG.info("Usuario inicial creado: {} ({})", username, role);
    }
}