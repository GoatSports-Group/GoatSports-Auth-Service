package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.infrastructure.persistence.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByKeycloakId(UUID keycloakId) {
        return jpaUserRepository.findByKeycloakId(keycloakId).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByKeycloakId(UUID keycloakId) {
        return jpaUserRepository.existsByKeycloakId(keycloakId);
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return jpaUserRepository.existsByUsernameOrEmail(username, email);
    }

    @Override
    public Optional<User> findByUsernameOrEmailWithRole(String username, String email) {
        return jpaUserRepository.findByUsernameOrEmailWithRole(username, email).map(UserMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        UserEntity savedEntity = jpaUserRepository.save(entity);
        return UserMapper.toDomain(savedEntity);
    }

    @Override
    public Page<User> findAll(Specification<UserEntity> spec, Pageable pageable) {
        return jpaUserRepository.findAll(spec, pageable).map(UserMapper::toDomain);
    }
}
