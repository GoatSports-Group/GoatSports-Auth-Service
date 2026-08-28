package iuh.fit.auth.infrastructure.persistence.entity;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "player_sport_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_sport_profile_user_sport",
                columnNames = {"user_id", "sport_type"}
        ),
        indexes = @Index(name = "idx_player_sport_profile_user", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSportProfileEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "profile_id", columnDefinition = "UUID")
    private UUID profileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", nullable = false)
    private SportType sportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false)
    private SkillLevel skillLevel;

    @Column(name = "elo_rating")
    private Integer eloRating;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "player_preferred_positions",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    @Column(name = "position", nullable = false)
    @Builder.Default
    private Set<String> preferredPositions = new HashSet<>();

    @Column(name = "play_style")
    private String playStyle;
    private Double latitude;
    private Double longitude;
    @Column(name = "play_radius_km")
    private Double playRadiusKm;
    @Column(name = "win_count")
    private Integer winCount;
    @Column(name = "loss_count")
    private Integer lossCount;
    @Column(name = "draw_count")
    private Integer drawCount;
    @Column(name = "match_count")
    private Integer matchCount;
    @Column(name = "win_rate")
    private Double winRate;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<AvailabilityEntity> availabilities = new HashSet<>();
}
