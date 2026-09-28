package iuh.fit.auth.presentation.response.user;

import iuh.fit.auth.domain.entity.AppliedMatchResult;

import java.time.LocalDateTime;
import java.util.UUID;

/** Một dòng "Lịch sử thi đấu" trong hồ sơ thể thao. */
public record MatchHistoryResponse(
        UUID resultId,
        String source,
        UUID referenceId,
        String title,
        String opponentName,
        Integer myScore,
        Integer opponentScore,
        Integer opponentElo,
        String outcome,
        Integer oldElo,
        Integer newElo,
        Integer delta,
        LocalDateTime playedAt
) {
    public static MatchHistoryResponse from(AppliedMatchResult result) {
        return new MatchHistoryResponse(result.getResultId(), result.getSource(), result.getReferenceId(),
                result.getTitle(), result.getOpponentName(), result.getMyScore(), result.getOpponentScore(),
                result.getOpponentElo(), result.getOutcome(), result.getOldElo(), result.getNewElo(),
                result.delta(), result.getAppliedAt());
    }
}
