package iuh.fit.auth.application.dto.request;

public record VerificationRequestCommand(
        String email,
        String verificationCode
) {}
