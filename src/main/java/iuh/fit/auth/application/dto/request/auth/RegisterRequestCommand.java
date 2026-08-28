package iuh.fit.auth.application.dto.request.auth;

import iuh.fit.auth.domain.enums.RoleEnum;

public record RegisterRequestCommand(
        RoleEnum accountType,
        String username,
        String fullName,
        String email,
        String password,
        String confirmPassword
) {}
