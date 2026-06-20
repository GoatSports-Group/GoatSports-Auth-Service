package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.usecase.UserUseCase;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.request.user.AssignRoleRequest;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {
    private final UserUseCase userUseCase;

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllUsers(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<UserResponseCommand> resultDto = UserMapper.toUserResponseCommand(userUseCase.getAllUsers(query));

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<UserResponse> userResponses = resultDto.items().stream()
                .map(UserConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, userResponses));
    }

    @PutMapping("/role")
    public ResponseEntity<UserResponse> assignRoleForUser(
            @Valid @RequestBody AssignRoleRequest request
    ) throws ServiceException
    {
        UserResponseCommand user = userUseCase.assignRoleForUser(
                new AssignRoleRequestCommand(request.getUserId(), request.getRoleId())
        );

        return ResponseEntity.status(HttpStatus.OK).body(UserConverter.toResponse(user));
    }
}
