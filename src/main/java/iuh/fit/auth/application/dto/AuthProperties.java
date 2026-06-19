package iuh.fit.auth.application.dto;

public record AuthProperties(
        int verificationCodeValidityInSecond,
        long refreshTokenExpirationSecond
) {}
