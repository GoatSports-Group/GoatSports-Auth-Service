package iuh.fit.auth.application.dto.response.auth;

import iuh.fit.auth.application.dto.response.user.UserResponseCommand;

public record RefreshResponseCommand(
    UserResponseCommand user,
    String role,
    String accessToken,
    String refreshToken
) {}
