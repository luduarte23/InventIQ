package com.inventiq.infrastructure.security;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.application.security.AuthenticationService;
import com.inventiq.domain.model.exception.InvalidCredentialsException;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Conecta el formulario de login de Spring Security con nuestro AuthenticationService (§6.1).
 * Spring Security maneja la sesión, la cookie y el CSRF; la verificación del usuario y la
 * contraseña la hace siempre AuthenticationService.login(), que es la única lógica de login.
 *
 * Al ser el único AuthenticationProvider de la aplicación, Spring Security lo usa solo y además
 * desactiva el usuario "user" con contraseña generada que crea por defecto.
 */
@Component
public class InventiqAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationService authenticationService;

    public InventiqAuthenticationProvider(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        String rawPassword = credentials == null ? null : credentials.toString();
        try {
            AuthenticatedUser user = authenticationService.login(username, rawPassword);
            // Spring Security espera los roles con el prefijo "ROLE_": ADMIN -> ROLE_ADMIN.
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
            // El "principal" es nuestro AuthenticatedUser: los controladores lo reciben con
            // @AuthenticationPrincipal AuthenticatedUser user.
            return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
        } catch (InvalidCredentialsException e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }

    /** Solo atiende el login con usuario y contraseña (el del formulario). */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}