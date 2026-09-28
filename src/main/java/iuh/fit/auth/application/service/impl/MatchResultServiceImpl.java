package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.dto.request.user.ApplyTournamentMatchCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.domain.service.EloRatingPolicy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MatchResultServiceImpl implements MatchResultService {
    /** Lịch sử hiển thị trong hồ sơ: đủ cho một mùa chơi. */
    static final int HISTORY_LIMIT = 50;

    private final PlayerSportProfileRepository profileRepository;
    private final AppliedMatchResultRepository resultRepository;
    private final CurrentUserProvider currentUserProvider;

    public MatchResultServiceImpl(
            PlayerSportProfileRepository profileRepository,
            AppliedMatchResultRepository resultRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.profileRepository = profileRepository;
        this.resultRepository = resultRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public AppliedMatchResult apply(ApplyMatchResultCommand command) throws ServiceException {
        var existing = resultRepository.findBySessionIdAndUserId(command.sessionId(), command.userId());
        if (existing.isPresent()) return existing.get();
        // Người chơi có thể ghép kèo một môn chưa có hồ sơ; khi trận kết thúc thì hồ sơ
        // được tạo tự động, và phải khởi tạo bằng đúng Elo mà trận đó đã thi đấu —
        // nếu dùng mốc mặc định thì Elo trên thẻ trận và Elo trong hồ sơ sẽ lệch nhau.
        var profile = profileRepository.findByUserIdAndSportType(command.userId(), command.sportType())
                .orElseGet(() -> newProfile(command.userId(), command.sportType(), command.playerElo()));
        int oldElo = currentElo(profile);
        double actual = actualScore(command.outcome());
        int newElo = EloRatingPolicy.calculateNewRating(
                profile.getSportType(), oldElo, command.opponentElo(), value(profile.getMatchCount()), actual);
        record(profile, newElo, actual, 1);
        profileRepository.save(profile);
        return resultRepository.save(AppliedMatchResult.builder()
                .sessionId(command.sessionId()).userId(command.userId())
                .sportType(command.sportType()).outcome(command.outcome().toUpperCase())
                .oldElo(oldElo).newElo(newElo).appliedAt(LocalDateTime.now())
                .source(AppliedMatchResult.SOURCE_MATCHMAKING).title("Ghép trận")
                .opponentName(command.opponentName()).myScore(command.myScore())
                .opponentScore(command.opponentScore()).opponentElo(command.opponentElo())
                .build());
    }

    /**
     * Mỗi người được tính theo ELO của chính mình so với ELO trung bình của bên đối thủ, nên trong một đội
     * người yếu thắng đội mạnh được cộng nhiều hơn người mạnh. ELO "trước trận" của một người là giá trị lúc
     * trận được tính lần đầu: sửa tỉ số thì hoàn lại phần cộng/trừ cũ và tính lại từ đó, giữ nguyên ảnh hưởng
     * của các trận khác diễn ra sau.
     */
    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public List<RatedPlayer> applyTournamentMatch(ApplyTournamentMatchCommand command) throws ServiceException {
        validate(command);
        Map<UUID, AppliedMatchResult> previous = resultRepository.findBySessionId(command.matchId()).stream()
                .collect(Collectors.toMap(AppliedMatchResult::getUserId, Function.identity(), (a, b) -> a));
        int comparison = Integer.compare(command.first().score(), command.second().score());
        if (unchanged(command, previous, comparison)) {
            List<RatedPlayer> replay = new ArrayList<>();
            for (UUID userId : players(command)) replay.add(new RatedPlayer(previous.get(userId), false));
            return replay;
        }

        // Người từng được tính ở trận này nhưng không còn trong đội: hoàn lại phần của họ.
        for (AppliedMatchResult stale : previous.values()) {
            if (players(command).contains(stale.getUserId())) continue;
            Optional<PlayerSportProfile> profile = profileRepository.findByUserIdAndSportType(stale.getUserId(), stale.getSportType());
            if (profile.isPresent()) {
                record(profile.get(), currentElo(profile.get()) - stale.delta(), actualScore(stale.getOutcome()), -1);
                profileRepository.save(profile.get());
            }
            resultRepository.delete(stale.getResultId());
        }

        Map<UUID, PlayerSportProfile> profiles = new LinkedHashMap<>();
        Map<UUID, Integer> baseline = new HashMap<>();
        for (UUID userId : players(command)) {
            PlayerSportProfile profile = profileRepository.findByUserIdAndSportType(userId, command.sportType())
                    .orElseGet(() -> newProfile(userId, command.sportType(), null));
            AppliedMatchResult before = previous.get(userId);
            if (before != null) record(profile, currentElo(profile) - before.delta(), actualScore(before.getOutcome()), -1);
            profiles.put(userId, profile);
            baseline.put(userId, before != null ? before.getOldElo() : currentElo(profile));
        }
        int firstAverage = average(command.first().playerIds(), baseline);
        int secondAverage = average(command.second().playerIds(), baseline);

        List<RatedPlayer> rated = new ArrayList<>();
        rated.addAll(rateSide(command, command.first(), command.second(), secondAverage, comparison, profiles, baseline, previous));
        rated.addAll(rateSide(command, command.second(), command.first(), firstAverage, -comparison, profiles, baseline, previous));
        return rated;
    }

    @Override
    @Transactional
    public PlayerSportProfile ensureProfile(UUID userId, SportType sportType) throws ServiceException {
        Optional<PlayerSportProfile> existing = profileRepository.findByUserIdAndSportType(userId, sportType);
        if (existing.isPresent()) return existing.get();
        return profileRepository.save(newProfile(userId, sportType, null));
    }

    @Override
    public List<AppliedMatchResult> getMyHistory(SportType sportType) throws ServiceException {
        UUID userId = currentUserProvider.getCurrentUser().getUserId();
        if (userId == null) throw new ServiceException("Không tìm thấy người dùng hiện tại");
        return resultRepository.findRecent(userId, sportType, HISTORY_LIMIT);
    }

    private List<RatedPlayer> rateSide(ApplyTournamentMatchCommand command, ApplyTournamentMatchCommand.Side side,
                                       ApplyTournamentMatchCommand.Side opponent, int opponentElo, int comparison,
                                       Map<UUID, PlayerSportProfile> profiles, Map<UUID, Integer> baseline,
                                       Map<UUID, AppliedMatchResult> previous) throws ServiceException {
        String outcome = comparison > 0 ? "WIN" : comparison < 0 ? "LOSS" : "DRAW";
        double actual = actualScore(outcome);
        List<RatedPlayer> rated = new ArrayList<>();
        for (UUID userId : side.playerIds()) {
            PlayerSportProfile profile = profiles.get(userId);
            int oldElo = baseline.get(userId);
            int newElo = EloRatingPolicy.calculateNewRating(
                    command.sportType(), oldElo, opponentElo, value(profile.getMatchCount()), actual);
            // Hồ sơ có thể đã đổi vì các trận khác sau trận này: chỉ cộng phần chênh của trận này.
            record(profile, currentElo(profile) + (newElo - oldElo), actual, 1);
            profileRepository.save(profile);
            AppliedMatchResult before = previous.get(userId);
            AppliedMatchResult saved = resultRepository.save(AppliedMatchResult.builder()
                    .resultId(before == null ? null : before.getResultId())
                    .sessionId(command.matchId()).userId(userId).sportType(command.sportType())
                    .outcome(outcome).oldElo(oldElo).newElo(newElo).appliedAt(LocalDateTime.now())
                    .source(AppliedMatchResult.SOURCE_TOURNAMENT).referenceId(command.tournamentId())
                    .title(command.title()).opponentName(opponent.name())
                    .myScore(side.score()).opponentScore(opponent.score()).opponentElo(opponentElo)
                    .build());
            rated.add(new RatedPlayer(saved, true));
        }
        return rated;
    }

    /** Gọi lặp (retry) với đúng tỉ số và đúng người chơi: không tính lại, không báo lại. */
    private boolean unchanged(ApplyTournamentMatchCommand command, Map<UUID, AppliedMatchResult> previous, int comparison) {
        List<UUID> players = players(command);
        if (players.size() != previous.size()) return false;
        for (UUID userId : players) {
            AppliedMatchResult before = previous.get(userId);
            if (before == null) return false;
            boolean first = command.first().playerIds().contains(userId);
            int mine = first ? command.first().score() : command.second().score();
            int theirs = first ? command.second().score() : command.first().score();
            String outcome = (first ? comparison : -comparison) > 0 ? "WIN" : (first ? comparison : -comparison) < 0 ? "LOSS" : "DRAW";
            if (!Objects.equals(before.getMyScore(), mine) || !Objects.equals(before.getOpponentScore(), theirs)
                    || !outcome.equals(before.getOutcome())) {
                return false;
            }
        }
        return true;
    }

    private static void validate(ApplyTournamentMatchCommand command) throws ServiceException {
        if (command.matchId() == null || command.sportType() == null || command.first() == null || command.second() == null) {
            throw new ServiceException("Thiếu thông tin trận đấu.");
        }
        if (command.first().playerIds() == null || command.first().playerIds().isEmpty()
                || command.second().playerIds() == null || command.second().playerIds().isEmpty()) {
            throw new ServiceException("Mỗi bên phải có ít nhất một người chơi.");
        }
        var seen = new HashSet<UUID>();
        for (UUID userId : players(command)) {
            if (!seen.add(userId)) throw new ServiceException("Một người chơi không thể ở cả hai bên của trận.");
        }
    }

    private static List<UUID> players(ApplyTournamentMatchCommand command) {
        List<UUID> all = new ArrayList<>(command.first().playerIds());
        all.addAll(command.second().playerIds());
        return all;
    }

    private static int average(List<UUID> userIds, Map<UUID, Integer> baseline) {
        return (int) Math.round(userIds.stream().mapToInt(baseline::get).average().orElse(0));
    }

    /**
     * Ghi một trận vào hồ sơ ({@code sign = 1}) hoặc gỡ một trận đã ghi ({@code sign = -1}); ELO đặt thành {@code elo},
     * trình độ đi theo ELO thay vì giữ nguyên giá trị người chơi tự khai lúc đầu.
     */
    private void record(PlayerSportProfile profile, int elo, double actual, int sign) {
        profile.setEloRating(Math.max(0, elo));
        profile.setSkillLevel(EloRatingPolicy.skillLevelFor(profile.getEloRating()));
        profile.setMatchCount(Math.max(0, value(profile.getMatchCount()) + sign));
        if (actual == 1.0) profile.setWinCount(Math.max(0, value(profile.getWinCount()) + sign));
        else if (actual == 0.0) profile.setLossCount(Math.max(0, value(profile.getLossCount()) + sign));
        else profile.setDrawCount(Math.max(0, value(profile.getDrawCount()) + sign));
        profile.setWinRate(profile.getMatchCount() == 0 ? 0.0 : (double) value(profile.getWinCount()) / profile.getMatchCount());
    }

    /** Hồ sơ tự tạo: thiếu ELO thì bắt đầu từ trình độ thấp nhất. */
    private PlayerSportProfile newProfile(UUID userId, SportType sportType, Integer seedElo) {
        int elo = seedElo != null ? seedElo : EloRatingPolicy.initialRating(sportType, SkillLevel.BEGINNER);
        return PlayerSportProfile.builder()
                .userId(userId)
                .sportType(sportType)
                .skillLevel(EloRatingPolicy.skillLevelFor(elo))
                .eloRating(elo)
                .playStyle("BALANCED")
                .playRadiusKm(10.0)
                .winCount(0).lossCount(0).drawCount(0).matchCount(0).winRate(0.0)
                .build();
    }

    private static int currentElo(PlayerSportProfile profile) {
        return profile.getEloRating() == null
                ? EloRatingPolicy.initialRating(profile.getSportType(), profile.getSkillLevel() == null
                        ? SkillLevel.BEGINNER : profile.getSkillLevel())
                : profile.getEloRating();
    }

    private static double actualScore(String outcome) throws ServiceException {
        return switch (outcome == null ? "" : outcome.toUpperCase()) {
            case "WIN" -> 1.0;
            case "DRAW" -> 0.5;
            case "LOSS" -> 0.0;
            default -> throw new ServiceException("Kết quả trận đấu không hợp lệ.");
        };
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
