package iuh.fit.auth.application.dto.request.user;

public record CreatePasswordRequestCommand(
        String newPassword,
        String confirmPassword
) {}
