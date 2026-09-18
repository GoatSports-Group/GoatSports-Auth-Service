package iuh.fit.auth.domain.service;

import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Owns the per-sport Elo policy used by player sport profiles
 **/
public final class EloRatingPolicy {
    private static final double ELO_SCALE = 400.0;
    private static final int PROVISIONAL_MATCH_LIMIT = 10;
    private static final int EXPERIENCED_MATCH_LIMIT = 30;

    private static final Map<SkillLevel, Integer> INITIAL_RATINGS = initialRatings();
    private static final Map<SportType, SportEloConfig> SPORT_CONFIGS = sportConfigs();

    private EloRatingPolicy() {
    }

    public static int initialRating(SportType sportType, SkillLevel skillLevel) {
        requireConfiguredSport(sportType);
        Integer rating = INITIAL_RATINGS.get(skillLevel);
        if (rating == null) {
            throw new IllegalArgumentException("Skill level is required to initialize Elo");
        }
        return rating;
    }

    /**
     * Suy trình độ từ Elo hiện tại. Ngưỡng là điểm giữa của các mốc Elo khởi tạo
     * ({@link #initialRatings()}), nên một người tự khai đúng trình độ sẽ được xếp
     * lại đúng bậc đó, còn Elo thay đổi sau các trận sẽ tự kéo trình độ đi theo.
     */
    public static SkillLevel skillLevelFor(int eloRating) {
        if (eloRating < 1100) return SkillLevel.BEGINNER;
        if (eloRating < 1350) return SkillLevel.INTERMEDIATE;
        if (eloRating < 1650) return SkillLevel.ADVANCED;
        return SkillLevel.PRO;
    }

    public static int calculateNewRating(
            SportType sportType,
            int currentRating,
            int opponentRating,
            int matchCount,
            double actualScore
    ) {
        if (actualScore < 0.0 || actualScore > 1.0) {
            throw new IllegalArgumentException("Actual Elo score must be between 0 and 1");
        }
        double expectedScore = 1.0 / (
                1.0 + Math.pow(10.0, (opponentRating - currentRating) / ELO_SCALE)
        );
        int updatedRating = (int) Math.round(
                currentRating + kFactor(sportType, matchCount) * (actualScore - expectedScore)
        );
        return Math.max(0, updatedRating);
    }

    public static int kFactor(SportType sportType, int matchCount) {
        SportEloConfig config = requireConfiguredSport(sportType);
        int safeMatchCount = Math.max(0, matchCount);
        if (safeMatchCount < PROVISIONAL_MATCH_LIMIT) {
            return 40;
        }
        return safeMatchCount < EXPERIENCED_MATCH_LIMIT
                ? config.establishedKFactor()
                : config.experiencedKFactor();
    }

    private static SportEloConfig requireConfiguredSport(SportType sportType) {
        SportEloConfig config = SPORT_CONFIGS.get(sportType);
        if (config == null) {
            throw new IllegalArgumentException("Sport type is required to calculate Elo");
        }
        return config;
    }

    private static Map<SkillLevel, Integer> initialRatings() {
        Map<SkillLevel, Integer> ratings = new EnumMap<>(SkillLevel.class);
        ratings.put(SkillLevel.BEGINNER, 1000);
        ratings.put(SkillLevel.INTERMEDIATE, 1200);
        ratings.put(SkillLevel.ADVANCED, 1500);
        ratings.put(SkillLevel.PRO, 1800);
        return Map.copyOf(ratings);
    }

    private static Map<SportType, SportEloConfig> sportConfigs() {
        Map<SportType, SportEloConfig> configs = new EnumMap<>(SportType.class);

        // Head-to-head/racket sports: a result is a relatively direct signal of individual skill.
        SportEloConfig individual = new SportEloConfig(32, 24);
        configs.put(SportType.BADMINTON, individual);
        configs.put(SportType.TENNIS, individual);
        configs.put(SportType.PICKLEBALL, individual);

        // Team sports: reduce rating volatility because one result also depends on teammates.
        SportEloConfig team = new SportEloConfig(24, 16);
        configs.put(SportType.FOOTBALL, team);
        configs.put(SportType.BASKETBALL, team);
        configs.put(SportType.VOLLEYBALL, team);
        return Map.copyOf(configs);
    }

    private record SportEloConfig(int establishedKFactor, int experiencedKFactor) {
    }
}
