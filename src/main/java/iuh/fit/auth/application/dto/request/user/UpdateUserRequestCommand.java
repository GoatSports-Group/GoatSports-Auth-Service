package iuh.fit.auth.application.dto.request.user;

public record UpdateUserRequestCommand(
    String username,
    String fullName,
    String phone,
    String country,
    String gender
) {}
