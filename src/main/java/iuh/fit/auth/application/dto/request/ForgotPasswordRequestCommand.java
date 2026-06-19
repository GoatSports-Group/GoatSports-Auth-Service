package iuh.fit.auth.application.dto.request;

public record ForgotPasswordRequestCommand(
        String email,
        String password,
        String confirmPassword
) {}
