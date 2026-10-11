package iuh.fit.auth.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.Map;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserSummaryResponseCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.mapper.UserEntityMapper;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
    private final JpaRoleRepository jpaRoleRepository;

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
        if (user.getRole() == null || user.getRole().getRoleId() == null) {
            throw new ServiceException("Người dùng phải có vai trò hợp lệ.");
        }
        var roleEntity = jpaRoleRepository.findById(user.getRole().getRoleId())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại."));

        UserEntity entity;
        if (user.getUserId() != null) {
            entity = jpaUserRepository.findById(user.getUserId())
                    .map(existing -> {
                        userEntityMapper.updateEntity(user, existing, roleEntity);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Người dùng không tồn tại."));
        } else {
            entity = userEntityMapper.createEntity(user);
            entity.setRole(roleEntity);
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
    public List<UserReportResponseCommand> findAllForReport() {
        return jpaUserRepository.findAllForReport().stream()
                .map(row -> new UserReportResponseCommand(row.userId(), row.fullName(), row.email(),
                        row.role().name(), row.status().getValue(), row.createdAt()))
                .toList();
    }

    @Override
    public List<UserSummaryResponseCommand> findSummariesByIds(Collection<UUID> userIds) {
        return userIds.isEmpty() ? List.of() : jpaUserRepository.findSummariesByIds(userIds);
    }

    // UUID nhỏ nhất: trang đầu của keyset không cần nhánh "after is null" (PostgreSQL so sánh uuid theo byte).
    private static final UUID KEYSET_START = new UUID(0L, 0L);

    @Override
    public List<UUID> findActiveIdsByRoles(Collection<String> roleNames, UUID after, int limit) {
        return jpaUserRepository.findIdsByStatusAndRoles(UserStatusEnum.ACTIVE, roles(roleNames),
                after == null ? KEYSET_START : after, PageRequest.of(0, limit));
    }

    @Override
    public long countActiveByRoles(Collection<String> roleNames) {
        return jpaUserRepository.countByStatusAndRoles(UserStatusEnum.ACTIVE, roles(roleNames));
    }

    private static List<RoleEnum> roles(Collection<String> roleNames) {
        return roleNames.stream().map(RoleEnum::valueOf).toList();
    }

    @Override
    public List<User> searchActivePlayers(String keyword, UUID excludedUserId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return jpaUserRepository.searchActivePlayers(
                        keyword.trim(),
                        excludedUserId,
                        UserStatusEnum.ACTIVE,
                        RoleEnum.PLAYER,
                        PageRequest.of(0, safeLimit)
                ).stream()
                .map(userEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findByRole_Name(String name) {
        try {
            return jpaUserRepository.findByRole_Name(RoleEnum.valueOf(name.toUpperCase()))
                    .map(userEntityMapper::toDomain);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Long> countByRole() {
        return toCountMap(jpaUserRepository.countGroupedByRole());
    }

    @Override
    public Map<String, Long> countByStatus() {
        return toCountMap(jpaUserRepository.countGroupedByStatus());
    }

    @Override
    public List<LocalDateTime> findCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
        return jpaUserRepository.findCreatedAtBetween(from, to);
    }

    private static Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) counts.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return counts;
    }
}
