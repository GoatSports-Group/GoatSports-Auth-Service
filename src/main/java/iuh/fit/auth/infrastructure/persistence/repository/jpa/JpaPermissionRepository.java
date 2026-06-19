package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {
    boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method);

    Set<PermissionEntity> findByPermissionIdIn(Set<UUID> permissionIds);
}
