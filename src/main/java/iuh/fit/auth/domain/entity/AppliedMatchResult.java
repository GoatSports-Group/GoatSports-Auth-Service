package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedMatchResult {
    private UUID resultId;
    private UUID sessionId;
    private UUID userId;
    private SportType sportType;
    private String outcome;
    private Integer oldElo;
    private Integer newElo;
    private LocalDateTime appliedAt;
}
