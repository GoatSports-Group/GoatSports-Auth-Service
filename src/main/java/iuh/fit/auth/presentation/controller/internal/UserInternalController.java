package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.service.InternalService;
import iuh.fit.auth.application.service.PlayerSportProfileService;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.PlayerSportProfileConverter;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.response.report.UserDetailReportResponse;
import iuh.fit.auth.presentation.response.report.UserReportResponse;
import iuh.fit.auth.domain.enums.DayOfWeek;
import iuh.fit.auth.domain.enums.SkillLevel;
import iuh.fit.auth.domain.enums.SportType;
import iuh.fit.auth.application.dto.request.user.ScoutingSearchCommand;
import iuh.fit.auth.presentation.request.user.ScoutingLookupRequest;
import jakarta.validation.Valid;
import iuh.fit.auth.presentation.response.user.PlayerSportProfileResponse;
import iuh.fit.auth.presentation.response.user.ScoutingCandidateResponse;
import iuh.fit.auth.presentation.response.user.UserPermissionResponse;
import iuh.fit.shared.annotation.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {

    private final InternalService internalService;
    private final UserConverter userConverter;
    private final PlayerSportProfileService playerSportProfileService;
    private final PlayerSportProfileConverter playerSportProfileConverter;

    @GetMapping
    public ResponseEntity<List<UserReportResponse>> getAllUsers() {
        List<UserReportResponseCommand> userReportResponseCommands = internalService.getAllUserReports();

        List<UserReportResponse> users = userReportResponseCommands.stream().map(
                userConverter::toResponse
        ).toList();

        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailReportResponse> getUserById(@PathVariable String userId) throws ServiceException {
        UserResponseCommand userResponseCommand = internalService.getUserById(userId);

        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponseUserDetailReport(userResponseCommand));
    }

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<UserPermissionResponse> getUserPermissions(@PathVariable String userId) throws ServiceException {
        UserPermissionsResponseCommand resultDto = internalService.getUserPermissions(userId);

        UserPermissionResponse result = UserPermissionResponse.builder()
                .email(resultDto.email())
                .role(resultDto.role())
                .permissions(resultDto.permissions())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/{userId}/roles/venue-owner")
    @AuditLog(action = "ASSIGN_ROLE", description = "Gán quyền Chủ sân cho người dùng #{#userId}")
    public ResponseEntity<Void> assignVenueOwnerRole(@PathVariable String userId) throws ServiceException {
        internalService.assignVenueOwnerRole(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{userId}/sport-profiles")
    public ResponseEntity<List<PlayerSportProfileResponse>> getSportProfiles(@PathVariable UUID userId) {
        return ResponseEntity.ok(
                playerSportProfileConverter.toResponse(playerSportProfileService.getProfilesOf(userId))
        );
    }

    @GetMapping("/scouting")
    public ResponseEntity<List<ScoutingCandidateResponse>> findScoutingCandidates(
            @RequestParam SportType sportType,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(defaultValue = "40") int limit,
            @RequestParam(required = false) Set<SkillLevel> skillLevels,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) DayOfWeek availableOn
    ) {
        ScoutingSearchCommand search = new ScoutingSearchCommand(
                sportType, latitude, longitude, radiusKm, limit, skillLevels, position, availableOn);
        return ResponseEntity.ok(playerSportProfileConverter.toScoutingResponse(
                playerSportProfileService.findScoutingCandidates(search)));
    }

    @PostMapping("/scouting/lookup")
    public ResponseEntity<List<ScoutingCandidateResponse>> lookupScoutingProfiles(
            @Valid @RequestBody ScoutingLookupRequest request
    ) {
        return ResponseEntity.ok(playerSportProfileConverter.toScoutingResponse(
                playerSportProfileService.lookupScoutingProfiles(
                        request.sportType(), request.latitude(), request.longitude(), request.userIds())));
    }

    /** Người nhận thông báo hệ thống (notification-service gửi theo lô): mã tài khoản ACTIVE, keyset theo mã. */
    @GetMapping("/recipients/ids")
    public ResponseEntity<List<UUID>> getRecipientIds(
            @RequestParam Set<String> roles,
            @RequestParam(required = false) UUID after,
            @RequestParam(defaultValue = "500") int limit
    ) throws ServiceException {
        return ResponseEntity.ok(internalService.findRecipientIds(roles, after, limit));
    }

    @GetMapping("/recipients/count")
    public ResponseEntity<Long> countRecipients(@RequestParam Set<String> roles) throws ServiceException {
        return ResponseEntity.ok(internalService.countRecipients(roles));
    }

    @GetMapping("/admin")
    public ResponseEntity<String> getAdmin() throws ServiceException {
        return ResponseEntity.status(HttpStatus.OK).body(internalService.getAdmin());
    }
}
