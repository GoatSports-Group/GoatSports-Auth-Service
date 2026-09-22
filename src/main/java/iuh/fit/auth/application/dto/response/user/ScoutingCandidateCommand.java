package iuh.fit.auth.application.dto.response.user;

import iuh.fit.auth.domain.enums.SkillLevel;

import java.util.UUID;

public record ScoutingCandidateCommand(
        UUID userId,
        String username,
        String fullName,
        String avatarUrl,
        SkillLevel skillLevel,
        Integer eloRating,
        Integer matchCount,
        Double winRate,
        Double playRadiusKm,
        Double latitude,
        Double longitude
) { }
