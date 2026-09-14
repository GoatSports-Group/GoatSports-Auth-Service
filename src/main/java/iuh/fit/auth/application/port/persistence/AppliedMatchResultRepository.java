package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.domain.entity.AppliedMatchResult;

import java.util.Optional;
import java.util.UUID;

public interface AppliedMatchResultRepository {
    Optional<AppliedMatchResult> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    AppliedMatchResult save(AppliedMatchResult result);
}
