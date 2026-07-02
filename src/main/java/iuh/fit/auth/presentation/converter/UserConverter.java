package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.presentation.report.UserDetailReportResponse;
import iuh.fit.auth.presentation.report.UserReportResponse;
import iuh.fit.auth.presentation.response.user.UserResponse;

public class UserConverter {

    public static UserResponse toResponse(UserResponseCommand user) {
        if (user == null) return null;
        return UserResponse.builder()
            .userId(user.userId())
            .email(user.email())
            .username(user.username())
            .fullName(user.fullName())
            .avatarUrl(user.avatarUrl())
            .status(user.status() != null ? user.status().name() : null)
            .createdAt(user.createdAt())
            .updatedAt(user.updatedAt())
            .role(user.role() != null ? UserResponse.UserRole.builder()
                .roleId(user.role().getRoleId())
                .name(user.role().getName())
                .build() : null)
            .keycloakId(user.keycloakId())
            .authProvider(user.authProvider())
            .build();
    }

    public static UserReportResponse toResponse(UserReportResponseCommand user) {
        if (user == null) return null;
        return UserReportResponse.builder()
                .userId(user.userId())
                .fullName(user.fullName())
                .email(user.email())
                .roleName(user.roleName())
                .status(user.status())
                .createdAt(user.createdAt())
                .build();
    }

    public static UserDetailReportResponse toResponseUserDetailReport(UserResponseCommand user) {
        if (user == null) return null;
        return UserDetailReportResponse.builder()
                .userId(user.userId())
                .email(user.email())
                .username(user.username())
                .fullName(user.fullName())
                .avatarUrl(user.avatarUrl())
                .status(user.status().getValue())
                .createdAt(user.createdAt())
                .roleName(user.role() != null ? user.role().getName() : "Unknown")
                .build();
    }

}
