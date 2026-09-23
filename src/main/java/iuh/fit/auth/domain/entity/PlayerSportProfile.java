package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSportProfile extends BaseEntity {
    private UUID profileId;
    private UUID userId;
    private SportType sportType;
    private SkillLevel skillLevel;
    private Integer eloRating;
    @Builder.Default
    private Set<String> preferredPositions = new HashSet<>();
    private String playStyle;
    private String city;
    private Double latitude;
    private Double longitude;
    private Double playRadiusKm;
    private Integer winCount;
    private Integer lossCount;
    private Integer drawCount;
    private Integer matchCount;
    private Double winRate;
    @Builder.Default
    private Set<Availability> availabilities = new HashSet<>();
}
