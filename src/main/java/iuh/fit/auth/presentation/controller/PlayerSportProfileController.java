package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.dto.request.user.SaveAvailabilityCommand;
import iuh.fit.auth.application.dto.request.user.SavePlayerSportProfileCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.MatchResultService;
import iuh.fit.auth.application.service.PlayerSportProfileService;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.presentation.converter.PlayerSportProfileConverter;
import iuh.fit.auth.presentation.request.user.AvailabilityRequest;
import iuh.fit.auth.presentation.request.user.SavePlayerSportProfileRequest;
import iuh.fit.auth.presentation.response.user.MatchHistoryResponse;
import iuh.fit.auth.presentation.response.user.PlayerSportProfileResponse;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/sport-profiles")
@RequiredArgsConstructor
public class PlayerSportProfileController {
    private final PlayerSportProfileService profileService;
    private final PlayerSportProfileConverter profileConverter;
    private final MatchResultService matchResultService;

    @GetMapping
    public ResponseEntity<List<PlayerSportProfileResponse>> getMyProfiles() throws ServiceException {
        return ResponseEntity.ok(profileConverter.toResponse(profileService.getMyProfiles()));
    }

    /** Lịch sử thi đấu (ghép trận + giải đấu) của hồ sơ một môn, mới nhất trước. */
    @GetMapping("/{sportType}/history")
    public ResponseEntity<List<MatchHistoryResponse>> getMyHistory(
            @PathVariable SportType sportType
    ) throws ServiceException {
        return ResponseEntity.ok(matchResultService.getMyHistory(sportType).stream()
                .map(MatchHistoryResponse::from).toList());
    }

    @PostMapping
    @AuditLog(action = "CREATE_SPORT_PROFILE", description = "Tạo hồ sơ thể thao")
    public ResponseEntity<PlayerSportProfileResponse> createMyProfile(
            @Valid @RequestBody SavePlayerSportProfileRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileConverter.toResponse(profileService.createMyProfile(toCommand(request))));
    }

    @PutMapping("/{profileId}")
    @AuditLog(action = "UPDATE_SPORT_PROFILE", description = "Cập nhật hồ sơ thể thao #{#profileId}")
    public ResponseEntity<PlayerSportProfileResponse> updateMyProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody SavePlayerSportProfileRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(profileConverter.toResponse(
                profileService.updateMyProfile(profileId, toCommand(request))
        ));
    }

    @DeleteMapping("/{profileId}")
    @AuditLog(action = "DELETE_SPORT_PROFILE", description = "Xóa hồ sơ thể thao #{#profileId}")
    public ResponseEntity<Void> deleteMyProfile(@PathVariable UUID profileId) throws ServiceException {
        profileService.deleteMyProfile(profileId);
        return ResponseEntity.noContent().build();
    }

    private SavePlayerSportProfileCommand toCommand(SavePlayerSportProfileRequest request) {
        List<SaveAvailabilityCommand> availabilities = request.getAvailabilities() == null
                ? List.of()
                : request.getAvailabilities().stream().map(this::toCommand).toList();
        return new SavePlayerSportProfileCommand(
                request.getSportType(),
                request.getSkillLevel(),
                request.getPreferredPositions(),
                request.getPlayStyle(),
                request.getCity(),
                request.getLatitude(),
                request.getLongitude(),
                request.getPlayRadiusKm(),
                availabilities,
                request.getDiscoverable()
        );
    }

    private SaveAvailabilityCommand toCommand(AvailabilityRequest request) {
        return new SaveAvailabilityCommand(
                request.getAvailabilityId(),
                request.getDayOfWeek(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTimezone(),
                request.getActive()
        );
    }
}
