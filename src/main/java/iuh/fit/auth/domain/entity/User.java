package iuh.fit.auth.domain.entity;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID userId;
    private UUID keycloakId;
    private String email;
    private String username;
    private String password;
    private String fullName;
    private String avatarUrl;
    private UserStatusEnum status;
    private AuthProviderEnum authProvider;
    private Role role;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
