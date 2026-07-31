package iuh.fit.auth.application.dto.request.user;

public record AdminUpdatePasswordRequestCommand(
        String userId,
        String newPassword,
        String confirmPassword
) {
}
