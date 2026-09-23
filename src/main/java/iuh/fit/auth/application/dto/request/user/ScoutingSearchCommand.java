package iuh.fit.auth.application.dto.request.user;

import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;

import java.util.Set;

public record ScoutingSearchCommand(
        SportType sportType,
        double latitude,
        double longitude,
        double radiusKm,
        int limit,
        Set<SkillLevel> skillLevels,
        String position,
        DayOfWeek availableOn
) {
    public boolean anySkill() { return skillLevels == null || skillLevels.isEmpty(); }

    public boolean anyPosition() { return position == null || position.isBlank(); }
}
