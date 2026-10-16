package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.port.persistence.AccountAppealRepository;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.infrastructure.persistence.entity.AccountAppealEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaAccountAppealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountAppealRepositoryImpl implements AccountAppealRepository {
    private final JpaAccountAppealRepository repository;

    @Override
    public AccountAppeal save(AccountAppeal appeal) {
        AccountAppealEntity entity = new AccountAppealEntity();
        entity.setAppealId(appeal.getAppealId() == null ? UUID.randomUUID() : appeal.getAppealId());
        entity.setUserId(appeal.getUserId());
        entity.setEmail(appeal.getEmail());
        entity.setFullName(appeal.getFullName());
        entity.setSubject(appeal.getSubject());
        entity.setMessage(appeal.getMessage());
        entity.setStatus(appeal.getStatus());
        entity.setCreatedAt(appeal.getCreatedAt());
        entity.setReadAt(appeal.getReadAt());
        entity.setResolvedAt(appeal.getResolvedAt());
        entity.setReply(appeal.getReply());
        entity.setRepliedAt(appeal.getRepliedAt());
        entity.setRepliedBy(appeal.getRepliedBy());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<AccountAppeal> findById(UUID appealId) {
        return repository.findById(appealId).map(this::toDomain);
    }

    @Override
    public PageResult<AccountAppeal> find(AccountAppealStatus status, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(0, page - 1), pageSize);
        Page<AccountAppealEntity> result = status == null
                ? repository.findAllByOrderByCreatedAtDesc(request)
                : repository.findByStatusOrderByCreatedAtDesc(status, request);
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(), result.getTotalElements(),
                page, pageSize, result.getTotalPages());
    }

    @Override
    public long countByStatus(AccountAppealStatus status) {
        return repository.countByStatus(status);
    }

    private AccountAppeal toDomain(AccountAppealEntity entity) {
        return AccountAppeal.builder()
                .appealId(entity.getAppealId()).userId(entity.getUserId())
                .email(entity.getEmail()).fullName(entity.getFullName())
                .subject(entity.getSubject()).message(entity.getMessage()).status(entity.getStatus())
                .createdAt(entity.getCreatedAt()).readAt(entity.getReadAt()).resolvedAt(entity.getResolvedAt())
                .reply(entity.getReply()).repliedAt(entity.getRepliedAt()).repliedBy(entity.getRepliedBy())
                .build();
    }
}
