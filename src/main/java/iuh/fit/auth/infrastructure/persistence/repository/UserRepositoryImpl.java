package iuh.fit.auth.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.RoleEntityMapper;
import iuh.fit.auth.infrastructure.persistence.mapper.UserEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
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
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;
    private final FilterParser filterParser;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final UserEntityMapper userEntityMapper;
    private final RoleEntityMapper roleEntityMapper;

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id).map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByKeycloakId(UUID keycloakId) {
        return jpaUserRepository.findByKeycloakId(keycloakId).map(userEntityMapper::toDomain);
    }

    @Override
    public boolean existsByKeycloakId(UUID keycloakId) {
        return jpaUserRepository.existsByKeycloakId(keycloakId);
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        return jpaUserRepository.existsByUsernameOrEmail(username, email);
    }

    @Override
    public Optional<User> findByUsernameOrEmailWithRole(String username, String email) {
        return jpaUserRepository.findByUsernameOrEmailWithRole(username, email).map(userEntityMapper::toDomain);
    }

    @Override
    public User save(User user) throws ServiceException {
        UserEntity entity;
        if (user.getUserId() != null) {
            entity = jpaUserRepository.findById(user.getUserId())
                    .map(existing -> {
                        userEntityMapper.updateEntity(user, existing, roleEntityMapper.toEntity(user.getRole()));
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Người dùng không tồn tại."));
        } else {
            entity = userEntityMapper.createEntity(user);
        }
        UserEntity savedEntity = jpaUserRepository.save(entity);
        return userEntityMapper.toDomain(savedEntity);
    }

    @Override
    public PageResult<User> findAll(PageFilter filter) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<UserEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            spec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
        }

        Page<UserEntity> entityPage = jpaUserRepository.findAll(spec, pageable);
        List<User> items = entityPage.getContent().stream()
                .map(userEntityMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getTotalElements(),
                filter.page(),
                filter.size(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public List<User> findAllUsers() {
        List<UserEntity> userEntities = jpaUserRepository.findAll();
        return userEntities.stream()
                .map(userEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findByRole_Name(String name) {
        return jpaUserRepository.findByRole_Name(name).map(userEntityMapper::toDomain);
    }
}
