package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.usecase.user.GetUserUseCase;
import iuh.fit.auth.application.usecase.user.ManageUserUseCase;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.presentation.request.user.AssignRoleRequest;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.user.UserResponse;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {
    private final GetUserUseCase getUserUseCase;
    private final ManageUserUseCase manageUserUseCase;

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllUsers(
            @Filter Specification<UserEntity> spec, Pageable pageable
    ) {
        BaseListResponse result = this.getUserUseCase.getAllUsers(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/role")
    public ResponseEntity<UserResponse> assignRoleForUser(
            @Valid @RequestBody AssignRoleRequest request
    ) throws ServiceException {
        UserResponse result = this.manageUserUseCase.assignRoleForUser(request.getUserId(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
