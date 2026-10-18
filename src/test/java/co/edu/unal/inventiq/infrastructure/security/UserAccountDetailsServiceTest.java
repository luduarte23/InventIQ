package co.edu.unal.inventiq.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import co.edu.unal.inventiq.domain.model.Role;
import co.edu.unal.inventiq.domain.model.User;

/** A8: cómo pasan los usuarios del dominio a Spring Security. Sin Spring ni base de datos. */
class UserAccountDetailsServiceTest {

    private static final String HASH = "$2a$10$" + "x".repeat(53);

    private final Map<String, User> users = Map.of(
            "admin", new User(1L, "admin", HASH, Role.ADMIN, true),
            "cajero", new User(2L, "cajero", HASH, Role.CASHIER, true),
            "exempleado", new User(3L, "exempleado", HASH, Role.CASHIER, false));

    private final UserAccountDetailsService service =
            new UserAccountDetailsService(username -> Optional.ofNullable(users.get(username)));

    @Test
    void theAdministratorGetsOnlyTheAdminRole() {
        UserDetails admin = service.loadUserByUsername("admin");

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    void theCashierGetsOnlyTheCashierRole() {
        assertThat(service.loadUserByUsername("cajero").getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CASHIER");
    }

    @Test
    void theStoredHashIsPassedAsIsNeverAPlainPassword() {
        assertThat(service.loadUserByUsername("admin").getPassword()).isEqualTo(HASH);
    }

    @Test
    void anInactiveUserIsDisabled() {
        assertThat(service.loadUserByUsername("exempleado").isEnabled()).isFalse();
    }

    @Test
    void anUnknownUserIsReportedAsNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
