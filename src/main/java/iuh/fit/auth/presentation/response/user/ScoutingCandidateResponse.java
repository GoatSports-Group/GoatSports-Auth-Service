package iuh.fit.auth.presentation.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoutingCandidateResponse {
    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private boolean profileVisible;
    private SkillLevel skillLevel;
    private Integer eloRating;
    private Set<String> preferredPositions;
    private String playStyle;
    private String city;
    private Double playRadiusKm;
    private Integer winCount;
    private Integer lossCount;
    private Integer drawCount;
    private Integer matchCount;
    private Double winRate;
    private List<AvailabilityResponse> availabilities;
    private Double distanceKm;
}
