package com.inventiq.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.model.User;
import com.inventiq.infrastructure.persistence.adapter.SupplierRepositoryAdapter;
import com.inventiq.infrastructure.persistence.adapter.UserRepositoryAdapter;

import jakarta.persistence.EntityManager;

/**
 * Prueba los adaptadores de User y Supplier (A5) contra H2 con las tablas reales de Flyway.
 * @DataJpaTest levanta solo JPA y deshace los cambios al final de cada prueba.
 */
@DataJpaTest
@Import({UserRepositoryAdapter.class, SupplierRepositoryAdapter.class})
class UserSupplierPersistenceTest {

    @Autowired private UserRepositoryAdapter users;
    @Autowired private SupplierRepositoryAdapter suppliers;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    // --- User ---

    @Test
    void savesAUserAndFindsItByUsernameAndById() {
        Long id = users.save(new User(null, "admin", "$2a$10$hash", Role.ADMIN, true)).getId();
        reloadFromDatabase();

        User byUsername = users.findByUsername("admin").get();
        User byId = users.findById(id).get();

        assertNotNull(id);
        assertEquals(id, byUsername.getId());
        assertEquals("$2a$10$hash", byUsername.getPasswordHash());
        assertEquals(Role.ADMIN, byUsername.getRole());
        assertTrue(byUsername.isActive());
        assertEquals("admin", byId.getUsername());
    }

    @Test
    void anUnknownUsernameReturnsEmpty() {
        assertTrue(users.findByUsername("nadie").isEmpty());
    }

    @Test
    void savingAnExistingUserUpdatesTheSameRow() {
        User saved = users.save(new User(null, "cajero", "$2a$10$hash", Role.CASHIER, true));
        saved.changeRole(Role.ADMIN);
        saved.deactivate();

        users.save(saved);
        reloadFromDatabase();

        User read = users.findById(saved.getId()).get();
        assertEquals(Role.ADMIN, read.getRole());
        assertFalse(read.isActive());
        assertEquals(1, jdbc.queryForObject("select count(*) from users", Integer.class));
    }

    // --- Supplier ---

    @Test
    void findAllActiveOnlyReturnsActiveSuppliers() {
        insertSupplier("Lácteos del Valle", true);
        insertSupplier("Panadería La Espiga", true);
        insertSupplier("Proveedor retirado", false);

        List<Supplier> active = suppliers.findAllActive();

        assertEquals(2, active.size());
        assertTrue(active.stream().allMatch(Supplier::isActive));
    }

    @Test
    void findByIdMapsEveryColumnIncludingTheNullableMinimums() {
        // min_order_units no se inserta: queda NULL (proveedor con mínimo solo en dinero).
        jdbc.update("insert into suppliers (name, whatsapp_number, lead_time_days, "
                + "min_order_amount, active) values (?, ?, ?, ?, ?)",
                "Lácteos del Valle", "+573001234567", 3, new BigDecimal("150000.00"), true);
        Long id = jdbc.queryForObject("select id from suppliers", Long.class);

        Supplier supplier = suppliers.findById(id).get();

        assertEquals("Lácteos del Valle", supplier.getName());
        assertEquals("+573001234567", supplier.getWhatsappNumber());
        assertEquals(3, supplier.getLeadTimeDays());
        assertEquals(new BigDecimal("150000.00"), supplier.getMinOrderAmount());
        assertNull(supplier.getMinOrderUnits());
        assertTrue(supplier.hasMinimumOrder());
    }

    @Test
    void savesANewSupplierAndThenUpdatesTheSameRow() {
        Supplier saved = suppliers.save(new Supplier(null, "Lácteos", "+573001234567", 3,
                null, null, true));
        assertNotNull(saved.getId());

        saved.rename("Lácteos del Valle");
        saved.changeMinimumOrder(new BigDecimal("150000"), 24);
        saved.deactivate();
        suppliers.save(saved);
        reloadFromDatabase();

        Supplier reloaded = suppliers.findById(saved.getId()).get();
        assertEquals("Lácteos del Valle", reloaded.getName());
        assertEquals(0, new BigDecimal("150000").compareTo(reloaded.getMinOrderAmount()));
        assertEquals(24, reloaded.getMinOrderUnits());
        assertFalse(reloaded.isActive());
    }

    @Test
    void findAllIncludesInactiveSuppliersSortedByName() {
        insertSupplier("Zeta Distribuciones", true);
        insertSupplier("Alfa Retirado", false);

        List<String> names = suppliers.findAll().stream().map(Supplier::getName).toList();

        assertTrue(names.contains("Alfa Retirado"));
        assertTrue(names.indexOf("Alfa Retirado") < names.indexOf("Zeta Distribuciones"));
    }

    @Test
    void anUnknownSupplierIdReturnsEmpty() {
        assertTrue(suppliers.findById(999L).isEmpty());
    }

    private void insertSupplier(String name, boolean active) {
        jdbc.update("insert into suppliers (name, whatsapp_number, lead_time_days, active) "
                + "values (?, ?, ?, ?)", name, "+573001234567", 3, active);
    }

    /** Escribe en la base lo pendiente y olvida lo que JPA tenía en memoria, para leer de verdad de H2. */
    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}