package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {
    Optional<Role> findById(UUID id);
    Optional<Role> findByName(String name);
    Role save(Role role);
    void delete(Role role);
    Page<Role> findAll(Specification<RoleEntity> spec, Pageable pageable);
}
