package co.edu.unal.inventiq.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario de la tabla {@code app_user}. Por ahora de solo lectura: los usuarios iniciales vienen de
 * la migración y el login (A7) solo los consulta.
 */
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt, sin prefijo {bcrypt}. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** ADMIN | CASHIER, el nombre de {@code Role}. */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean active;

    protected UserEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
