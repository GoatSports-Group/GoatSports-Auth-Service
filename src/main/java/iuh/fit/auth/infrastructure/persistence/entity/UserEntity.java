package iuh.fit.auth.infrastructure.persistence.entity;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID userId;
    @Column(columnDefinition = "UUID")
    private UUID keycloakId;
    private String email;
    private String username;
    private String password;
    private String fullName;
    private String avatarUrl;
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;
    @Enumerated(EnumType.STRING)
    private AuthProviderEnum authProvider;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private RoleEntity role;
}
