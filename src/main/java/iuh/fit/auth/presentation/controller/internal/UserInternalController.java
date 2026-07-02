package iuh.fit.auth.presentation.controller.internal;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.service.InternalService;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.report.UserReportResponse;
import iuh.fit.auth.presentation.response.user.UserPermissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class UserInternalController {

    private final InternalService internalService;

    @GetMapping
    public ResponseEntity<List<UserReportResponse>> getAllUsers() {
        List<UserReportResponseCommand> userReportResponseCommands = internalService.getAllUserReports();

        List<UserReportResponse> users = userReportResponseCommands.stream().map(
                UserConverter::toResponse
        ).toList();

        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @GetMapping("/{userId}/permissions")
    public ResponseEntity<UserPermissionResponse> getUserPermissions(@PathVariable String userId) throws ServiceException {
        UserPermissionsResponseCommand resultDto = internalService.getUserPermissions(userId);

        UserPermissionResponse result = UserPermissionResponse.builder()
                .email(resultDto.email())
                .role(resultDto.role())
                .permissions(resultDto.permissions())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PostMapping("/{userId}/roles/venue-owner")
    public ResponseEntity<Void> assignVenueOwnerRole(@PathVariable String userId) throws ServiceException {
        internalService.assignVenueOwnerRole(userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/admin")
    public ResponseEntity<String> getAdmin() throws ServiceException {
        return ResponseEntity.status(HttpStatus.OK).body(internalService.getAdmin());
    }
}

