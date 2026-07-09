package iuh.fit.auth.application.dto.request.auth;

public record RegisterRequestCommand(
        String username,
        String fullName,
        String email,
        String gender,
        String password,
        String confirmPassword
) {}
