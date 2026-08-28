package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.dto.request.auth.RegisterRequestCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.request.auth.RegisterRequest;
import iuh.fit.auth.presentation.response.user.UserResponse;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class AuthInternalController {

    private final AuthenticationService authenticationService;
    private final UserConverter userConverter;

    @PostMapping("/venue-owner/register")
    @AuditLog(action = "REGISTER_VENUE_OWNER", description = "Register venue owner account from workflow")
    public ResponseEntity<UserResponse> registerVenueOwner(
            @Valid @RequestBody RegisterRequest request
    ) throws ServiceException {
        UserResponseCommand user = authenticationService.registerVenueOwner(
                new RegisterRequestCommand(
                        request.getUsername(),
                        request.getFullName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getConfirmPassword()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(userConverter.toResponse(user));
    }
}
