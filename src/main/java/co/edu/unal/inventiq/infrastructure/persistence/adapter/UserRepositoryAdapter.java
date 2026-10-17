package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.User;
import co.edu.unal.inventiq.domain.port.UserRepository;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.UserJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.UserMapper;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserMapper::toDomain);
    }
}
