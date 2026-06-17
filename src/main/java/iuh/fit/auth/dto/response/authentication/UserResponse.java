package iuh.fit.auth.dto.response.authentication;

import iuh.fit.auth.enums.AuthProvider;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID userId;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserRole role;
    private UUID keycloakId;
    private AuthProvider authProvider;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserRole {
        private UUID roleId;
        private String name;
    }
}
