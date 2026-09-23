package iuh.fit.auth.presentation.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSportProfileResponse {
    private UUID profileId;
    private SportType sportType;
    private SkillLevel skillLevel;
    private Integer eloRating;
    private Set<String> preferredPositions;
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
    private List<AvailabilityResponse> availabilities;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
