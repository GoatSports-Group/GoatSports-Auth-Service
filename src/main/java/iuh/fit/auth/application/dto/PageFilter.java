package iuh.fit.auth.application.dto;

public record PageFilter(
    String filter,
    int page,
    int size
) {}
