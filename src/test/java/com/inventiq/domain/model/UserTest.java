package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de User: validaciones, rol, contraseña y activación. */
class UserTest {

    private static final String HASH = "$2a$10$hashDePrueba";

    @Test
    void aValidUserKeepsItsData() {
        User user = new User(1L, "cajero", HASH, Role.CASHIER, true);

        assertEquals(1L, user.getId());
        assertEquals("cajero", user.getUsername());
        assertEquals(HASH, user.getPasswordHash());
        assertEquals(Role.CASHIER, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    void usernameAndPasswordHashCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(null, null, HASH, Role.CASHIER, true));
        assertThrows(IllegalArgumentException.class,
                () -> new User(null, " ", HASH, Role.CASHIER, true));
        assertThrows(IllegalArgumentException.class,
                () -> new User(null, "cajero", "", Role.CASHIER, true));
    }

    @Test
    void roleIsRequired() {
        assertThrows(NullPointerException.class, () -> new User(null, "cajero", HASH, null, true));
    }

    @Test
    void roleAndPasswordHashCanBeChanged() {
        User user = user();

        user.changeRole(Role.ADMIN);
        user.changePasswordHash("$2a$10$otroHash");

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("$2a$10$otroHash", user.getPasswordHash());
    }

    @Test
    void invalidChangesAreRejected() {
        User user = user();

        assertThrows(NullPointerException.class, () -> user.changeRole(null));
        assertThrows(IllegalArgumentException.class, () -> user.changePasswordHash(" "));
    }

    @Test
    void aUserCanBeDeactivatedAndActivated() {
        User user = user();

        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void usersWithTheSameIdAreEqual() {
        User a = new User(1L, "cajero", HASH, Role.CASHIER, true);
        User b = new User(1L, "admin", HASH, Role.ADMIN, false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void usersWithoutIdAreOnlyEqualToThemselves() {
        User a = user();
        User b = user();

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    private static User user() {
        return new User(null, "cajero", HASH, Role.CASHIER, true);
    }
}
