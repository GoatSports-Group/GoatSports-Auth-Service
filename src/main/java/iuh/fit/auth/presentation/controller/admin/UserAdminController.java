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
@PreAuthorize("hasAuthority('ADMIN')")
public class UserAdminController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @PostMapping
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
}
