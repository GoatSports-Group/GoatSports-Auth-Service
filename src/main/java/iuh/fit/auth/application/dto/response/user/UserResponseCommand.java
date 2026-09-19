package iuh.fit.auth.application.dto.response.user;

import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponseCommand(
        UUID userId,
        UUID keycloakId,
        String email,
        String username,
        String password,
        String fullName,
        String avatarUrl,
        LocalDate dateOfBirth,
        String phone,
        String country,
        UserStatusEnum status,
        GenderEnum gender,
        Set<AuthProviderEnum> authProviders,
        Role role,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
