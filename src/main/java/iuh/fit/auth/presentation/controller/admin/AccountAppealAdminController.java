package iuh.fit.auth.presentation.controller.admin;

import iuh.fit.auth.application.dto.response.base.BaseListResponse;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.AccountAppealService;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.presentation.response.appeal.AccountAppealResponse;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/account-appeals")
@RequiredArgsConstructor
public class AccountAppealAdminController {
    private final AccountAppealService accountAppealService;

    /** {@code page} tính từ 1 như các danh sách khác của auth-service; status bỏ trống = mọi thư. */
    @GetMapping
    @PreAuthorize("hasAuthority('GET_USERS')")
    public ResponseEntity<BaseListResponse> list(
            @RequestParam(required = false) AccountAppealStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<AccountAppeal> result = accountAppealService.list(status, page, size);
        BaseListResponse.Meta meta = new BaseListResponse.Meta(result.page(), result.pageSize(), result.totalPages(),
                result.total());
        return ResponseEntity.ok(new BaseListResponse(meta,
                result.items().stream().map(item -> AccountAppealResponse.from(item, null)).toList()));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('GET_USERS')")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", accountAppealService.unreadCount()));
    }

    @GetMapping("/{appealId}")
    @PreAuthorize("hasAuthority('GET_USERS')")
    public ResponseEntity<AccountAppealResponse> open(@PathVariable UUID appealId) throws ServiceException {
        AccountAppealService.AppealDetail detail = accountAppealService.open(appealId);
        return ResponseEntity.ok(AccountAppealResponse.from(detail.appeal(), detail.accountStatus()));
    }

    @PostMapping("/{appealId}/reply")
    @PreAuthorize("hasAuthority('CHANGE_USER_STATUS')")
    @AuditLog(action = "REPLY_ACCOUNT_APPEAL", description = "Trả lời khiếu nại tài khoản #{#appealId}")
    public ResponseEntity<AccountAppealResponse> reply(
            @PathVariable UUID appealId, @Valid @RequestBody ReplyRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(AccountAppealResponse.from(
                accountAppealService.reply(appealId, request.message(), request.resolve()), null));
    }

    @PutMapping("/{appealId}/status")
    @PreAuthorize("hasAuthority('CHANGE_USER_STATUS')")
    @AuditLog(action = "CHANGE_ACCOUNT_APPEAL_STATUS", description = "Đổi trạng thái khiếu nại #{#appealId} thành #{#request.status}")
    public ResponseEntity<AccountAppealResponse> changeStatus(
            @PathVariable UUID appealId, @Valid @RequestBody StatusRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(AccountAppealResponse.from(
                accountAppealService.changeStatus(appealId, request.status()), null));
    }

    public record ReplyRequest(@NotBlank String message, boolean resolve) {
    }

    public record StatusRequest(@NotNull AccountAppealStatus status) {
    }
}
