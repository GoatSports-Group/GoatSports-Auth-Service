package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.dto.request.user.ApplyTournamentMatchCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.AppliedMatchResult;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.enums.SportType;

import java.util.List;
import java.util.UUID;

/** ELO và lịch sử thi đấu của hồ sơ thể thao: mọi trận (ghép trận, giải đấu) đều đi qua đây. */
public interface MatchResultService {
    AppliedMatchResult apply(ApplyMatchResultCommand command) throws ServiceException;

    /** Tính ELO cho mọi người của một trận giải; {@code changed=false} nghĩa là gọi lặp, không có gì thay đổi. */
    List<RatedPlayer> applyTournamentMatch(ApplyTournamentMatchCommand command) throws ServiceException;

    /** Hồ sơ môn này của người chơi; chưa có thì tạo ở trình độ thấp nhất. */
    PlayerSportProfile ensureProfile(UUID userId, SportType sportType) throws ServiceException;

    /** Lịch sử thi đấu gần nhất của người đang đăng nhập ở một môn. */
    List<AppliedMatchResult> getMyHistory(SportType sportType) throws ServiceException;

    record RatedPlayer(AppliedMatchResult result, boolean changed) { }
}
