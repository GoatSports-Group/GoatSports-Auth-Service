package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.mapper.PermissionMapper;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final JpaPermissionRepository jpaPermissionRepository;

    @Override
    public Optional<Permission> findById(UUID id) {
        return jpaPermissionRepository.findById(id).map(PermissionMapper::toDomain);
    }

    @Override
    public boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method) {
        return jpaPermissionRepository.existsByApiPathAndModuleAndMethod(apiPath, module, method);
    }

    @Override
    public Set<Permission> findByPermissionIdIn(Set<UUID> permissionIds) {
        Set<PermissionEntity> entities = jpaPermissionRepository.findByPermissionIdIn(permissionIds);
        return PermissionMapper.toDomainSet(entities);
    }

    @Override
    public Permission save(Permission permission) {
        PermissionEntity entity = PermissionMapper.toEntity(permission);
        PermissionEntity savedEntity = jpaPermissionRepository.save(entity);
        return PermissionMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Permission permission) {
        PermissionEntity entity = PermissionMapper.toEntity(permission);
        jpaPermissionRepository.delete(entity);
    }

    @Override
    public Page<Permission> findAll(Specification<PermissionEntity> spec, Pageable pageable) {
        return jpaPermissionRepository.findAll(spec, pageable).map(PermissionMapper::toDomain);
    }
}
