package iuh.fit.auth.controller.internal;

import iuh.fit.auth.dto.response.authentication.UserPermissionResponse;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<UserPermissionResponse> getUserPermissions(@PathVariable String userId) throws ServiceException {
        UserPermissionResponse result = userService.getUserPermissions(userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
