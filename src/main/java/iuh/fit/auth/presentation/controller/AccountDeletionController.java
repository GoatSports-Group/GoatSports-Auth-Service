package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AccountDeletionService;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/deletion")
@RequiredArgsConstructor
public class AccountDeletionController {
    private final AccountDeletionService accountDeletionService;

    public record DeletionCheckResponse(List<String> blockers) {
    }

    public record DeletionRequest(@NotBlank String code) {
    }

    public record DeletionScheduledResponse(LocalDateTime purgeAt) {
    }

    @GetMapping
    public ResponseEntity<DeletionCheckResponse> check() throws ServiceException {
        return ResponseEntity.ok(new DeletionCheckResponse(accountDeletionService.currentBlockers()));
    }

    @PostMapping("/code")
    public ResponseEntity<Void> sendCode() throws ServiceException {
        accountDeletionService.sendCode();
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @AuditLog(action = "REQUEST_ACCOUNT_DELETION", description = "Yêu cầu xóa tài khoản")
    public ResponseEntity<DeletionScheduledResponse> requestDeletion(@Valid @RequestBody DeletionRequest request)
            throws ServiceException {
        return ResponseEntity.ok(new DeletionScheduledResponse(accountDeletionService.requestDeletion(request.code())));
    }
}
