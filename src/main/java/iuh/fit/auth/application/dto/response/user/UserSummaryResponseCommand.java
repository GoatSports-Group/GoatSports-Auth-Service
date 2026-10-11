package iuh.fit.auth.application.dto.response.user;

import java.util.UUID;

public record UserSummaryResponseCommand(UUID userId, String fullName, String email) {}
