package iuh.fit.auth.presentation.response.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
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
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserRole role;
    private UUID keycloakId;
    private Set<String> authProviders;

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
