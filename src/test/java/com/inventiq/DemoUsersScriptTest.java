package com.inventiq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Los usuarios del script demo deben poder iniciar sesión con las contraseñas documentadas.
 * Lee el SQL directamente (sin Spring ni base de datos) y verifica cada hash BCrypt.
 */
class DemoUsersScriptTest {

    private static final Map<String, String> DEMO_PASSWORDS =
            Map.of("admin", "admin123", "cajero", "cajero123");

    @Test
    void demoUsersHaveTheDocumentedPasswords() throws IOException {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/demo/R__datos_demo.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Matcher users = Pattern.compile("'(admin|cajero)',\\s*'(\\$2a\\$[^']+)'").matcher(sql);

        int found = 0;
        while (users.find()) {
            found++;
            String username = users.group(1);
            assertTrue(encoder.matches(DEMO_PASSWORDS.get(username), users.group(2)),
                    "El hash de '" + username + "' no corresponde a su contraseña de demo");
        }
        assertEquals(2, found, "El script demo debe crear exactamente a admin y cajero");
    }
}
