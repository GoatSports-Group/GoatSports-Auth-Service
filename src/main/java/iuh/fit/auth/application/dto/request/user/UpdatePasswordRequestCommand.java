package iuh.fit.auth.application.dto.request.user;

public record UpdatePasswordRequestCommand(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {}
