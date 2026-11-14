package com.inventiq.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.fakes.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Creación de los usuarios iniciales al arrancar, sin Spring ni base de datos. */
class InitialUsersTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final InitialUsers initialUsers = new InitialUsers(users, encoder, "admin123", "cajero123");

    @Test
    void createsAdminAndCashierWithTheirRolesAndHashedPasswords() {
        initialUsers.run(null);

        User admin = users.findByUsername("admin").get();
        User cashier = users.findByUsername("cajero").get();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.CASHIER, cashier.getRole());
        assertTrue(admin.isActive());
        // En la base queda el hash BCrypt, nunca la contraseña en texto.
        assertNotEquals("admin123", admin.getPasswordHash());
        assertTrue(encoder.matches("admin123", admin.getPasswordHash()));
        assertTrue(encoder.matches("cajero123", cashier.getPasswordHash()));
    }

    @Test
    void runningTwiceDoesNotDuplicateUsers() {
        initialUsers.run(null);
        Long adminId = users.findByUsername("admin").get().getId();

        initialUsers.run(null);

        assertEquals(adminId, users.findByUsername("admin").get().getId());
        assertTrue(users.findById(3L).isEmpty());   // solo existen los ids 1 y 2
    }

    @Test
    void anExistingUserIsNeverOverwritten() {
        String changedHash = encoder.encode("nueva-clave-del-admin");
        users.save(new User(null, "admin", changedHash, Role.ADMIN, true));

        initialUsers.run(null);

        assertEquals(changedHash, users.findByUsername("admin").get().getPasswordHash());
    }
}
