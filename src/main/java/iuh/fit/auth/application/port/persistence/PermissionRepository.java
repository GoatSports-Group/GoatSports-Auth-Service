package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository {
    Optional<Permission> findById(UUID id);
    boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method);
    Set<Permission> findByPermissionIdIn(Set<UUID> permissionIds);
    Permission save(Permission permission);
    void delete(Permission permission);
    Page<Permission> findAll(Specification<PermissionEntity> spec, Pageable pageable);
}
