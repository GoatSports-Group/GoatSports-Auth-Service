package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.usecase.user.GetUserUseCase;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.response.user.UserPermissionResponse;
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

    private final GetUserUseCase getUserUseCase;

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<UserPermissionResponse> getUserPermissions(@PathVariable String userId) throws ServiceException {
        UserPermissionResponse result = getUserUseCase.getUserPermissions(userId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
