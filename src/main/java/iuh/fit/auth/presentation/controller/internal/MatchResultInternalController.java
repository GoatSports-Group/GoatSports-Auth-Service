package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.dto.request.user.ApplyMatchResultCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.presentation.request.internal.ApplyMatchResultRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                request.opponentElo(), request.outcome()
        ));
        return ResponseEntity.ok(Map.of(
                "userId", result.getUserId(),
                "oldElo", result.getOldElo(),
                "newElo", result.getNewElo(),
                "outcome", result.getOutcome()
        ));
    }
}
