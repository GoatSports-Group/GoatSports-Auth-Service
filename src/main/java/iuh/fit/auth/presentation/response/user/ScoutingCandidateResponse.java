package iuh.fit.auth.presentation.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private SkillLevel skillLevel;
    private Integer eloRating;
    private Integer matchCount;
    private Double winRate;
    private Double playRadiusKm;
    private Double distanceKm;
}
