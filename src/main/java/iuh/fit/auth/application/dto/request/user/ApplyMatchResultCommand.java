package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.SportType;

import java.util.UUID;

/** Kết quả một trận ghép (1 đấu 1). Các trường tên/tỉ số chỉ để ghi lịch sử thi đấu. */
public record ApplyMatchResultCommand(
        UUID sessionId,
        UUID userId,
        SportType sportType,
        Integer opponentElo,
        String outcome,
        Integer playerElo,
        String opponentName,
        Integer myScore,
        Integer opponentScore
) {
    public ApplyMatchResultCommand(UUID sessionId, UUID userId, SportType sportType, Integer opponentElo,
                                   String outcome, Integer playerElo) {
        this(sessionId, userId, sportType, opponentElo, outcome, playerElo, null, null, null);
    }
}
