package iuh.fit.auth.application.dto.request;

public record LinkAccountRequestCommand(
        String code,
        String redirectUri
) {}
