package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.infrastructure.persistence.entity.AppliedMatchResultEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaAppliedMatchResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppliedMatchResultRepositoryImpl implements AppliedMatchResultRepository {
    private final JpaAppliedMatchResultRepository repository;

    @Override
    public Optional<AppliedMatchResult> findBySessionIdAndUserId(UUID sessionId, UUID userId) {
        return repository.findBySessionIdAndUserId(sessionId, userId).map(this::toDomain);
    }

    @Override
    public AppliedMatchResult save(AppliedMatchResult result) {
        var entity = new AppliedMatchResultEntity();
        entity.setResultId(result.getResultId());
        entity.setSessionId(result.getSessionId());
        entity.setUserId(result.getUserId());
        entity.setSportType(result.getSportType());
        entity.setOutcome(result.getOutcome());
        entity.setOldElo(result.getOldElo());
        entity.setNewElo(result.getNewElo());
        entity.setAppliedAt(result.getAppliedAt());
        return toDomain(repository.save(entity));
    }

    private AppliedMatchResult toDomain(AppliedMatchResultEntity entity) {
        return AppliedMatchResult.builder()
                .resultId(entity.getResultId()).sessionId(entity.getSessionId())
                .userId(entity.getUserId()).sportType(entity.getSportType())
                .outcome(entity.getOutcome()).oldElo(entity.getOldElo())
                .newElo(entity.getNewElo()).appliedAt(entity.getAppliedAt()).build();
    }
}
