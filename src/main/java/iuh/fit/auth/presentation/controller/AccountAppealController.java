package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AccountAppealService;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/account-appeals")
@RequiredArgsConstructor
public class AccountAppealController {
    private final AccountAppealService accountAppealService;

    @PostMapping
    @AuditLog(action = "SUBMIT_ACCOUNT_APPEAL", description = "Gửi khiếu nại tài khoản bị khóa")
    public ResponseEntity<Void> submit(@Valid @RequestBody SubmitAppealRequest request) throws ServiceException {
        accountAppealService.submit(request.appealToken(), request.subject(), request.message());
        return ResponseEntity.accepted().build();
    }

    public record SubmitAppealRequest(
            @NotBlank String appealToken,
            @NotBlank @Size(max = AccountAppealService.SUBJECT_MAX) String subject,
            @NotBlank @Size(min = AccountAppealService.MESSAGE_MIN, max = AccountAppealService.MESSAGE_MAX) String message
    ) {
    }
}
