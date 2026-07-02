package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.dto.response.base.PageResult;

import java.util.List;
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

    PageResult<User> findAll(PageFilter filter);

    List<User> findAllUsers();

    Optional<User> findByRole_Name(String name);
}
