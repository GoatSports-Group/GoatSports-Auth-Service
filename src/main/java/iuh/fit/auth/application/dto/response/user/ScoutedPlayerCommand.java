package iuh.fit.auth.application.dto.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ScoutedPlayerCommand(
        UUID userId,
        String username,
        String fullName,
        String avatarUrl,
        boolean profileVisible,
        SkillLevel skillLevel,
        Integer eloRating,
        Set<String> preferredPositions,
        String playStyle,
        String city,
        Double playRadiusKm,
        Integer winCount,
        Integer lossCount,
        Integer drawCount,
        Integer matchCount,
        Double winRate,
        List<AvailabilityResponseCommand> availabilities,
        Double latitude,
        Double longitude,
        Double distanceKm
) {
    public static ScoutedPlayerCommand identityOnly(UUID userId, String username, String fullName, String avatarUrl) {
        return new ScoutedPlayerCommand(userId, username, fullName, avatarUrl, false, null, null, Set.of(), null,
                null, null, null, null, null, null, null, List.of(), null, null, null);
    }

    public ScoutedPlayerCommand withDistanceKm(Double distance) {
        return new ScoutedPlayerCommand(userId, username, fullName, avatarUrl, profileVisible, skillLevel, eloRating,
                preferredPositions, playStyle, city, playRadiusKm, winCount, lossCount, drawCount, matchCount,
                winRate, availabilities, latitude, longitude, distance);
    }
}
