package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.SportType;

import java.util.List;
import java.util.UUID;

/**
 * Kết quả một trận giải đấu giữa hai bên (mỗi bên 1 người với đánh đơn, nhiều người với đôi/đội).
 * Gọi lại với cùng {@code matchId} (sửa tỉ số) thì lần tính cũ được hoàn lại rồi tính lại.
 */
public record ApplyTournamentMatchCommand(
        UUID matchId,
        UUID tournamentId,
        SportType sportType,
        String title,
        Side first,
        Side second
) {
    public record Side(String name, int score, List<UUID> playerIds) { }
}
