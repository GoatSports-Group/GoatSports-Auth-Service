package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;

import java.util.Optional;
import java.util.UUID;

public interface AccountAppealRepository {
    AccountAppeal save(AccountAppeal appeal);

    Optional<AccountAppeal> findById(UUID appealId);

    /** Mới nhất trước; {@code status} null = mọi trạng thái. {@code page} tính từ 1. */
    PageResult<AccountAppeal> find(AccountAppealStatus status, int page, int pageSize);

    long countByStatus(AccountAppealStatus status);
}
