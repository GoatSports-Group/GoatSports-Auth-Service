package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.SportType;

import java.util.UUID;

public record ApplyMatchResultCommand(
        UUID sessionId,
        UUID userId,
        SportType sportType,
        Integer opponentElo,
        String outcome,
        Integer playerElo
) {
}
