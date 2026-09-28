package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.ApplyTournamentMatchCommand;
import iuh.fit.auth.application.dto.request.user.ApplyTournamentMatchCommand.Side;
import iuh.fit.auth.application.dto.response.user.ScoutedPlayerCommand;
import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tính ELO cho trận giải đấu, chạy trên kho dữ liệu trong bộ nhớ để thấy đúng trạng thái sau nhiều lần gọi. */
class TournamentMatchRatingTest {
    private final Profiles profiles = new Profiles();
    private final Results results = new Results();
    private final MatchResultServiceImpl service = new MatchResultServiceImpl(profiles, results, null);
    private final UUID tournamentId = UUID.randomUUID();
    private final UUID matchId = UUID.randomUUID();

    @Test
    void singlesWinnerGainsWhatTheLoserDropsAndHistoryRecordsTheMatch() throws Exception {
        UUID a = player(1000), b = player(1000);

        var rated = service.applyTournamentMatch(match(side("Minh", 21, a), side("Thắng", 19, b)));

        assertThat(rated).allMatch(item -> item.changed());
        assertThat(profiles.elo(a)).isEqualTo(1020);
        assertThat(profiles.elo(b)).isEqualTo(980);
        AppliedMatchResult history = results.of(a);
        assertThat(history.getSource()).isEqualTo(AppliedMatchResult.SOURCE_TOURNAMENT);
        assertThat(history.getReferenceId()).isEqualTo(tournamentId);
        assertThat(history.getTitle()).isEqualTo("Bán kết · Giải cầu lông đơn");
        assertThat(history.getOpponentName()).isEqualTo("Thắng");
        assertThat(history.getMyScore()).isEqualTo(21);
        assertThat(history.getOpponentScore()).isEqualTo(19);
        assertThat(history.getOutcome()).isEqualTo("WIN");
        assertThat(history.delta()).isEqualTo(20);
    }

    @Test
    void doublesRateEachPlayerAgainstTheOpponentAverage() throws Exception {
        UUID a1 = player(1000), a2 = player(1200), b1 = player(1100), b2 = player(1100);

        service.applyTournamentMatch(match(side("Đội A", 21, a1, a2), side("Đội B", 15, b1, b2)));

        // Cùng thắng đội trung bình 1100: người yếu hơn được cộng nhiều hơn.
        assertThat(profiles.elo(a1)).isEqualTo(1026);
        assertThat(profiles.elo(a2)).isEqualTo(1214);
        assertThat(profiles.elo(b1)).isEqualTo(1080);
        assertThat(results.of(b1).getOpponentElo()).isEqualTo(1100);
    }

    @Test
    void retryWithTheSameScoreChangesNothing() throws Exception {
        UUID a = player(1000), b = player(1000);
        var command = match(side("Minh", 21, a), side("Thắng", 19, b));
        service.applyTournamentMatch(command);

        var replay = service.applyTournamentMatch(command);

        assertThat(replay).noneMatch(item -> item.changed());
        assertThat(profiles.elo(a)).isEqualTo(1020);
        assertThat(profiles.get(a).getMatchCount()).isEqualTo(1);
    }

    @Test
    void correctingTheScoreRevertsTheFirstRatingBeforeApplyingTheNewOne() throws Exception {
        UUID a = player(1000), b = player(1000);
        service.applyTournamentMatch(match(side("Minh", 21, a), side("Thắng", 19, b)));

        var rated = service.applyTournamentMatch(match(side("Minh", 19, a), side("Thắng", 21, b)));

        assertThat(rated).allMatch(item -> item.changed());
        assertThat(profiles.elo(a)).isEqualTo(980);
        assertThat(profiles.elo(b)).isEqualTo(1020);
        PlayerSportProfile first = profiles.get(a);
        assertThat(first.getMatchCount()).isEqualTo(1);
        assertThat(first.getWinCount()).isZero();
        assertThat(first.getLossCount()).isEqualTo(1);
        assertThat(results.of(a).getOldElo()).isEqualTo(1000);
        assertThat(results.all()).hasSize(2);
    }

    @Test
    void playerWithoutAProfileStartsAtTheLowestLevel() throws Exception {
        UUID newcomer = UUID.randomUUID(), b = player(1000);

        service.applyTournamentMatch(match(side("Người mới", 21, newcomer), side("Thắng", 19, b)));

        assertThat(profiles.get(newcomer)).isNotNull();
        assertThat(results.of(newcomer).getOldElo()).isEqualTo(1000);
        assertThat(profiles.get(newcomer).getSkillLevel()).isEqualTo(SkillLevel.BEGINNER);
    }

    @Test
    void ensureProfileCreatesBeginnerOnceAndKeepsAnExistingOne() throws Exception {
        UUID userId = UUID.randomUUID();

        var created = service.ensureProfile(userId, SportType.BADMINTON);
        created.setEloRating(1300);
        var again = service.ensureProfile(userId, SportType.BADMINTON);

        assertThat(created.getSkillLevel()).isEqualTo(SkillLevel.BEGINNER);
        assertThat(again.getEloRating()).isEqualTo(1300);
        assertThat(profiles.count()).isEqualTo(1);
    }

    @Test
    void refusesAPlayerOnBothSides() {
        UUID a = player(1000);
        assertThatThrownBy(() -> service.applyTournamentMatch(match(side("A", 21, a), side("B", 19, a))))
                .hasMessageContaining("cả hai bên");
    }

    // ---------------------------------------------------------------- helpers

    private UUID player(int elo) {
        UUID userId = UUID.randomUUID();
        profiles.saveQuietly(PlayerSportProfile.builder().profileId(UUID.randomUUID()).userId(userId)
                .sportType(SportType.BADMINTON).skillLevel(SkillLevel.BEGINNER).eloRating(elo)
                .winCount(0).lossCount(0).drawCount(0).matchCount(0).winRate(0.0).build());
        return userId;
    }

    private static Side side(String name, int score, UUID... players) {
        return new Side(name, score, List.of(players));
    }

    private ApplyTournamentMatchCommand match(Side first, Side second) {
        return new ApplyTournamentMatchCommand(matchId, tournamentId, SportType.BADMINTON,
                "Bán kết · Giải cầu lông đơn", first, second);
    }

    private static final class Profiles implements PlayerSportProfileRepository {
        private final Map<UUID, PlayerSportProfile> byUser = new HashMap<>();

        void saveQuietly(PlayerSportProfile profile) { byUser.put(profile.getUserId(), profile); }
        PlayerSportProfile get(UUID userId) { return byUser.get(userId); }
        int elo(UUID userId) { return byUser.get(userId).getEloRating(); }
        int count() { return byUser.size(); }

        @Override public Optional<PlayerSportProfile> findByUserIdAndSportType(UUID userId, SportType sportType) {
            return Optional.ofNullable(byUser.get(userId)).filter(profile -> profile.getSportType() == sportType);
        }
        @Override public PlayerSportProfile save(PlayerSportProfile profile) { byUser.put(profile.getUserId(), profile); return profile; }
        @Override public boolean existsByUserIdAndSportType(UUID userId, SportType sportType) {
            return findByUserIdAndSportType(userId, sportType).isPresent();
        }
        @Override public List<PlayerSportProfile> findAllByUserId(UUID userId) { throw new UnsupportedOperationException(); }
        @Override public List<ScoutedPlayerCommand> findScoutingCandidates(ScoutingSearchCommand search, double a, double b,
                                                                          double c, double d, int limit) { throw new UnsupportedOperationException(); }
        @Override public List<ScoutedPlayerCommand> findScoutingProfiles(SportType sportType, Collection<UUID> userIds) {
            throw new UnsupportedOperationException();
        }
        @Override public Optional<PlayerSportProfile> findByIdAndUserId(UUID profileId, UUID userId) { throw new UnsupportedOperationException(); }
        @Override public void deleteByIdAndUserId(UUID profileId, UUID userId) { throw new UnsupportedOperationException(); }
    }

    private static final class Results implements AppliedMatchResultRepository {
        private final Map<UUID, AppliedMatchResult> byId = new HashMap<>();

        AppliedMatchResult of(UUID userId) {
            return byId.values().stream().filter(item -> item.getUserId().equals(userId)).findFirst().orElseThrow();
        }
        List<AppliedMatchResult> all() { return new ArrayList<>(byId.values()); }

        @Override public Optional<AppliedMatchResult> findBySessionIdAndUserId(UUID sessionId, UUID userId) {
            return byId.values().stream().filter(item -> item.getSessionId().equals(sessionId) && item.getUserId().equals(userId)).findFirst();
        }
        @Override public List<AppliedMatchResult> findBySessionId(UUID sessionId) {
            return byId.values().stream().filter(item -> item.getSessionId().equals(sessionId)).toList();
        }
        @Override public List<AppliedMatchResult> findRecent(UUID userId, SportType sportType, int limit) { return List.of(); }
        @Override public AppliedMatchResult save(AppliedMatchResult result) {
            AppliedMatchResult saved = result.getResultId() == null ? result.toBuilder().resultId(UUID.randomUUID()).build() : result;
            byId.put(saved.getResultId(), saved);
            return saved;
        }
        @Override public void delete(UUID resultId) { byId.remove(resultId); }
    }
}
