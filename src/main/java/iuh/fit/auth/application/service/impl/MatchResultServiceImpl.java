package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.service.EloRatingPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public class MatchResultServiceImpl implements MatchResultService {
    private final PlayerSportProfileRepository profileRepository;
    private final AppliedMatchResultRepository resultRepository;

    public MatchResultServiceImpl(
            PlayerSportProfileRepository profileRepository,
            AppliedMatchResultRepository resultRepository
    ) {
        this.profileRepository = profileRepository;
        this.resultRepository = resultRepository;
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
                .orElseGet(() -> {
                    int seedElo = command.playerElo() != null
                            ? command.playerElo()
                            : EloRatingPolicy.initialRating(command.sportType(), SkillLevel.INTERMEDIATE);
                    return PlayerSportProfile.builder()
                            .userId(command.userId())
                            .sportType(command.sportType())
                            .skillLevel(EloRatingPolicy.skillLevelFor(seedElo))
                            .eloRating(seedElo)
                            .playStyle("BALANCED")
                            .playRadiusKm(10.0)
                            .winCount(0).lossCount(0).drawCount(0).matchCount(0).winRate(0.0)
                            .build();
                });
        int oldElo = profile.getEloRating() == null
                ? EloRatingPolicy.initialRating(profile.getSportType(), profile.getSkillLevel())
                : profile.getEloRating();
        double actual = switch (command.outcome().toUpperCase()) {
            case "WIN" -> 1.0;
            case "DRAW" -> 0.5;
            case "LOSS" -> 0.0;
            default -> throw new ServiceException("Kết quả trận đấu không hợp lệ.");
        };
        int newElo = EloRatingPolicy.calculateNewRating(
                profile.getSportType(),
                oldElo,
                command.opponentElo(),
                value(profile.getMatchCount()),
                actual
        );
        profile.setEloRating(newElo);
        // Trình độ đi theo Elo thay vì giữ nguyên giá trị người chơi tự khai lúc đầu.
        profile.setSkillLevel(EloRatingPolicy.skillLevelFor(newElo));
        profile.setMatchCount(value(profile.getMatchCount()) + 1);
        if (actual == 1.0) profile.setWinCount(value(profile.getWinCount()) + 1);
        else if (actual == 0.0) profile.setLossCount(value(profile.getLossCount()) + 1);
        else profile.setDrawCount(value(profile.getDrawCount()) + 1);
        profile.setWinRate((double) value(profile.getWinCount()) / profile.getMatchCount());
        profileRepository.save(profile);

        return resultRepository.save(AppliedMatchResult.builder()
                .sessionId(command.sessionId()).userId(command.userId())
                .sportType(command.sportType()).outcome(command.outcome().toUpperCase())
                .oldElo(oldElo).newElo(newElo).appliedAt(LocalDateTime.now()).build());
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
