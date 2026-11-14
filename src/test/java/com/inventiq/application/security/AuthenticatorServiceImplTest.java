package com.inventiq.application.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.domain.model.exception.InvalidCredentialsException;
import com.inventiq.fakes.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pruebas del login (§6.1) sin Spring ni base de datos: repositorio en memoria y BCrypt real.
 * BCrypt con fuerza 4 (la mínima) solo para que las pruebas sean rápidas; la app usa la de defecto.
 */
class AuthenticationServiceImplTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AuthenticationServiceImpl service = new AuthenticationServiceImpl(users, encoder);

    private Long adminId;
    private Long cashierId;
    private Long inactiveId;

    @BeforeEach
    void setUp() {
        adminId = users.save(user("admin", "admin123", Role.ADMIN, true)).getId();
        cashierId = users.save(user("cajero", "cajero123", Role.CASHIER, true)).getId();
        inactiveId = users.save(user("retirado", "retirado123", Role.CASHIER, false)).getId();
    }

    // --- login ---

    @Test
    void correctCredentialsReturnTheAuthenticatedUser() {
        AuthenticatedUser user = service.login("admin", "admin123");

        assertEquals(adminId, user.userId());
        assertEquals("admin", user.username());
        assertEquals(Role.ADMIN, user.role());
    }

    @Test
    void spacesAroundTheUsernameAreIgnored() {
        assertEquals(cashierId, service.login("  cajero ", "cajero123").userId());
    }

    @Test
    void aWrongPasswordFails() {
        assertThrows(InvalidCredentialsException.class, () -> service.login("admin", "otra"));
    }

    @Test
    void anUnknownUserFailsWithTheSameMessageAsAWrongPassword() {
        String unknownUser = assertThrows(InvalidCredentialsException.class,
                () -> service.login("nadie", "admin123")).getMessage();
        String wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> service.login("admin", "otra")).getMessage();

        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    void anInactiveUserCannotLogInEvenWithTheRightPassword() {
        assertThrows(InvalidCredentialsException.class, () -> service.login("retirado", "retirado123"));
    }

    @Test
    void missingUsernameOrPasswordFails() {
        assertThrows(InvalidCredentialsException.class, () -> service.login(null, "admin123"));
        assertThrows(InvalidCredentialsException.class, () -> service.login("admin", null));
        assertThrows(InvalidCredentialsException.class, () -> service.login("admin", ""));
    }

    @Test
    void passwordsAreComparedCaseSensitively() {
        assertThrows(InvalidCredentialsException.class, () -> service.login("admin", "ADMIN123"));
    }

    // --- hasRole ---

    @Test
    void hasRoleIsTrueOnlyForTheUsersOwnRole() {
        assertTrue(service.hasRole(adminId, Role.ADMIN));
        assertFalse(service.hasRole(adminId, Role.CASHIER));
        assertTrue(service.hasRole(cashierId, Role.CASHIER));
        assertFalse(service.hasRole(cashierId, Role.ADMIN));
    }

    @Test
    void hasRoleIsFalseForInactiveUnknownOrMissingValues() {
        assertFalse(service.hasRole(inactiveId, Role.CASHIER));
        assertFalse(service.hasRole(999L, Role.ADMIN));
        assertFalse(service.hasRole(null, Role.ADMIN));
        assertFalse(service.hasRole(adminId, null));
    }

    /** Usuario nuevo con la contraseña ya convertida a hash, como estaría en la base. */
    private User user(String username, String rawPassword, Role role, boolean active) {
        return new User(null, username, encoder.encode(rawPassword), role, active);
    }
}