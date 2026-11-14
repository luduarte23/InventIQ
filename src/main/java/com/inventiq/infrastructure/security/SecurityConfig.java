package com.inventiq.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso (ACUERDOS §6.1, NFR-03): el cajero solo usa la pantalla de ventas y
 * todo lo demás exige rol ADMIN. Cerrar todo por defecto hace que cualquier pantalla nueva
 * quede protegida sin que nadie tenga que acordarse de configurarla.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Libre: la página de login, los archivos estáticos y la página de error
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                // La pantalla de ventas: cajero y administrador
                .requestMatchers("/pos", "/pos/**").hasAnyRole("CASHIER", "ADMIN")
                // La página de inicio solo redirige: basta con haber iniciado sesión
                .requestMatchers("/").authenticated()
                // Todo lo demás (pantallas de C y D, consola de H2): solo administrador
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            // La consola de H2 no envía el token CSRF y se muestra dentro de un <frame>
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
