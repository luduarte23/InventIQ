package co.edu.unal.inventiq.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación. Todo lo que dependa de "hoy" (ROP, vencimientos)
 * recibe este Clock en lugar de llamar a LocalDate.now(), para que las pruebas
 * puedan fijar la fecha con Clock.fixed(...).
 */
@Configuration
public class ClockConfig {

    static final ZoneId STORE_ZONE = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(STORE_ZONE);
    }
}
