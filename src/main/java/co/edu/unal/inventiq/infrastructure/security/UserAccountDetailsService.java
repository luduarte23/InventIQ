package co.edu.unal.inventiq.infrastructure.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import co.edu.unal.inventiq.domain.port.UserRepository;

/**
 * Le dice a Spring Security quién es cada usuario, a partir del puerto del dominio. El rol del
 * dominio (ADMIN, CASHIER) se vuelve la autoridad ROLE_ADMIN o ROLE_CASHIER, y un usuario inactivo
 * queda deshabilitado: Spring rechaza su login aunque la contraseña sea correcta.
 */
public class UserAccountDetailsService implements UserDetailsService {

    private final UserRepository users;

    public UserAccountDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(user -> User.withUsername(user.username())
                        .password(user.passwordHash())
                        .roles(user.role().name())
                        .disabled(!user.active())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario " + username));
    }
}
