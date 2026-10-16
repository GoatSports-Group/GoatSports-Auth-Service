package iuh.fit.auth.presentation.response.appeal;

import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

/** {@code accountStatus} chỉ có khi mở một thư (để biết tài khoản còn bị khóa không). */
public record AccountAppealResponse(
        UUID appealId,
        UUID userId,
        String email,
        String fullName,
        String subject,
        String message,
        AccountAppealStatus status,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        LocalDateTime resolvedAt,
        String reply,
        LocalDateTime repliedAt,
        String repliedBy,
        UserStatusEnum accountStatus
) {
    public static AccountAppealResponse from(AccountAppeal appeal, UserStatusEnum accountStatus) {
        return new AccountAppealResponse(appeal.getAppealId(), appeal.getUserId(), appeal.getEmail(),
                appeal.getFullName(), appeal.getSubject(), appeal.getMessage(), appeal.getStatus(),
                appeal.getCreatedAt(), appeal.getReadAt(), appeal.getResolvedAt(), appeal.getReply(),
                appeal.getRepliedAt(), appeal.getRepliedBy(), accountStatus);
    }
}
