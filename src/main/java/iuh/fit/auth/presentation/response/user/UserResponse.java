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
    private String phone;
    private String country;
    private String status;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserRole role;
    /** Quyền đang có hiệu lực (rỗng khi vai trò tạm ngưng). Chỉ /auth/me điền, để admin app ẩn / hiện trang theo quyền. */
    private Set<String> permissions;
    private UUID keycloakId;
    private Set<String> authProviders;
    private Boolean hasPassword;
    private Boolean accountRestored;

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
