package iuh.fit.auth.controller;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.request.user.AssignRoleRequest;
import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.service.UserService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllUsers(
            @Filter Specification<User> spec, Pageable pageable
    ) {
        BaseListResponse result = this.userService.getAllUsers(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PutMapping("/role")
    public ResponseEntity<UserResponse> assignRoleForUser(
            @Valid @RequestBody AssignRoleRequest request
    ) throws ServiceException
    {
        UserResponse result = this.userService.assignRoleForUser(request.getUserId(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
