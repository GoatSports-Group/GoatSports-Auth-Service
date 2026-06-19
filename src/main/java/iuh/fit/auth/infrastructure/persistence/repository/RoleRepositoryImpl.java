package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.mapper.RoleMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final JpaRoleRepository jpaRoleRepository;

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRoleRepository.findById(id).map(RoleMapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRoleRepository.findByName(name).map(RoleMapper::toDomain);
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = RoleMapper.toEntity(role);
        RoleEntity savedEntity = jpaRoleRepository.save(entity);
        return RoleMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Role role) {
        RoleEntity entity = RoleMapper.toEntity(role);
        jpaRoleRepository.delete(entity);
    }

    @Override
    public Page<Role> findAll(Specification<RoleEntity> spec, Pageable pageable) {
        return jpaRoleRepository.findAll(spec, pageable).map(RoleMapper::toDomain);
    }
}
