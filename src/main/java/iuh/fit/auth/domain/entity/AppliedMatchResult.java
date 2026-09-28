package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Một trận đã tính ELO cho một người chơi: đồng thời là một dòng lịch sử thi đấu của hồ sơ thể thao.
 * {@code sessionId} là phiên ghép trận hoặc trận của giải; mỗi (sessionId, userId) chỉ có một dòng.
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AppliedMatchResult {
    public static final String SOURCE_MATCHMAKING = "MATCHMAKING";
    public static final String SOURCE_TOURNAMENT = "TOURNAMENT";

    private UUID resultId;
    private UUID sessionId;
    private UUID userId;
    private SportType sportType;
    private String outcome;
    private Integer oldElo;
    private Integer newElo;
    private LocalDateTime appliedAt;
    private String source;
    private UUID referenceId;
    private String title;
    private String opponentName;
    private Integer myScore;
    private Integer opponentScore;
    private Integer opponentElo;

    public int delta() {
        return newElo - oldElo;
    }
}
