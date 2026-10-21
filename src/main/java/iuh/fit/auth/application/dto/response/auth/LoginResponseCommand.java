package iuh.fit.auth.application.dto.response.auth;

import iuh.fit.auth.application.dto.response.user.UserResponseCommand;

public record LoginResponseCommand(
    UserResponseCommand user,
    String accessToken,
    String refreshToken,
    boolean accountRestored
) {}
