package iuh.fit.auth.infrastructure.persistence.repository;

import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.infrastructure.persistence.entity.AppliedMatchResultEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaAppliedMatchResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.util.List;
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
    public List<AppliedMatchResult> findBySessionId(UUID sessionId) {
        return repository.findBySessionId(sessionId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AppliedMatchResult> findRecent(UUID userId, SportType sportType, int limit) {
        return repository.findByUserIdAndSportTypeOrderByAppliedAtDesc(userId, sportType, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
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
        entity.setSource(result.getSource() == null ? AppliedMatchResult.SOURCE_MATCHMAKING : result.getSource());
        entity.setReferenceId(result.getReferenceId());
        entity.setTitle(result.getTitle());
        entity.setOpponentName(result.getOpponentName());
        entity.setMyScore(result.getMyScore());
        entity.setOpponentScore(result.getOpponentScore());
        entity.setOpponentElo(result.getOpponentElo());
        return toDomain(repository.save(entity));
    }

    @Override
    public void delete(UUID resultId) {
        repository.deleteById(resultId);
    }

    private AppliedMatchResult toDomain(AppliedMatchResultEntity entity) {
        return AppliedMatchResult.builder()
                .resultId(entity.getResultId()).sessionId(entity.getSessionId())
                .userId(entity.getUserId()).sportType(entity.getSportType())
                .outcome(entity.getOutcome()).oldElo(entity.getOldElo())
                .newElo(entity.getNewElo()).appliedAt(entity.getAppliedAt())
                .source(entity.getSource()).referenceId(entity.getReferenceId()).title(entity.getTitle())
                .opponentName(entity.getOpponentName()).myScore(entity.getMyScore())
                .opponentScore(entity.getOpponentScore()).opponentElo(entity.getOpponentElo())
                .build();
    }
}
