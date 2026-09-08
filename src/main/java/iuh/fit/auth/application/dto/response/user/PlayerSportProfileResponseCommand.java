package iuh.fit.auth.application.dto.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PlayerSportProfileResponseCommand(
        UUID profileId,
        SportType sportType,
        SkillLevel skillLevel,
        Integer eloRating,
        Set<String> preferredPositions,
        String playStyle,
        Double latitude,
        Double longitude,
        Double playRadiusKm,
        Integer winCount,
        Integer lossCount,
        Integer drawCount,
        Integer matchCount,
        Double winRate,
        List<AvailabilityResponseCommand> availabilities,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
