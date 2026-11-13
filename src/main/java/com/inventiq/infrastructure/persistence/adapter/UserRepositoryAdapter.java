package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.User;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.infrastructure.persistence.entity.UserEntity;
import com.inventiq.infrastructure.persistence.jpa.UserJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa el puerto UserRepository del dominio usando JPA (A5). */
@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        return jpa.save(UserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }
}