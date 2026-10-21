package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;

import java.util.Optional;
import java.util.UUID;

public interface AccountAppealRepository {
    AccountAppeal save(AccountAppeal appeal);

    Optional<AccountAppeal> findById(UUID appealId);

    PageResult<AccountAppeal> find(AccountAppealStatus status, int page, int pageSize);

    long countByStatus(AccountAppealStatus status);

    void deleteByUserId(UUID userId);
}
