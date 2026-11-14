package com.inventiq.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.application.security.AuthenticationServiceImpl;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.fakes.InMemoryUserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** El puente entre Spring Security y AuthenticationService, sin levantar Spring. */
class InventiqAuthenticationProviderTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final InventiqAuthenticationProvider provider =
            new InventiqAuthenticationProvider(new AuthenticationServiceImpl(users, encoder));

    @BeforeEach
    void setUp() {
        users.save(new User(null, "cajero", encoder.encode("cajero123"), Role.CASHIER, true));
    }

    @Test
    void correctCredentialsGiveAnAuthenticatedUserWithItsRole() {
        Authentication result = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("cajero", "cajero123"));

        assertTrue(result.isAuthenticated());
        AuthenticatedUser user = assertInstanceOf(AuthenticatedUser.class, result.getPrincipal());
        assertEquals("cajero", user.username());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_CASHIER")), List.copyOf(result.getAuthorities()));
    }

    @Test
    void wrongCredentialsAreRejectedTheWaySpringSecurityExpects() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("cajero", "otra")));
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("nadie", "cajero123")));
    }

    @Test
    void itOnlyHandlesUsernameAndPasswordLogins() {
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        assertFalse(provider.supports(Authentication.class));
    }
}