package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.dto.request.user.ApplyTournamentMatchCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.presentation.request.internal.ApplyMatchResultRequest;
import iuh.fit.auth.presentation.request.internal.ApplyTournamentMatchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/match-results")
@RequiredArgsConstructor
public class MatchResultInternalController {
    private final MatchResultService matchResultService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> apply(
            @Valid @RequestBody ApplyMatchResultRequest request
    ) throws ServiceException {
        var result = matchResultService.apply(new ApplyMatchResultCommand(
                request.sessionId(), request.userId(), request.sportType(),
                request.opponentElo(), request.outcome(), request.playerElo(),
                request.opponentName(), request.myScore(), request.opponentScore()
        ));
        return ResponseEntity.ok(Map.of(
                "userId", result.getUserId(),
                "oldElo", result.getOldElo(),
                "newElo", result.getNewElo(),
                "outcome", result.getOutcome()
        ));
    }

    /** Một trận giải đấu: trả ELO trước/sau của từng người; {@code changed=false} là gọi lặp, không cần báo lại. */
    @PostMapping("/tournament")
    public ResponseEntity<List<Map<String, Object>>> applyTournament(
            @Valid @RequestBody ApplyTournamentMatchRequest request
    ) throws ServiceException {
        var rated = matchResultService.applyTournamentMatch(new ApplyTournamentMatchCommand(
                request.matchId(), request.tournamentId(), request.sportType(), request.title(),
                side(request.first()), side(request.second())
        ));
        return ResponseEntity.ok(rated.stream().map(item -> Map.<String, Object>of(
                "userId", item.result().getUserId(),
                "oldElo", item.result().getOldElo(),
                "newElo", item.result().getNewElo(),
                "outcome", item.result().getOutcome(),
                "changed", item.changed()
        )).toList());
    }

    private static ApplyTournamentMatchCommand.Side side(ApplyTournamentMatchRequest.Side side) {
        return new ApplyTournamentMatchCommand.Side(side.name(), side.score(), side.playerIds());
    }
}
