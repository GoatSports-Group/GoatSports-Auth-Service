package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;

import java.util.List;
import java.util.Set;

public record SavePlayerSportProfileCommand(
        SportType sportType,
        SkillLevel skillLevel,
        Set<String> preferredPositions,
        String playStyle,
        Double latitude,
        Double longitude,
        Double playRadiusKm,
        List<SaveAvailabilityCommand> availabilities
) {
}
