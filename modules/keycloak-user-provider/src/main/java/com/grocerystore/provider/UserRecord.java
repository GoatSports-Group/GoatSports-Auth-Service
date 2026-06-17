package com.grocerystore.provider;

import java.util.UUID;

public record UserRecord(
        UUID userId,
        String keycloakId,
        String email,
        String username,
        String fullName,
        String avatarUrl,
        String password,
        String status,
        String roleId) {
};