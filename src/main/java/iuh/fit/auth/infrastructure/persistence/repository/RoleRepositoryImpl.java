package iuh.fit.auth.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.RoleEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {
    private final JpaRoleRepository jpaRoleRepository;
    private final FilterParser filterParser;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final RoleEntityMapper roleEntityMapper;

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRoleRepository.findById(id).map(roleEntityMapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        try {
            return jpaRoleRepository.findByName(RoleEnum.valueOf(name.toUpperCase()))
                    .map(roleEntityMapper::toDomain);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Role save(Role role) throws ServiceException {
        RoleEntity entity;
        if (role.getRoleId() != null) {
            entity = jpaRoleRepository.findById(role.getRoleId())
                    .map(existing -> {
                        roleEntityMapper.updateEntity(role, existing);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Vai trò không tồn tại."));
        } else {
            entity = roleEntityMapper.createEntity(role);
        }
        RoleEntity savedEntity = jpaRoleRepository.save(entity);
        return roleEntityMapper.toDomain(savedEntity);
    }

    @Override
    public void delete(Role role) {
        RoleEntity entity = roleEntityMapper.createEntity(role);
        jpaRoleRepository.delete(entity);
    }

    @Override
    public PageResult<Role> findAll(PageFilter filter) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<RoleEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            spec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
        }

        Page<RoleEntity> entityPage = jpaRoleRepository.findAll(spec, pageable);
        List<Role> items = entityPage.getContent().stream()
                .map(roleEntityMapper::toDomain)
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
