package iuh.fit.auth.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.PermissionEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {
    private final JpaPermissionRepository jpaPermissionRepository;
    private final FilterParser filterParser;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final PermissionEntityMapper permissionEntityMapper;

    @Override
    public Optional<Permission> findById(UUID id) {
        return jpaPermissionRepository.findById(id).map(permissionEntityMapper::toDomain);
    }

    @Override
    public boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method) {
        return jpaPermissionRepository.existsByApiPathAndModuleAndMethod(apiPath, module, method);
    }

    @Override
    public Set<Permission> findByPermissionIdIn(Set<UUID> permissionIds) {
        Set<PermissionEntity> entities = jpaPermissionRepository.findByPermissionIdIn(permissionIds);
        return permissionEntityMapper.toDomainSet(entities);
    }

    @Override
    public Permission save(Permission permission) throws ServiceException {
        PermissionEntity entity;
        if (permission.getPermissionId() != null) {
            entity = jpaPermissionRepository.findById(permission.getPermissionId())
                    .map(existing -> {
                        permissionEntityMapper.updateEntity(permission, existing);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Quyền hạn không tồn tại."));
        } else {
            entity = permissionEntityMapper.createEntity(permission);
        }
        PermissionEntity savedEntity = jpaPermissionRepository.save(entity);
        return permissionEntityMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Permission permission) {
        PermissionEntity entity = permissionEntityMapper.createEntity(permission);
        jpaPermissionRepository.delete(entity);
    }

    @Override
    public PageResult<Permission> findAll(PageFilter filter) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<PermissionEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            spec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
        }

        Page<PermissionEntity> entityPage = jpaPermissionRepository.findAll(spec, pageable);
        List<Permission> items = entityPage.getContent().stream()
                .map(permissionEntityMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getTotalElements(),
                filter.page(),
                filter.size(),
                entityPage.getTotalPages()
        );
    }
}
