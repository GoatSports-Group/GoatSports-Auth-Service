package iuh.fit.auth.application.dto.request.auth;

public record VerificationRequestCommand(
        String email,
        String verificationCode
) {}
