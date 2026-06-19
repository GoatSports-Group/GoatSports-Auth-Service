package iuh.fit.auth.application.dto.request.auth;

public record ForgotPasswordRequestCommand(
        String email,
        String password,
        String confirmPassword
) {}
