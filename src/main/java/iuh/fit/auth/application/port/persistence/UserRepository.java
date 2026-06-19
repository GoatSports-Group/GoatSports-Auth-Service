package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(UUID keycloakId);
    boolean existsByKeycloakId(UUID keycloakId);
    boolean existsByUsernameOrEmail(String username, String email);
    Optional<User> findByUsernameOrEmailWithRole(String username, String email);
    User save(User user);
    Page<User> findAll(Specification<UserEntity> spec, Pageable pageable);
}
