package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AdminUpdatePasswordRequestCommand;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.request.auth.UpdatePasswordRequest;
import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.auth.presentation.request.user.AssignRoleRequest;
import iuh.fit.auth.application.dto.response.base.BaseListResponse;
import iuh.fit.auth.presentation.request.user.CreateUserRequest;
import iuh.fit.auth.presentation.response.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_USER')")
    @AuditLog(action = "ADMIN_CREATE_USER", description = "Admin tạo tài khoản mới")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) throws ServiceException
    {
        UserResponseCommand user = userService.createUser(
                new CreateUserRequestCommand(
                        request.getUsername(), request.getFullName(), request.getEmail(),
                        request.getGender(), request.getPassword()
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponse(user));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GET_USERS')")
    public ResponseEntity<BaseListResponse> getAllUsers(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) throws ServiceException
    {
        String decodedFilter = filter;
        if (filter != null) {
            try {
                decodedFilter = URLDecoder.decode(filter, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new ServiceException("Bộ lọc không hợp lệ: " + filter);
            }
        }

        PageFilter query = new PageFilter(decodedFilter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<UserResponseCommand> resultDto = userMapper.toUserResponseCommand(userService.getAllUsers(query));

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<UserResponse> userResponses = resultDto.items().stream()
                .map(userConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, userResponses));
    }

    @PutMapping("/role")
    @PreAuthorize("hasAuthority('ASSIGN_ROLE_USER')")
    @AuditLog(action = "ADMIN_ASSIGN_ROLE", description = "Admin gán vai trò người dùng")
    public ResponseEntity<UserResponse> assignRoleForUser(
            @Valid @RequestBody AssignRoleRequest request
    ) throws ServiceException
    {
        UserResponseCommand user = userService.assignRoleForUser(
                new AssignRoleRequestCommand(request.getUserId(), request.getRoleId())
        );

        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponse(user));
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasAuthority('CHANGE_PASSWORD_USER')")
    @AuditLog(action = "ADMIN_RESET_PASSWORD", description = "Admin đặt lại mật khẩu")
    public ResponseEntity<Void> updatePassword(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdatePasswordRequest request
    ) throws ServiceException
    {
        userService.updatePasswordByAdmin(
                new AdminUpdatePasswordRequestCommand(
                        userId,
                        request.getNewPassword(),
                        request.getConfirmPassword()
                )
        );

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{userId}/verify")
    @PreAuthorize("hasAuthority('VERIFY_USER')")
    @AuditLog(action = "ADMIN_TOGGLE_VERIFY", description = "Admin thay đổi trạng thái xác thực tài khoản")
    public ResponseEntity<Void> verifyUser(
            @PathVariable("userId") String userId,
            @RequestParam("verified") boolean verified
    ) throws ServiceException
    {
        userService.verifyUser(userId, verified);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
