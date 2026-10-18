package co.edu.unal.inventiq.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** A8: las contraseñas se guardan con BCrypt y nunca en claro (FR-01, NFR-03). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:password-storage;DB_CLOSE_DELAY=-1")
class PasswordStorageTest {

    /** $2a$, $2b$ o $2y$, el costo en dos dígitos y 53 caracteres de sal + hash. */
    private static final String BCRYPT_HASH = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theApplicationEncodesPasswordsWithBcrypt() {
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.encode("cualquiera")).matches(BCRYPT_HASH);
    }

    @Test
    void everyStoredPasswordIsABcryptHashWithCostAtLeast10() {
        List<Map<String, Object>> users = jdbc.queryForList("select username, password_hash from app_user");

        assertThat(users).isNotEmpty();
        for (Map<String, Object> row : users) {
            String hash = (String) row.get("PASSWORD_HASH");
            assertThat(hash).as("hash de %s", row.get("USERNAME")).matches(BCRYPT_HASH);
            assertThat(Integer.parseInt(hash.substring(4, 6))).as("costo de %s", row.get("USERNAME"))
                    .isGreaterThanOrEqualTo(10);
        }
    }

    @Test
    void theInitialUsersHashesMatchTheirDocumentedPasswordsAndNothingElse() {
        String admin = hashOf("admin");
        String cashier = hashOf("cajero");

        assertThat(encoder.matches("admin123", admin)).isTrue();
        assertThat(encoder.matches("cajero123", cashier)).isTrue();
        assertThat(encoder.matches("cajero123", admin)).isFalse();
        assertThat(encoder.matches("Admin123", admin)).as("distingue mayúsculas").isFalse();
        assertThat(admin).doesNotContain("admin123");
        assertThat(cashier).doesNotContain("cajero123");
    }

    @Test
    void theSamePasswordGivesADifferentHashEachTime() {
        String first = encoder.encode("clave123");
        String second = encoder.encode("clave123");

        assertThat(first).isNotEqualTo(second);
        assertThat(encoder.matches("clave123", first)).isTrue();
        assertThat(encoder.matches("clave123", second)).isTrue();
    }

    private String hashOf(String username) {
        return jdbc.queryForObject("select password_hash from app_user where username = ?", String.class, username);
    }
}
