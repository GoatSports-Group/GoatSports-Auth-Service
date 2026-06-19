package iuh.fit.auth.application.dto.request;

public record RegisterRequestCommand(
        String username,
        String fullName,
        String email,
        String password,
        String confirmPassword
) {}
