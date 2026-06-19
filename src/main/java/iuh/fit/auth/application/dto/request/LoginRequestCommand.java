package iuh.fit.auth.application.dto.request;

public record LoginRequestCommand(
        String username,
        String password
) { }
