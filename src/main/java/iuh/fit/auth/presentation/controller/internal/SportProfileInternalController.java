package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.domain.enums.SportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/sport-profiles")
@RequiredArgsConstructor
public class SportProfileInternalController {
    private final MatchResultService matchResultService;

    /** Đăng ký giải một môn chưa có hồ sơ: tạo hồ sơ ở trình độ thấp nhất (gọi lặp thì trả hồ sơ đang có). */
    @PostMapping("/ensure")
    public ResponseEntity<Map<String, Object>> ensure(@Valid @RequestBody EnsureRequest request) throws ServiceException {
        var profile = matchResultService.ensureProfile(request.userId(), request.sportType());
        return ResponseEntity.ok(Map.of(
                "userId", profile.getUserId(),
                "sportType", profile.getSportType(),
                "skillLevel", profile.getSkillLevel(),
                "eloRating", profile.getEloRating()
        ));
    }

    public record EnsureRequest(@NotNull UUID userId, @NotNull SportType sportType) { }
}
