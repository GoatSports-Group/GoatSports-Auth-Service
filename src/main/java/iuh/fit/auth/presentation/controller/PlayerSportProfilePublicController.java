package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.service.PlayerSportProfileService;
import iuh.fit.auth.presentation.converter.PlayerSportProfileConverter;
import iuh.fit.auth.presentation.response.user.PlayerSportProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Hồ sơ thể thao của một người chơi khác — chỉ đọc, dùng cho màn hình xem thông tin
 * đối phương (trang tin nhắn, trang ghép kèo). Phần chỉnh sửa vẫn nằm ở
 * {@link PlayerSportProfileController} và chỉ áp dụng cho chính chủ.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/sport-profiles")
@RequiredArgsConstructor
public class PlayerSportProfilePublicController {
    private final PlayerSportProfileService profileService;
    private final PlayerSportProfileConverter profileConverter;

    @GetMapping
    public ResponseEntity<List<PlayerSportProfileResponse>> getProfilesOf(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileConverter.toResponse(profileService.getProfilesOf(userId)));
    }
}
