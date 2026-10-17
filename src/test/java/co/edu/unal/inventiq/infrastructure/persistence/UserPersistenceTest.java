package co.edu.unal.inventiq.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unal.inventiq.domain.model.Role;
import co.edu.unal.inventiq.domain.model.User;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.UserRepositoryAdapter;

/** Repositorio de usuarios (A5) contra H2 con el esquema real de Flyway, que ya trae admin y cajero. */
@DataJpaTest
@Import(UserRepositoryAdapter.class)
class UserPersistenceTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepositoryAdapter users;

    @Test
    void theInitialUsersExistWithTheirRoles() {
        User admin = users.findByUsername("admin").orElseThrow();
        User cashier = users.findByUsername("cajero").orElseThrow();

        assertThat(admin.id()).isNotNull();
        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(admin.isAdmin()).isTrue();
        assertThat(admin.active()).isTrue();
        assertThat(cashier.role()).isEqualTo(Role.CASHIER);
        assertThat(cashier.isAdmin()).isFalse();
    }

    @Test
    void passwordsAreStoredAsBcryptHashesNeverInPlainText() {
        User admin = users.findByUsername("admin").orElseThrow();

        assertThat(admin.passwordHash())
                .startsWith("$2a$10$")
                .hasSize(60)
                .doesNotContain("admin123");
    }

    @Test
    void anUnknownUsernameIsNotFound() {
        assertThat(users.findByUsername("nadie")).isEmpty();
    }

    @Test
    void inactiveUsersAreReturnedSoTheLoginCanRejectThem() {
        jdbc.update("insert into app_user (username, password_hash, role, active) values (?, ?, ?, ?)",
                "exempleado", "$2a$10$" + "x".repeat(53), "CASHIER", false);

        User former = users.findByUsername("exempleado").orElseThrow();

        assertThat(former.active()).isFalse();
        assertThat(former.role()).isEqualTo(Role.CASHIER);
    }
}
