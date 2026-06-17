package iuh.fit.auth.repository;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import iuh.fit.auth.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID>, JpaSpecificationExecutor<Permission> {
    boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method);

    Set<Permission> findByPermissionIdIn(Set<UUID> permissionIds);
}
