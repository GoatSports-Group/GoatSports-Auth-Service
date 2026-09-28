package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.enums.SportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppliedMatchResultRepository {
    Optional<AppliedMatchResult> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    List<AppliedMatchResult> findBySessionId(UUID sessionId);

    List<AppliedMatchResult> findRecent(UUID userId, SportType sportType, int limit);

    AppliedMatchResult save(AppliedMatchResult result);

    void delete(UUID resultId);
}
