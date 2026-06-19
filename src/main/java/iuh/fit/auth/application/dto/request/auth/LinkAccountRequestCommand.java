package iuh.fit.auth.application.dto.request.auth;

public record LinkAccountRequestCommand(
        String code,
        String redirectUri
) {}
