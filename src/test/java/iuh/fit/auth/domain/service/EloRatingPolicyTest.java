package iuh.fit.auth.domain.service;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EloRatingPolicyTest {
    @Test
    void mapsDeclaredSkillToInitialRatingForEverySport() {
        for (SportType sportType : SportType.values()) {
            assertEquals(1000, EloRatingPolicy.initialRating(sportType, SkillLevel.BEGINNER));
            assertEquals(1200, EloRatingPolicy.initialRating(sportType, SkillLevel.INTERMEDIATE));
            assertEquals(1500, EloRatingPolicy.initialRating(sportType, SkillLevel.ADVANCED));
            assertEquals(1800, EloRatingPolicy.initialRating(sportType, SkillLevel.PRO));
        }
    }

    @Test
    void provisionalPlayersMoveFasterThanEstablishedPlayers() {
        assertEquals(1220, EloRatingPolicy.calculateNewRating(
                SportType.BADMINTON, 1200, 1200, 0, 1.0
        ));
        assertEquals(1216, EloRatingPolicy.calculateNewRating(
                SportType.BADMINTON, 1200, 1200, 10, 1.0
        ));
        assertEquals(1212, EloRatingPolicy.calculateNewRating(
                SportType.FOOTBALL, 1200, 1200, 10, 1.0
        ));
    }
}
