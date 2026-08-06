package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.dto.request.user.UpdatePasswordRequestCommand;
import iuh.fit.auth.application.dto.request.user.UpdateUserRequestCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.request.auth.UpdatePasswordRequest;
import iuh.fit.auth.presentation.request.user.UpdateUserRequest;
import iuh.fit.auth.presentation.response.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/{userId}")
    @AuditLog(action = "GET_USER", description = "Lấy thông tin người dùng #{#userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") String userId) throws ServiceException {
        UserResponseCommand user = userService.getUserById(userId);
        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponse(user));
    }

    @PutMapping("/{userId}")
    @AuditLog(action = "UPDATE_USER", description = "Cập nhật người dùng")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateUserRequest request
    ) throws ServiceException
    {
        UserResponseCommand user = userService.updateUser(
                userId,
                new UpdateUserRequestCommand(
                        request.getUsername(), request.getFullName(),
                        request.getPhone(), request.getCountry(), request.getGender()
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponse(user));
    }

    @PutMapping("/{userId}/avatar")
    @AuditLog(action = "UPDATE_AVATAR", description = "Cập nhật avatar")
    public ResponseEntity<Void> updateAvatar(
            @PathVariable("userId") String userId,
            @RequestParam("tempKey") String tempKey
    ) throws ServiceException
    {
        userService.updateAvatar(userId, tempKey);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/password")
    @AuditLog(action = "CHANGE_PASSWORD", description = "Đổi mật khẩu")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest
    ) throws ServiceException
    {
        userService.updatePassword(
                new UpdatePasswordRequestCommand(
                        updatePasswordRequest.getCurrentPassword(),
                        updatePasswordRequest.getNewPassword(),
                        updatePasswordRequest.getConfirmPassword()
                )
        );

        return ResponseEntity.ok().build();
    }
}
