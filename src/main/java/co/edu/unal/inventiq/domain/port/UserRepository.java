package co.edu.unal.inventiq.domain.port;

import java.util.Optional;

import co.edu.unal.inventiq.domain.model.User;

/** Puerto de usuarios. Lo usa el login (A7). */
public interface UserRepository {

    /** Incluye usuarios inactivos: el login decide si los deja entrar. */
    Optional<User> findByUsername(String username);
}
