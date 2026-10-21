package iuh.fit.auth.infrastructure.persistence.repository.jpa;

import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.infrastructure.persistence.entity.AccountAppealEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAccountAppealRepository extends JpaRepository<AccountAppealEntity, UUID> {
    Page<AccountAppealEntity> findByStatusOrderByCreatedAtDesc(AccountAppealStatus status, Pageable pageable);

    Page<AccountAppealEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(AccountAppealStatus status);

    void deleteByUserId(UUID userId);
}
