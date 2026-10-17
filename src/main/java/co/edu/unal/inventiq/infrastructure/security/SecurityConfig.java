package co.edu.unal.inventiq.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import co.edu.unal.inventiq.domain.port.UserRepository;

/**
 * Login con sesión y dos roles (FR-01, NFR-03).
 *
 * <ul>
 *   <li>Públicos: la página de login, la de error y los archivos estáticos.</li>
 *   <li>Ventas ({@code /pos}): administrador y cajero.</li>
 *   <li>Inicio ({@code /}): cualquier usuario con sesión; al cajero lo manda a Ventas.</li>
 *   <li>Todo lo demás: solo el administrador. Así las pantallas nuevas (C, D) nacen protegidas y el
 *       cajero recibe 403 en cualquier ruta de administración.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    static final String LOGIN_PAGE = "/login";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(LOGIN_PAGE, "/error", "/css/**", "/js/**", "/webjars/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/pos", "/pos/**").hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers("/").authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage(LOGIN_PAGE)
                        // Después del login va a lo que pidió, o al inicio, que redirige según el rol.
                        .defaultSuccessUrl("/", false)
                        .failureUrl(LOGIN_PAGE + "?error"))
                .logout(logout -> logout
                        .logoutSuccessUrl(LOGIN_PAGE + "?logout"));
        return http.build();
    }

    /** Los hashes de la base están en BCrypt sin prefijo {bcrypt} (migración de usuarios iniciales). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return new UserAccountDetailsService(users);
    }
}
