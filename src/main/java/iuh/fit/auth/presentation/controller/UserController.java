package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") String userId) throws ServiceException {
        UserResponseCommand user = userService.getUserById(userId);
        return ResponseEntity.status(HttpStatus.OK).body(userConverter.toResponse(user));
    }

}
