package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.AccountAppealStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountAppeal {
    private UUID appealId;
    private UUID userId;
    private String email;
    private String fullName;
    private String subject;
    private String message;
    private AccountAppealStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime resolvedAt;
    private String reply;
    private LocalDateTime repliedAt;
    private String repliedBy;
}
