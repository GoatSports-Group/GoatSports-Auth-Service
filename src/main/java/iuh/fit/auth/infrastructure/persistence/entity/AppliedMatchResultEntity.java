package iuh.fit.auth.infrastructure.persistence.entity;

import iuh.fit.auth.domain.enums.SportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applied_match_results", uniqueConstraints = @UniqueConstraint(
        name = "uk_match_result_session_user", columnNames = {"session_id", "user_id"}
))
@Getter
@Setter
@NoArgsConstructor
public class AppliedMatchResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "result_id", columnDefinition = "UUID")
    private UUID resultId;
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false)
    private SportType sportType;
    @Column(nullable = false)
    private String outcome;
    @Column(name = "old_elo", nullable = false)
    private Integer oldElo;
    @Column(name = "new_elo", nullable = false)
    private Integer newElo;
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
    @Column(nullable = false, length = 20)
    private String source;
    @Column(name = "reference_id")
    private UUID referenceId;
    @Column(length = 255)
    private String title;
    @Column(name = "opponent_name", length = 255)
    private String opponentName;
    @Column(name = "my_score")
    private Integer myScore;
    @Column(name = "opponent_score")
    private Integer opponentScore;
    @Column(name = "opponent_elo")
    private Integer opponentElo;
}
