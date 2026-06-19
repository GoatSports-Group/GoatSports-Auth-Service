package iuh.fit.auth.application.dto.request.auth;

public record LoginRequestCommand(
        String username,
        String password
) { }
