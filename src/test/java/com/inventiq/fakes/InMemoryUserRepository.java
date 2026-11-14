package com.inventiq.fakes;

import com.inventiq.domain.model.User;
import com.inventiq.domain.port.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso: guarda los usuarios en una lista, sin base de datos. Solo para pruebas. */
public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    /** Si el usuario es nuevo (sin id), le asigna uno, como haría la base de datos. */
    @Override
    public User save(User user) {
        User saved = user;
        if (user.getId() == null) {
            saved = new User(nextId++, user.getUsername(), user.getPasswordHash(),
                    user.getRole(), user.isActive());
        } else {
            users.removeIf(u -> u.getId().equals(user.getId()));
        }
        users.add(saved);
        return saved;
    }

    @Override
    public Optional<User> findById(Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }
}
