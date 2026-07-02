package iuh.fit.auth.application.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserReportResponseCommand(
        UUID userId,
        String fullName,
        String email,
        String roleName,
        String status,
        LocalDateTime createdAt
) {}
