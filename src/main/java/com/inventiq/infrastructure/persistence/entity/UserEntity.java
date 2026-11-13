package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de la tabla 'users'. El rol se guarda como texto (VARCHAR), igual que en la
 * migración V1: la entidad lo lee como String y lo convierte al enum Role del dominio.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** JPA necesita un constructor vacío. */
    protected UserEntity() {
    }

    /** Del dominio a la tabla. */
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.id = user.getId();
        entity.username = user.getUsername();
        entity.passwordHash = user.getPasswordHash();
        entity.role = user.getRole().name();
        entity.active = user.isActive();
        return entity;
    }

    /** De la tabla al dominio. */
    public User toDomain() {
        return new User(id, username, passwordHash, Role.valueOf(role), active);
    }
}
