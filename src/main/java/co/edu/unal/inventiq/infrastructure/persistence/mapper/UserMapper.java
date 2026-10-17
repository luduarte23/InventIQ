package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.Role;
import co.edu.unal.inventiq.domain.model.User;
import co.edu.unal.inventiq.infrastructure.persistence.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                Role.valueOf(entity.getRole()),
                entity.isActive());
    }
}
